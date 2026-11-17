package com.datamaster.module.ai.service.dbgpt;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.config.DbGptProperties;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** API contract double only; never connects to DB-GPT or a model provider. */
public class DbGptModelManagementClientTest {
    @Test public void independentAliasUsesBackendAndNormalizesCompatibleUrl() {
        Stub client=new Stub();
        client.ensure("dm_test_s1_p2_v3","real-model","https://provider.invalid/v1/chat/completions/","synthetic-only");
        assertEquals(1,client.posts);
        assertEquals("dm_test_s1_p2_v3",client.created.getString("model"));
        JSONObject params=client.created.getJSONObject("params");
        assertEquals("dm_test_s1_p2_v3",params.getString("name"));
        assertEquals("real-model",params.getString("backend"));
        assertEquals("proxy/openai",params.getString("provider"));
        assertEquals("https://provider.invalid/v1",params.getString("api_base"));
        assertEquals("synthetic-only",params.getString("api_key"));
        client.ensure("dm_test_s1_p2_v3","real-model","https://provider.invalid/v1","synthetic-only");
        assertEquals(1,client.posts);
    }
    @Test public void returnedSuccessWithoutHealthyRegistrationIsNotReady() {
        Stub client=new Stub(); client.becomesHealthy=false;
        rejected(() -> client.ensure("dm_test_s1_p2_v1","model","https://provider.invalid/v1","synthetic-only"));
        assertEquals(1,client.posts);
        rejected(() -> client.ensure("dm_test_s1_p2_v1","model","https://provider.invalid/v1","synthetic-only"));
        assertEquals(1,client.posts); // Never create a duplicate just because it is not yet healthy.
    }
    @Test public void managementTransportRequiresExplicitTrustAndDoesNotAllowUrlCredentials() {
        DbGptProperties p=new DbGptProperties(); p.setUrl("http://192.0.2.1:5670");
        DbGptModelManagementClient client=new DbGptModelManagementClient(p);
        rejected(client::target);
        p.getModelSync().setAllowInsecureHttp(true); assertEquals("http://192.0.2.1:5670",client.target());
        p.getModelSync().setManagementUrl("https://example.invalid/api/"); assertEquals("https://example.invalid/api",client.target());
        p.getModelSync().setManagementUrl("https://user:secret@example.invalid"); rejected(client::target);
        p.getModelSync().setManagementUrl("https://example.invalid?key=x"); rejected(client::target);
        p.getModelSync().setManagementUrl("http://127.0.0.1:15670"); p.getModelSync().setAllowInsecureHttp(false); assertNotNull(client.target());
    }
    private static void rejected(Runnable action) { try { action.run(); fail("Expected rejection"); } catch(ServiceException expected) { assertNotNull(expected.getMessage()); } }
    private static class Stub extends DbGptModelManagementClient {
        JSONObject created; int posts; boolean becomesHealthy=true;
        Stub() { super(new DbGptProperties()); }
        @Override protected JSONObject call(String method,String path,JSONObject body) {
            JSONObject response=new JSONObject(); response.put("success",true); JSONArray data=new JSONArray(); response.put("data",data);
            if(path.endsWith("model-types")) {
                JSONObject type=new JSONObject(); type.put("provider","proxy/openai"); type.put("worker_type","llm"); type.put("host","worker"); type.put("port",5670);
                JSONArray params=new JSONArray(); for(String field:Arrays.asList("name","backend","api_base","api_key","provider")) { JSONObject definition=new JSONObject(); definition.put("param_name",field); params.add(definition); }
                type.put("params",params); data.add(type);
            } else if("POST".equals(method)) { created=body; posts++; }
            else if(created!=null) { JSONObject model=new JSONObject(); model.put("model_name",created.getString("model")); model.put("worker_type","llm"); model.put("healthy",becomesHealthy); data.add(model); }
            return response;
        }
    }
}
