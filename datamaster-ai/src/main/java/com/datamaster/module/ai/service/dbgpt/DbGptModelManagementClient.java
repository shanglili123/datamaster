package com.datamaster.module.ai.service.dbgpt;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.config.DbGptProperties;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** DB-GPT 0.8.1 management API. No prompts, credential logs, redirects or model inference. */
@Component
public class DbGptModelManagementClient {
    private final DbGptProperties properties;
    public DbGptModelManagementClient(DbGptProperties properties) { this.properties=properties; }

    public String target() {
        String configured=properties.getModelSync().getManagementUrl();
        String value=configured==null || configured.isBlank()?properties.getUrl():configured;
        try {
            URI uri=URI.create(value.trim());
            if(uri.getHost()==null || uri.getRawUserInfo()!=null || uri.getRawQuery()!=null || uri.getRawFragment()!=null) throw new IllegalArgumentException();
            boolean loopback="localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()) || "[::1]".equals(uri.getHost()) || "::1".equals(uri.getHost());
            if(!"https".equalsIgnoreCase(uri.getScheme()) && !("http".equalsIgnoreCase(uri.getScheme()) && (loopback || properties.getModelSync().isAllowInsecureHttp())))
                throw new ServiceException("DB-GPT同步需HTTPS或回环隧道；受控内网HTTP须显式允许");
            String result=uri.toString(); while(result.endsWith("/")) result=result.substring(0,result.length()-1); return result;
        } catch(ServiceException e) { throw e; }
        catch(RuntimeException e) { throw new ServiceException("DB-GPT管理地址无效"); }
    }

    public JSONArray models() { return array(call("GET","/api/v2/serve/model/models",null)); }
    public boolean healthy(String alias) {
        int count=0; boolean healthy=false;
        for(Object item:models()) {
            JSONObject model=(JSONObject)item;
            if(alias.equals(model.getString("model_name")) && "llm".equals(model.getString("worker_type"))) {
                count++; healthy=Boolean.TRUE.equals(model.getBoolean("healthy"));
            }
        }
        if(count>1) throw new ServiceException("DB-GPT同步模型实例冲突");
        return count==1 && healthy;
    }

    public void ensure(String alias,String backend,String apiUrl,String key) {
        JSONArray instances=models();
        JSONObject existing=null;
        for(Object item:instances) {
            JSONObject model=(JSONObject)item;
            if(alias.equals(model.getString("model_name"))) {
                if(existing!=null || !"llm".equals(model.getString("worker_type"))) throw new ServiceException("DB-GPT同步模型实例冲突");
                existing=model;
            }
        }
        if(existing!=null) {
            if(Boolean.TRUE.equals(existing.getBoolean("healthy"))) return;
            throw new ServiceException("DB-GPT已注册模型尚未健康，请稍后重试");
        }
        JSONObject supported=null;
        for(Object item:array(call("GET","/api/v2/serve/model/model-types",null))) {
            JSONObject type=(JSONObject)item;
            if("proxy/openai".equals(type.getString("provider")) && "llm".equals(type.getString("worker_type"))) { supported=type; break; }
        }
        if(supported==null || supported.getString("host")==null || supported.getInteger("port")==null) throw new ServiceException("DB-GPT未提供兼容模型管理节点");
        Set<String> fields=new HashSet<>(); JSONArray definitions=supported.getJSONArray("params");
        if(definitions!=null) for(Object item:definitions) fields.add(((JSONObject)item).getString("param_name"));
        if(!fields.containsAll(Arrays.asList("name","backend","api_base","api_key","provider"))) throw new ServiceException("DB-GPT模型管理参数不兼容");
        String base=apiUrl.trim(); while(base.endsWith("/")) base=base.substring(0,base.length()-1);
        if(base.endsWith("/chat/completions")) base=base.substring(0,base.length()-"/chat/completions".length());
        JSONObject params=new JSONObject(); params.put("name",alias); params.put("backend",backend);
        params.put("provider","proxy/openai"); params.put("api_base",base); params.put("api_key",key);
        params.put("concurrency",Math.max(1,Math.min(16,properties.getModelSync().getConcurrency()))); params.put("verbose",false);
        JSONObject request=new JSONObject(); request.put("host",supported.getString("host")); request.put("port",supported.getInteger("port"));
        request.put("model",alias); request.put("worker_type","llm"); request.put("params",params);
        call("POST","/api/v2/serve/model/models",request);
        if(!healthy(alias)) throw new ServiceException("DB-GPT模型已提交但尚未健康");
    }

    protected JSONObject call(String method,String path,JSONObject body) {
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL(target()+path).openConnection();
            connection.setInstanceFollowRedirects(false); connection.setRequestMethod(method);
            int timeout=Math.max(1000,Math.min(30000,properties.getModelSync().getTimeoutMillis()));
            connection.setConnectTimeout(timeout); connection.setReadTimeout(timeout);
            connection.setRequestProperty("Accept","application/json");
            String token=properties.getModelSync().getManagementToken();
            if(token!=null && !token.isBlank()) {
                if(token.contains("\r") || token.contains("\n")) throw new ServiceException("DB-GPT管理凭据格式无效");
                connection.setRequestProperty("Authorization","Bearer "+token.trim());
            }
            if(body!=null) {
                connection.setDoOutput(true); connection.setRequestProperty("Content-Type","application/json");
                try(OutputStream out=connection.getOutputStream()) { out.write(JSON.toJSONBytes(body)); }
            }
            int status=connection.getResponseCode();
            if(status<200 || status>=300) throw new ServiceException("DB-GPT管理接口拒绝请求（HTTP "+status+"）");
            byte[] bytes;
            try(InputStream input=connection.getInputStream()) { bytes=input.readNBytes(4*1024*1024+1); }
            if(bytes.length>4*1024*1024) throw new ServiceException("DB-GPT管理响应超出上限");
            JSONObject response=JSON.parseObject(new String(bytes,StandardCharsets.UTF_8));
            if(response==null || !Boolean.TRUE.equals(response.getBoolean("success"))) throw new ServiceException("DB-GPT管理接口未确认成功");
            return response;
        } catch(ServiceException e) { throw e; }
        catch(Exception ignored) { throw new ServiceException("DB-GPT管理接口不可用或响应不兼容"); }
        finally { if(connection!=null) connection.disconnect(); }
    }
    private static JSONArray array(JSONObject response) {
        Object data=response.get("data"); if(!(data instanceof JSONArray)) throw new ServiceException("DB-GPT管理列表响应不兼容"); return (JSONArray)data;
    }
}
