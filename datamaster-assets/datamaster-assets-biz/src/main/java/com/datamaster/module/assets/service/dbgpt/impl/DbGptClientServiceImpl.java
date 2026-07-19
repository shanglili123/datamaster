package com.datamaster.module.assets.service.dbgpt.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import cn.hutool.core.io.FileUtil;
import com.datamaster.common.exception.ServiceException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.datamaster.module.assets.config.DbGptProperties;
import com.datamaster.module.assets.model.dto.dbgpt.*;
import com.datamaster.module.assets.service.dbgpt.IDbGptClientService;
import com.datamaster.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class DbGptClientServiceImpl implements IDbGptClientService {

    @Resource
    private DbGptProperties dbGptProperties;

    private static final String CHAT_PATH = "/api/v2/chat/completions";
    private static final String CHAT_V1_PATH = "/api/v1/chat/completions";
    private static final String CHAT_REACT_AGENT_PATH = "/api/v1/chat/react-agent";
    private static final String DATASOURCE_PATH = "/api/v2/serve/datasources";
    private static final String KNOWLEDGE_SPACES_PATH = "/api/v2/serve/knowledge/spaces";
    private static final String KNOWLEDGE_DOCUMENTS_PATH = "/api/v2/serve/knowledge/documents";
    private static final String LEGACY_KNOWLEDGE_PATH = "/api/v1/knowledge";
    private static final String SKILL_UPLOAD_PATH = "/api/v1/skills/upload";

    @Override
    public DbGptChatCompletionResponse chatCompletion(DbGptChatCompletionRequest request) {
        String url = dbGptProperties.getUrl() + CHAT_PATH;
        String jsonBody = JSON.toJSONString(request);

        log.debug("DB-GPT chat request: {}", jsonBody.length() > 500 ? jsonBody.substring(0, 500) + "..." : jsonBody);

        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT调用失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
            }

            return JSON.parseObject(responseBody, DbGptChatCompletionResponse.class);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT调用异常: " + e.getMessage());
        }
    }

    @Override
    public DbGptChatCompletionResponse chatCompletionV1(DbGptChatCompletionRequest request) {
        String url = dbGptProperties.getUrl() + CHAT_REACT_AGENT_PATH;
        String jsonBody = buildReactAgentBody(request).toJSONString();

        log.debug("DB-GPT v1 chat request: {}", jsonBody.length() > 500 ? jsonBody.substring(0, 500) + "..." : jsonBody);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(dbGptProperties.getTimeout());
            connection.setReadTimeout(0);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "text/event-stream, application/json");
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            String responseBody = readResponseBody(connection, status);
            if (status != 200) {
                throw new ServiceException("DB-GPT原生问数调用失败，状态码: " + status + "，响应: " + responseBody);
            }
            return toChatCompletionResponse(responseBody);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT原生问数调用异常: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void chatCompletionV1Stream(DbGptChatCompletionRequest request, Consumer<String> onMessage) {
        String url = dbGptProperties.getUrl() + CHAT_REACT_AGENT_PATH;
        String jsonBody = buildReactAgentBody(request).toJSONString();

        log.debug("DB-GPT v1 stream request: {}", jsonBody.length() > 500 ? jsonBody.substring(0, 500) + "..." : jsonBody);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(dbGptProperties.getTimeout());
            connection.setReadTimeout(0);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "text/event-stream, application/json");
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            if (status != 200) {
                String responseBody = readResponseBody(connection, status);
                throw new ServiceException("DB-GPT原生问数调用失败，状态码: " + status + "，响应: " + responseBody);
            }
            streamResponseBody(connection, onMessage);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT原生问数调用异常: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public String chatCompletionStream(DbGptChatCompletionRequest request) {
        String url = dbGptProperties.getUrl() + CHAT_PATH;
        request.setStream(true);
        String jsonBody = JSON.toJSONString(request);

        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .body(jsonBody)
                .timeout(dbGptProperties.getTimeout() * 3)
                .execute()) {

            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT流式调用失败，状态码: " + response.getStatus());
            }

            return response.body();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT流式调用异常: " + e.getMessage());
        }
    }

    private DbGptChatCompletionResponse toChatCompletionResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return emptyResponse("");
        }
        if (!responseBody.startsWith("data: ")) {
            return tryParseSingleJson(responseBody);
        }
        String fullContent = parseSseContent(responseBody);
        return emptyResponse(fullContent != null ? fullContent : responseBody);
    }

    private String parseSseContent(String sseBody) {
        String lastContent = null;
        String[] events = sseBody.split("\n\n");
        for (String event : events) {
            if (event == null || !event.startsWith("data: ")) {
                continue;
            }
            String data = event.substring(6).trim();
            if ("[DONE]".equals(data)) {
                continue;
            }
            try {
                JSONObject obj = JSON.parseObject(data);
                JSONArray choices = obj.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
                    if (msg != null && msg.getString("content") != null) {
                        lastContent = msg.getString("content");
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return lastContent;
    }

    private String readResponseBody(HttpURLConnection connection, int status) throws Exception {
        InputStream inputStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            return "";
        }
        StringBuilder raw = new StringBuilder();
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    raw.append(line).append('\n');
                    String parsed = parseSseLine(line);
                    if (parsed == null) {
                        continue;
                    }
                    if ("[DONE]".equals(parsed)) {
                        break;
                    }
                    mergeIncrementalContent(content, parsed);
                    if (hasCompleteSqlBlock(content.toString())) {
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                if (content.length() == 0 && raw.length() == 0) {
                    throw e;
                }
                log.warn("DB-GPT流式响应等待超时，返回已接收内容。contentLength={}, rawLength={}",
                        content.length(), raw.length());
            }
        }
        return content.length() > 0 ? content.toString() : raw.toString();
    }

    private boolean hasCompleteSqlBlock(String content) {
        if (content == null) {
            return false;
        }
        String lower = content.toLowerCase();
        int start = lower.indexOf("```sql");
        if (start < 0) {
            return false;
        }
        return lower.indexOf("```", start + 6) > start;
    }

    private String parseSseLine(String line) {
        if (line == null) {
            return null;
        }
        String value = line.trim();
        if (value.isEmpty() || value.startsWith(":")) {
            return null;
        }
        if (value.startsWith("data:")) {
            value = value.substring(5).trim();
        }
        if ("[DONE]".equals(value)) {
            return "[DONE]";
        }
        try {
            JSONObject object = JSON.parseObject(value);
            if (isNativeAgentEvent(object)) {
                return JSON.toJSONString(object);
            }
            return extractContent(object);
        } catch (Exception ignored) {
            return value;
        }
    }

    private boolean isNativeAgentEvent(JSONObject object) {
        if (object == null) {
            return false;
        }
        if (hasStructuredPayload(object)
                || object.containsKey("incremental")
                || object.containsKey("display_type")
                || object.containsKey("view_name")
                || object.containsKey("resource_value")
                || object.containsKey("thoughts")
                || object.containsKey("action")
                || object.containsKey("tool_name")
                || object.containsKey("tool_input")) {
            return true;
        }
        Object data = object.get("data");
        return data instanceof JSONObject && (hasStructuredPayload((JSONObject) data)
                || ((JSONObject) data).containsKey("incremental")
                || ((JSONObject) data).containsKey("display_type")
                || ((JSONObject) data).containsKey("view_name")
                || ((JSONObject) data).containsKey("resource_value")
                || ((JSONObject) data).containsKey("thoughts")
                || ((JSONObject) data).containsKey("action")
                || ((JSONObject) data).containsKey("tool_name")
                || ((JSONObject) data).containsKey("tool_input"));
    }

    private String extractContent(JSONObject object) {
        if (object == null) {
            return null;
        }
        if (hasStructuredPayload(object)) {
            return JSON.toJSONString(object);
        }
        String content = firstNonBlank(object.getString("text"), object.getString("content"), object.getString("message"));
        if (!content.isEmpty()) {
            return content;
        }
        Object data = object.get("data");
        if (data instanceof String) {
            return (String) data;
        }
        if (data instanceof JSONObject) {
            String nested = extractContent((JSONObject) data);
            return firstNonBlank(nested, JSON.toJSONString(data));
        }
        if (data instanceof JSONArray) {
            return JSON.toJSONString(data);
        }
        JSONArray choices = object.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            if (message != null && message.getString("content") != null) {
                return message.getString("content");
            }
            JSONObject delta = choice.getJSONObject("delta");
            if (delta != null && delta.getString("content") != null) {
                return delta.getString("content");
            }
        }
        return null;
    }

    private boolean hasStructuredPayload(JSONObject object) {
        return object.containsKey("sql")
                || object.containsKey("detailData")
                || object.containsKey("chatData")
                || object.containsKey("selectColumn")
                || object.containsKey("steps")
                || object.containsKey("tool_calls")
                || object.containsKey("vis")
                || object.containsKey("view")
                || object.containsKey("result")
                || object.containsKey("observation")
                || object.containsKey("thought")
                || object.containsKey("summary");
    }

    private void mergeIncrementalContent(StringBuilder content, String parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return;
        }
        String current = content.toString();
        if (parsed.startsWith(current)) {
            content.setLength(0);
            content.append(parsed);
        } else {
            content.append(parsed);
        }
    }

    private DbGptChatCompletionResponse tryParseSingleJson(String responseBody) {
        try {
            JSONObject object = JSON.parseObject(responseBody);
            String content = firstNonBlank(extractContent(object), responseBody);
            return emptyResponse(content);
        } catch (Exception ignored) {
            log.debug("DB-GPT response is not a single JSON object: {}", ignored.getMessage());
        }
        return emptyResponse(responseBody);
    }

    private JSONObject buildReactAgentBody(DbGptChatCompletionRequest request) {
        String dbName = firstNonBlank(request.getChatParam(), request.getDbName());
        String chatMode = firstNonBlank(request.getChatMode(), dbGptProperties.getChatMode(), "chat_with_db_qa");
        Object skillIdObj = request.getExtra() == null ? null : request.getExtra().get("skill_id");
        String skillId = skillIdObj == null ? "" : firstNonBlank(String.valueOf(skillIdObj));
        JSONObject body = new JSONObject();
        body.put("conv_uid", firstNonBlank(request.getConvUid(), "dm-" + System.currentTimeMillis()));
        body.put("chat_mode", chatMode);
        body.put("model_name", firstNonBlank(request.getModel(), dbGptProperties.getModel()));
        body.put("user_input", buildReactAgentInput(dbName, toUserInputText(request), skillId));
        body.put("temperature", request.getTemperature() == null ? 0.6 : request.getTemperature());
        body.put("max_new_tokens", request.getMaxTokens() == null ? 4000 : request.getMaxTokens());
        boolean skillMode = "chat_react_agent".equalsIgnoreCase(chatMode);
        body.put("select_param", skillMode ? "" : dbName);

        JSONObject extInfo = new JSONObject();
        extInfo.put("select_param", skillMode ? "" : dbName);
        extInfo.put("database_name", dbName);
        extInfo.put("db_name", dbName);
        extInfo.put("database_type", toDbGptDbType(request.getDbType()));
        extInfo.put("datasource_id", request.getDatasourceId());
        if (StringUtils.isNotBlank(request.getSpaceName())) {
            extInfo.put("space_name", request.getSpaceName());
        }
        if (request.getExtra() != null) {
            extInfo.putAll(request.getExtra());
        }
        body.put("ext_info", extInfo);
        return body;
    }

    private void streamResponseBody(HttpURLConnection connection, Consumer<String> onMessage) throws Exception {
        InputStream inputStream = connection.getInputStream();
        if (inputStream == null) {
            return;
        }
        StringBuilder emitted = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String parsed = parseSseLine(line);
                if (parsed == null) {
                    continue;
                }
                if ("[DONE]".equals(parsed)) {
                    break;
                }
                String delta = toDelta(emitted, parsed);
                if (!delta.isEmpty() && onMessage != null) {
                    onMessage.accept(delta);
                }
            }
        }
    }

    private String toDelta(StringBuilder emitted, String parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return "";
        }
        String current = emitted.toString();
        String delta;
        if (parsed.startsWith(current)) {
            delta = parsed.substring(current.length());
            emitted.setLength(0);
            emitted.append(parsed);
        } else {
            delta = parsed;
            emitted.append(parsed);
        }
        return delta;
    }

    private DbGptChatCompletionResponse emptyResponse(String content) {
        DbGptChatCompletionResponse response = new DbGptChatCompletionResponse();
        DbGptChatCompletionResponse.Choice choice = new DbGptChatCompletionResponse.Choice();
        choice.setMessage(new DbGptChatMessage("assistant", content));
        response.setChoices(Collections.singletonList(choice));
        return response;
    }

    private String toUserInputText(DbGptChatCompletionRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return "";
        }
        if (request.getMessages().size() == 1) {
            DbGptChatMessage message = request.getMessages().get(0);
            if (message != null && "user".equals(message.getRole())) {
                return firstNonBlank(message.getContent());
            }
        }
        StringBuilder builder = new StringBuilder();
        for (DbGptChatMessage message : request.getMessages()) {
            if (message == null || firstNonBlank(message.getContent()).isEmpty()) {
                continue;
            }
            if ("system".equals(message.getRole())) {
                builder.append("【系统规则】\n");
            } else if ("user".equals(message.getRole())) {
                builder.append("【用户输入】\n");
            }
            builder.append(message.getContent()).append("\n\n");
        }
        if (builder.length() > 0) {
            return builder.toString();
        }
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            DbGptChatMessage message = request.getMessages().get(i);
            if ("user".equals(message.getRole())) {
                return message.getContent();
            }
        }
        return request.getMessages().get(request.getMessages().size() - 1).getContent();
    }

    private String buildReactAgentInput(String dbName, String userInput, String skillId) {
        String input = firstNonBlank(userInput);
        if (StringUtils.isNotBlank(skillId) && !input.trim().startsWith("/" + skillId)) {
            input = "/" + skillId + "  " + input;
        }
        if (dbName == null || dbName.trim().isEmpty()) {
            return input;
        }
        return "[Database: " + dbName + "] " + input;
    }

    private String toDbGptDbType(String dbType) {
        String value = firstNonBlank(dbType).toLowerCase();
        if (value.contains("postgres")) {
            return "postgresql";
        }
        if (value.contains("mysql")) {
            return "mysql";
        }
        if (value.contains("oracle")) {
            return "oracle";
        }
        if (value.contains("sqlserver") || value.contains("sql server")) {
            return "mssql";
        }
        if (value.contains("kingbase")) {
            return "postgresql";
        }
        return value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    @Override
    public Integer createDatasource(DbGptDatasourceCreateRequest request) {
        String url = dbGptProperties.getUrl() + DATASOURCE_PATH;
        String jsonBody = JSON.toJSONString(request);

        log.info("DB-GPT create datasource: type={}, body={}", request.getType(), jsonBody);

        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT数据源创建失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
            }

            JSONObject result = JSON.parseObject(responseBody);
            if (!result.getBooleanValue("success")) {
                throw new ServiceException("DB-GPT数据源创建失败: " + result.getString("err_msg"));
            }

            JSONObject data = result.getJSONObject("data");
            if (data != null) {
                return data.getInteger("id");
            }
            return result.getInteger("id");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT数据源创建异常: " + e.getMessage());
        }
    }

    @Override
    public List<DbGptDatasourceResponse> listDatasources(String dbType) {
        String url = dbGptProperties.getUrl() + DATASOURCE_PATH;
        if (dbType != null) {
            url += "?db_type=" + dbType;
        }

        try (HttpResponse response = HttpUtil.createRequest(Method.GET, url)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT数据源查询失败，状态码: " + response.getStatus());
            }

            JSONObject result = JSON.parseObject(responseBody);
            if (!result.getBooleanValue("success")) {
                return Collections.emptyList();
            }

            String dataStr = result.getString("data");
            return JSON.parseArray(dataStr, DbGptDatasourceResponse.class);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT数据源查询异常: " + e.getMessage());
        }
    }

    @Override
    public String getDatasourceDbName(Integer datasourceId) {
        String url = dbGptProperties.getUrl() + DATASOURCE_PATH + "/" + datasourceId;
        try (HttpResponse response = HttpUtil.createRequest(Method.GET, url)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            if (response.getStatus() != 200) {
                log.warn("DB-GPT数据源查询失败，状态码: {}", response.getStatus());
                return null;
            }

            JSONObject result = JSON.parseObject(responseBody);
            if (!result.getBooleanValue("success")) {
                return null;
            }

            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                return null;
            }
            JSONObject params = data.getJSONObject("params");
            return firstNonBlank(data.getString("db_name"),
                    data.getString("name"),
                    params == null ? null : params.getString("name"),
                    params == null ? null : params.getString("database"));
        } catch (Exception e) {
            log.warn("DB-GPT数据源名称查询异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteDatasource(Integer id) {
        String url = dbGptProperties.getUrl() + DATASOURCE_PATH + "/" + id;

        try (HttpResponse response = HttpUtil.createRequest(Method.DELETE, url)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            if (response.getStatus() != 200) {
                String body = response.body();
                throw new ServiceException("DB-GPT数据源删除失败，状态码: " + response.getStatus() + "，响应: " + body);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT数据源删除异常: " + e.getMessage());
        }
    }

    @Override
    public String createKnowledgeSpace(String name, String vectorType, String owner) {
        String legacySpaceId = createLegacyKnowledgeSpace(name, vectorType, owner);
        if (legacySpaceId != null) {
            return legacySpaceId;
        }
        String url = dbGptProperties.getUrl() + KNOWLEDGE_SPACES_PATH;
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("vector_type", vectorType != null ? vectorType : "Chroma");
        if (owner != null) {
            body.put("owner", owner);
        }

        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT create space response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() == 200) {
                JSONObject result = JSON.parseObject(responseBody);
                if (result.containsKey("data")) {
                    Object dataObj = result.get("data");
                    if (dataObj instanceof JSONObject) {
                        String id = ((JSONObject) dataObj).getString("id");
                        if (id != null) return id;
                    }
                } else if (result.containsKey("id")) {
                    return result.getString("id");
                }
            }
            if (response.getStatus() != 200) {
                log.warn("创建DB-GPT知识空间非200响应: status={}, body={}", response.getStatus(), responseBody);
            }
            return null;
        } catch (Exception e) {
            log.warn("创建DB-GPT知识空间异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getKnowledgeSpaceId(String name) {
        String legacySpaceId = getLegacyKnowledgeSpaceId(name);
        if (legacySpaceId != null) {
            return legacySpaceId;
        }
        String url = dbGptProperties.getUrl() + KNOWLEDGE_SPACES_PATH;
        try (HttpResponse response = HttpUtil.createRequest(Method.GET, url)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT list spaces response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() != 200) {
                return null;
            }

            JSONObject result = JSON.parseObject(responseBody);
            JSONArray items = null;
            if (result.containsKey("data")) {
                Object dataObj = result.get("data");
                if (dataObj instanceof JSONArray) {
                    items = (JSONArray) dataObj;
                } else if (dataObj instanceof JSONObject) {
                    JSONObject dataObj2 = (JSONObject) dataObj;
                    items = dataObj2.getJSONArray("items");
                }
            }
            if (items == null) {
                items = result.getJSONArray("items");
            }
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    JSONObject space = items.getJSONObject(i);
                    if (name.equals(space.getString("name"))) {
                        return String.valueOf(space.get("id"));
                    }
                }
            } else if (result.containsKey("id") && name.equals(result.getString("name"))) {
                return result.getString("id");
            }
            return null;
        } catch (Exception e) {
            log.warn("获取DB-GPT知识空间ID失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteKnowledgeSpace(String spaceId) {
        String url = dbGptProperties.getUrl() + KNOWLEDGE_SPACES_PATH + "/" + spaceId;

        try (HttpResponse response = HttpUtil.createRequest(Method.DELETE, url)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT知识空间删除失败，状态码: " + response.getStatus());
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT知识空间删除异常: " + e.getMessage());
        }
    }

    private String createLegacyKnowledgeSpace(String name, String vectorType, String owner) {
        String url = dbGptProperties.getUrl() + LEGACY_KNOWLEDGE_PATH + "/space/add";
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("vector_type", vectorType != null ? vectorType : "Chroma");
        if (owner != null) {
            body.put("owner", owner);
        }
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT legacy create space response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() == 404 || response.getStatus() == 405) {
                return null;
            }
            if (response.getStatus() != 200) {
                return null;
            }
            JSONObject result = JSON.parseObject(responseBody);
            if (result.containsKey("success") && !result.getBooleanValue("success")) {
                String errMsg = result.getString("err_msg");
                if (errMsg != null && (errMsg.contains("already") || errMsg.contains("exist") || errMsg.contains("存在"))) {
                    String existingId = getLegacyKnowledgeSpaceId(name);
                    return existingId == null ? name : existingId;
                }
                return null;
            }
            String id = extractSpaceId(result.get("data"), name);
            return id == null ? name : id;
        } catch (Exception e) {
            log.warn("创建DB-GPT legacy知识空间失败: {}", e.getMessage());
            return null;
        }
    }

    private String getLegacyKnowledgeSpaceId(String name) {
        String url = dbGptProperties.getUrl() + LEGACY_KNOWLEDGE_PATH + "/space/list";
        JSONObject body = new JSONObject();
        body.put("name", name);
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT legacy list spaces response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() == 404 || response.getStatus() == 405 || response.getStatus() != 200) {
                return null;
            }
            JSONObject result = JSON.parseObject(responseBody);
            JSONArray items = extractDocumentItems(result);
            if (items == null || items.isEmpty()) {
                return null;
            }
            for (int i = 0; i < items.size(); i++) {
                JSONObject space = items.getJSONObject(i);
                if (name.equals(space.getString("name")) || name.equals(space.getString("space_name"))) {
                    Object id = firstJsonValue(space, "id", "space_id");
                    return id == null ? name : String.valueOf(id);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("获取DB-GPT legacy知识空间ID失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractSpaceId(Object dataObj, String name) {
        JSONObject space = normalizeDocumentObject(dataObj);
        if (space == null) {
            return null;
        }
        if (name.equals(space.getString("name")) || name.equals(space.getString("space_name")) || !space.containsKey("name")) {
            Object id = firstJsonValue(space, "id", "space_id");
            return id == null ? null : String.valueOf(id);
        }
        return null;
    }

    @Override
    public String uploadSkill(String fileName, String content) {
        String url = dbGptProperties.getUrl() + SKILL_UPLOAD_PATH;
        File tempDir = null;
        File tempFile = null;
        try {
            tempDir = FileUtil.mkdir(FileUtil.file(System.getProperty("java.io.tmpdir"),
                    "dbgpt-skill-" + System.nanoTime()));
            tempFile = FileUtil.file(tempDir, fileName);
            FileUtil.writeUtf8String(content == null ? "" : content, tempFile);
            return doUploadSkill(url, fileName, tempFile);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT Skill上传异常: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                FileUtil.del(tempDir);
            }
        }
    }

    private String doUploadSkill(String url, String fileName, File file) {
        log.info("DB-GPT upload skill: url={}, fileField=file, fileName={}, tempFile={}",
                url, fileName, file == null ? null : file.getAbsolutePath());
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .form("file", file)
                .form("name", fileName)
                .form("file_name", fileName)
                .form("skill_name", fileName)
                .timeout(dbGptProperties.getTimeout() * 2)
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT upload skill response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT Skill上传失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
            }
            JSONObject result = JSON.parseObject(responseBody);
            if (result.containsKey("success") && !result.getBooleanValue("success")) {
                throw new ServiceException("DB-GPT Skill上传失败: " + result.getString("err_msg"));
            }
            String skillId = extractSkillId(result);
            return skillId == null ? fileName : skillId;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT Skill上传异常: " + e.getMessage());
        }
    }

    private String extractSkillId(JSONObject result) {
        if (result == null) {
            return null;
        }
        Object idObj = firstJsonValue(result, "id", "skill_id", "name");
        if (idObj != null) {
            return String.valueOf(idObj);
        }
        JSONObject data = normalizeDocumentObject(result.get("data"));
        idObj = firstJsonValue(data, "id", "skill_id", "name");
        return idObj == null ? null : String.valueOf(idObj);
    }

    @Override
    public String findDocumentIdByName(String spaceId, String fileName) {
        String legacyDocId = findLegacyDocumentIdByName(dbGptProperties.getSkillSpaceName(), fileName);
        if (legacyDocId != null) {
            return legacyDocId;
        }
        String url = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "?space_id=" + spaceId;
        try (HttpResponse response = HttpUtil.createRequest(Method.GET, url)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT list documents response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() != 200) {
                return null;
            }

            JSONObject result = JSON.parseObject(responseBody);
            JSONArray items = extractDocumentItems(result);
            if (items == null || items.isEmpty()) {
                log.warn("DB-GPT list documents returned no items, raw response={}", responseBody);
                return null;
            }

            for (int i = 0; i < items.size(); i++) {
                JSONObject doc = items.getJSONObject(i);
                log.debug("DB-GPT doc[{}]: {}", i, doc.toJSONString());
                String matchedId = matchDocumentId(doc, fileName);
                if (matchedId != null) {
                    return matchedId;
                }
            }

            // 部分 DB-GPT 版本列表接口只返回 id，文档名需要详情接口读取。
            for (int i = 0; i < items.size(); i++) {
                JSONObject doc = items.getJSONObject(i);
                Object idObj = firstJsonValue(doc, "id", "doc_id", "document_id");
                if (idObj == null) {
                    continue;
                }
                String matchedId = findDocumentIdByDetail(String.valueOf(idObj), fileName);
                if (matchedId != null) {
                    return matchedId;
                }
            }

            log.warn("DB-GPT no doc matched fileName='{}', existing docs:", fileName);
            for (int i = 0; i < items.size(); i++) {
                JSONObject doc = items.getJSONObject(i);
                log.warn("  doc[{}] id={}, doc_id={}, name={}, doc_name={}, title={}, file_name={}",
                        i, doc.get("id"), doc.get("doc_id"), doc.getString("name"), doc.getString("doc_name"),
                        doc.getString("title"), doc.getString("file_name"));
            }
            return null;
        } catch (Exception e) {
            log.warn("查询DB-GPT文档ID失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteDocument(String docId) {
        String url = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "/" + docId;
        try (HttpResponse response = HttpUtil.createRequest(Method.DELETE, url)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            if (response.getStatus() != 200) {
                String body = response.body();
                log.warn("删除DB-GPT文档非200响应: status={}, body={}", response.getStatus(), body);
                return;
            }
            log.info("DB-GPT document '{}' deleted", docId);
        } catch (Exception e) {
            log.warn("删除DB-GPT文档异常: {}", e.getMessage());
        }
    }

    @Override
    public String getDocumentStatus(String docId) {
        String detailUrl = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "/" + docId;
        try (HttpResponse response = HttpUtil.createRequest(Method.GET, detailUrl)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {
            String body = response.body();
            log.info("DB-GPT document detail response: status={}, body={}", response.getStatus(), body);
            if (response.getStatus() != 200) {
                return null;
            }
            JSONObject result = JSON.parseObject(body);
            JSONObject doc = normalizeDocumentObject(result.get("data"));
            String status = firstNonBlank(
                    doc.getString("status"),
                    doc.getString("state"),
                    doc.getString("sync_status"),
                    doc.getString("vector_status"),
                    doc.getString("document_status"),
                    result.getString("status"),
                    result.getString("state"));
            return status == null ? null : status.toLowerCase();
        } catch (Exception e) {
            log.warn("查询DB-GPT文档状态失败: docId={}, error={}", docId, e.getMessage());
            return null;
        }
    }

    @Override
    public String uploadDocumentToKnowledge(String spaceId, String fileName, String content) {
        return doWebTextDocumentAdd(dbGptProperties.getSkillSpaceName(), fileName, content);
    }

    private String doWebTextDocumentAdd(String spaceName, String fileName, String content) {
        String url = dbGptProperties.getUrl() + "/knowledge/" + encodePath(spaceName) + "/document/add";
        JSONObject body = new JSONObject();
        body.put("doc_name", fileName);
        body.put("source", fileName);
        body.put("content", content);
        body.put("doc_type", "TEXT");
        body.put("questions", new JSONArray());

        log.info("DB-GPT web add text document: url={}, docName={}, contentLength={}",
                url, fileName, content == null ? 0 : content.length());
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(dbGptProperties.getTimeout() * 2)
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT web add text document response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT文本文档创建失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
            }
            JSONObject result = JSON.parseObject(responseBody);
            if (result.containsKey("success") && !result.getBooleanValue("success")) {
                throw new ServiceException("DB-GPT文本文档创建失败: " + result.getString("err_msg"));
            }
            String documentId = extractDocumentId(result.get("data"), fileName);
            if (documentId != null) {
                return documentId;
            }
            documentId = extractDocumentId(result, fileName);
            if (documentId != null) {
                return documentId;
            }
            documentId = findDocumentIdByName(null, fileName);
            if (documentId != null) {
                return documentId;
            }
            throw new ServiceException("DB-GPT文本文档创建成功但未返回文档ID：" + responseBody);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT文本文档创建异常: " + e.getMessage());
        }
    }

    private String doLegacyUploadDocument(String spaceName, String fileName, String content) {
        String url = dbGptProperties.getUrl() + LEGACY_KNOWLEDGE_PATH + "/" + encodePath(spaceName) + "/document/upload";
        File tempFile = null;
        try {
            tempFile = File.createTempFile("dbgpt-", ".md");
            FileUtil.writeUtf8String(content, tempFile);

            log.info("DB-GPT legacy upload document: url={}, spaceName={}, fileName={}, tempFile={}",
                    url, spaceName, fileName, tempFile.getAbsolutePath());
            try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                    .form("doc_name", fileName)
                    .form("doc_type", "DOCUMENT")
                    .form("doc_file", tempFile)
                    .timeout(dbGptProperties.getTimeout() * 2)
                    .execute()) {

                String responseBody = response.body();
                log.info("DB-GPT legacy upload document response: status={}, body={}", response.getStatus(), responseBody);
                if (response.getStatus() == 404 || response.getStatus() == 405) {
                    throw new ServiceException("DB-GPT原生知识库上传接口不可用，状态码: " + response.getStatus()
                            + "，请确认接口路径: " + url + "，响应: " + responseBody);
                }
                if (response.getStatus() != 200) {
                    throw new ServiceException("DB-GPT文档上传失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
                }

                JSONObject result = JSON.parseObject(responseBody);
                if (result.containsKey("success") && !result.getBooleanValue("success")) {
                    throw new ServiceException("DB-GPT文档上传失败: " + result.getString("err_msg"));
                }
                String documentId = extractDocumentId(result.get("data"), fileName);
                if (documentId != null) {
                    return documentId;
                }
                documentId = findLegacyDocumentIdByName(spaceName, fileName);
                if (documentId != null) {
                    return documentId;
                }
                throw new ServiceException("DB-GPT文档上传接口返回成功，但知识库中未找到文档：" + fileName
                        + "，上传响应: " + responseBody);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT文档上传异常: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private String doUploadDocument(String spaceId, String fileName, String content, boolean retryOnConflict) {
        String url = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH;
        File tempFile = null;
        try {
            tempFile = File.createTempFile("dbgpt-", ".md");
            FileUtil.writeUtf8String(content, tempFile);

            log.info("DB-GPT upload document: url={}, spaceId={}, fileName={}, tempFile={}",
                    url, spaceId, fileName, tempFile.getAbsolutePath());
            try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                    .form("space_id", spaceId)
                    .form("doc_name", fileName)
                    .form("doc_type", "DOCUMENT")
                    .form("doc_file", tempFile)
                    .timeout(dbGptProperties.getTimeout() * 2)
                    .execute()) {

                String responseBody = response.body();
                log.info("DB-GPT upload document response: status={}, body={}", response.getStatus(), responseBody);
                if (response.getStatus() != 200) {
                    // 如果是文档名冲突并且允许重试，则尝试删除旧文档后重试
                    if (retryOnConflict && responseBody != null && responseBody.contains("have already named")) {
                        log.warn("DB-GPT文档名冲突，尝试删除旧文档后重试: fileName={}", fileName);
                        deleteExistingDocument(spaceId, fileName);
                        return doUploadDocument(spaceId, fileName, content, false);
                    }
                    throw new ServiceException("DB-GPT文档上传失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
                }

                JSONObject result = JSON.parseObject(responseBody);
                if (!result.getBooleanValue("success")) {
                    if (retryOnConflict && result.getString("err_msg") != null && result.getString("err_msg").contains("have already named")) {
                        log.warn("DB-GPT文档名冲突，尝试删除旧文档后重试: fileName={}", fileName);
                        deleteExistingDocument(spaceId, fileName);
                        return doUploadDocument(spaceId, fileName, content, false);
                    }
                    throw new ServiceException("DB-GPT文档上传失败: " + result.getString("err_msg"));
                }

                String documentId = extractDocumentId(result.get("data"), fileName);
                if (documentId != null) {
                    return documentId;
                }
                log.info("DB-GPT document '{}' uploaded to space '{}'", fileName, spaceId);
                return findDocumentIdByName(spaceId, fileName);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT文档上传异常: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private void deleteExistingDocument(String spaceId, String fileName) {
        try {
            // 先列出空间中的所有文档 ID
            String listUrl = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "?space_id=" + spaceId;
            try (HttpResponse resp = HttpUtil.createRequest(Method.GET, listUrl)
                    .header("Content-Type", "application/json")
                    .timeout(dbGptProperties.getTimeout())
                    .execute()) {

                String listBody = resp.body();
                if (resp.getStatus() != 200) {
                    log.warn("DB-GPT list documents failed: status={}", resp.getStatus());
                    return;
                }

                // 解析列表，获取文档 ID 列表
                JSONArray ids = new JSONArray();
                Object parsed = JSON.parse(listBody);
                if (parsed instanceof JSONObject) {
                    JSONObject obj = (JSONObject) parsed;
                    Object dataObj = obj.get("data");
                    JSONObject data = dataObj instanceof JSONObject ? (JSONObject) dataObj : null;
                    if (data != null) {
                        JSONArray items = data.getJSONArray("items");
                        JSONArray records = data.getJSONArray("records");
                        JSONArray arr = items != null ? items : records;
                        if (arr != null) {
                            for (int i = 0; i < arr.size(); i++) {
                                JSONObject item = arr.getJSONObject(i);
                                if (item.containsKey("id")) {
                                    ids.add(item.get("id"));
                                }
                            }
                        }
                    }
                }

                if (ids.isEmpty()) {
                    log.warn("DB-GPT list documents returned no IDs, cannot delete old doc");
                    return;
                }

                // 逐个查询文档详情（因为列表 API 不返回 doc_name）
                for (int i = 0; i < ids.size(); i++) {
                    String docId = String.valueOf(ids.get(i));
                    String detailUrl = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "/" + docId;
                    try (HttpResponse detailResp = HttpUtil.createRequest(Method.GET, detailUrl)
                            .header("Content-Type", "application/json")
                            .timeout(dbGptProperties.getTimeout())
                            .execute()) {

                        String detailBody = detailResp.body();
                        if (detailResp.getStatus() != 200) {
                            continue;
                        }
                        // 详情返回格式: {"data":[["id",1],["doc_name","..."],...]}
                        JSONObject detailResult = JSON.parseObject(detailBody);
                        Object dataObj = detailResult.get("data");
                        if (dataObj instanceof JSONArray) {
                            JSONArray pairs = (JSONArray) dataObj;
                            String docName = null;
                            for (int j = 0; j < pairs.size(); j++) {
                                JSONArray pair = pairs.getJSONArray(j);
                                if (pair.size() == 2 && "doc_name".equals(pair.getString(0))) {
                                    docName = pair.getString(1);
                                    break;
                                }
                            }
                            if (fileName.equals(docName)) {
                                log.info("Found existing document '{}' (id={}), deleting...", fileName, docId);
                                String delUrl = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "/" + docId;
                                try (HttpResponse delResp = HttpUtil.createRequest(Method.DELETE, delUrl)
                                        .header("Content-Type", "application/json")
                                        .timeout(dbGptProperties.getTimeout())
                                        .execute()) {
                                    log.info("Delete document response: status={}", delResp.getStatus());
                                }
                                return;
                            }
                        }
                    }
                }
                log.warn("No existing document found with name '{}' among {} docs", fileName, ids.size());
            }
        } catch (Exception e) {
            log.warn("删除旧文档异常: {}", e.getMessage());
        }
    }

    private JSONArray extractDocumentItems(JSONObject result) {
        if (result == null) {
            return null;
        }
        JSONArray items = extractDocumentItemsFromValue(result.get("data"));
        if (items != null) {
            return items;
        }
        String[] rootFields = {"items", "records", "rows", "list", "documents", "docs", "results"};
        for (String field : rootFields) {
            items = result.getJSONArray(field);
            if (items != null) {
                return items;
            }
        }
        return null;
    }

    private String findLegacyDocumentIdByName(String spaceName, String fileName) {
        String url = dbGptProperties.getUrl() + LEGACY_KNOWLEDGE_PATH + "/" + encodePath(spaceName) + "/document/list";
        JSONObject body = new JSONObject();
        body.put("page", 1);
        body.put("page_size", 1000);
        body.put("pageSize", 1000);
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(dbGptProperties.getTimeout())
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT legacy list documents response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() == 404 || response.getStatus() == 405 || response.getStatus() != 200) {
                return null;
            }
            JSONObject result = JSON.parseObject(responseBody);
            JSONArray items = extractDocumentItems(result);
            if (items == null || items.isEmpty()) {
                return null;
            }
            for (int i = 0; i < items.size(); i++) {
                JSONObject doc = items.getJSONObject(i);
                String matchedId = matchDocumentId(doc, fileName);
                if (matchedId != null) {
                    return matchedId;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("查询DB-GPT legacy文档ID失败: {}", e.getMessage());
            return null;
        }
    }

    private JSONArray extractDocumentItemsFromValue(Object value) {
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            String[] fields = {"items", "records", "rows", "list", "documents", "docs", "results", "data"};
            for (String field : fields) {
                JSONArray array = object.getJSONArray(field);
                if (array != null) {
                    return array;
                }
                Object nested = object.get(field);
                if (nested instanceof JSONObject) {
                    JSONArray nestedArray = extractDocumentItemsFromValue(nested);
                    if (nestedArray != null) {
                        return nestedArray;
                    }
                }
            }
        }
        if (value instanceof String) {
            try {
                Object parsed = JSON.parse((String) value);
                return extractDocumentItemsFromValue(parsed);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String matchDocumentId(JSONObject doc, String fileName) {
        if (documentNameMatches(doc, fileName)) {
            Object idObj = firstJsonValue(doc, "id", "doc_id", "document_id");
            return idObj == null ? null : String.valueOf(idObj);
        }
        return null;
    }

    private boolean documentNameMatches(JSONObject doc, String fileName) {
        if (doc == null) {
            return false;
        }
        String[] nameFields = {"doc_name", "name", "document_name", "title", "file_name", "fileName"};
        for (String field : nameFields) {
            if (fileName.equals(doc.getString(field))) {
                return true;
            }
        }
        return false;
    }

    private String findDocumentIdByDetail(String docId, String fileName) {
        String detailUrl = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "/" + docId;
        try (HttpResponse response = HttpUtil.createRequest(Method.GET, detailUrl)
                .header("Content-Type", "application/json")
                .timeout(dbGptProperties.getTimeout())
                .execute()) {
            if (response.getStatus() != 200) {
                return null;
            }
            JSONObject result = JSON.parseObject(response.body());
            JSONObject doc = normalizeDocumentObject(result.get("data"));
            String matchedId = matchDocumentId(doc, fileName);
            if (matchedId != null) {
                return matchedId;
            }
            return documentNameMatches(doc, fileName) ? docId : null;
        } catch (Exception e) {
            log.warn("查询DB-GPT文档详情失败: docId={}, error={}", docId, e.getMessage());
            return null;
        }
    }

    private String extractDocumentId(Object dataObj, String fileName) {
        if (dataObj instanceof Number) {
            return String.valueOf(dataObj);
        }
        if (dataObj instanceof String && ((String) dataObj).matches("\\d+")) {
            return (String) dataObj;
        }
        JSONObject doc = normalizeDocumentObject(dataObj);
        String matchedId = matchDocumentId(doc, fileName);
        if (matchedId != null) {
            return matchedId;
        }
        Object idObj = firstJsonValue(doc, "id", "doc_id", "document_id");
        if (idObj != null) {
            return String.valueOf(idObj);
        }
        JSONArray items = extractDocumentItemsFromValue(dataObj);
        if (items == null) {
            return null;
        }
        for (int i = 0; i < items.size(); i++) {
            String id = extractDocumentId(items.get(i), fileName);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private JSONObject normalizeDocumentObject(Object dataObj) {
        if (dataObj == null) {
            return new JSONObject();
        }
        if (dataObj instanceof JSONObject) {
            return (JSONObject) dataObj;
        }
        if (dataObj instanceof String) {
            try {
                return normalizeDocumentObject(JSON.parse((String) dataObj));
            } catch (Exception ignored) {
                return null;
            }
        }
        if (!(dataObj instanceof JSONArray)) {
            return new JSONObject();
        }
        JSONArray array = (JSONArray) dataObj;
        JSONObject object = new JSONObject();
        boolean pairArray = false;
        for (int i = 0; i < array.size(); i++) {
            Object item = array.get(i);
            if (item instanceof JSONObject) {
                return (JSONObject) item;
            }
            if (item instanceof JSONArray) {
                JSONArray pair = (JSONArray) item;
                if (pair.size() >= 2) {
                    pairArray = true;
                    object.put(pair.getString(0), pair.get(1));
                }
            }
        }
        return pairArray ? object : new JSONObject();
    }

    private String encodePath(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    private Object firstJsonValue(JSONObject object, String... keys) {
        if (object == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = object.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void syncDocument(String spaceId, String docId, String fileName) {
        syncWebDocumentBatch(dbGptProperties.getSkillSpaceName(), docId, fileName);
    }

    private void syncWebDocumentBatch(String spaceName, String docId, String fileName) {
        String url = dbGptProperties.getUrl() + "/knowledge/" + encodePath(spaceName) + "/document/sync_batch";
        JSONArray body = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("name", fileName);
        item.put("doc_id", parseNumericOrRaw(docId));
        JSONObject chunkParameters = new JSONObject();
        chunkParameters.put("chunk_strategy", "Automatic");
        item.put("chunk_parameters", chunkParameters);
        body.add(item);

        log.info("DB-GPT web sync batch document: url={}, body={}", url, body.toJSONString());
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(dbGptProperties.getTimeout() * 5)
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT web sync batch document response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT文档批量同步失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
            }
            JSONObject result = JSON.parseObject(responseBody);
            if (result.containsKey("success") && !result.getBooleanValue("success")) {
                throw new ServiceException("DB-GPT文档批量同步失败: " + result.getString("err_msg"));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT文档批量同步异常: " + e.getMessage());
        }
    }

    private void syncLegacyDocument(String spaceName, String docId) {
        String url = dbGptProperties.getUrl() + LEGACY_KNOWLEDGE_PATH + "/" + encodePath(spaceName) + "/document/sync";
        JSONObject body = new JSONObject();
        JSONArray docIds = new JSONArray();
        docIds.add(parseNumericOrRaw(docId));
        body.put("doc_ids", docIds);
        body.put("model_name", dbGptProperties.getModel());

        log.info("DB-GPT legacy sync document: url={}, body={}", url, body.toJSONString());
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(dbGptProperties.getTimeout() * 5)
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT legacy sync document response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() == 404 || response.getStatus() == 405) {
                throw new ServiceException("DB-GPT原生知识库同步接口不可用，状态码: " + response.getStatus()
                        + "，请确认接口路径: " + url + "，响应: " + responseBody);
            }
            if (response.getStatus() != 200) {
                if (isDbGptAsyncSerializeBug(responseBody)) {
                    log.warn("DB-GPT legacy文档同步接口触发异步任务后响应序列化失败，按已触发处理: docId={}, response={}", docId, responseBody);
                    return;
                }
                throw new ServiceException("DB-GPT文档同步失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
            }
            JSONObject result = JSON.parseObject(responseBody);
            if (result.containsKey("success") && !result.getBooleanValue("success")) {
                throw new ServiceException("DB-GPT文档同步失败: " + result.getString("err_msg"));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT文档同步异常: " + e.getMessage());
        }
    }

    private void syncServeDocument(String spaceId, String docId) {
        String url = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "/sync";
        Object docIdValue = parseNumericOrRaw(docId);

        JSONObject docIdsBody = new JSONObject();
        docIdsBody.put("space_id", spaceId);
        JSONArray docIds = new JSONArray();
        docIds.add(docIdValue);
        docIdsBody.put("doc_ids", docIds);
        if (doSyncServeDocument(url, docIdsBody, docId)) {
            return;
        }

        JSONObject docIdBody = new JSONObject();
        docIdBody.put("doc_id", docIdValue);
        docIdBody.put("space_id", spaceId);
        if (doSyncServeDocument(url, docIdBody, docId)) {
            return;
        }

        JSONArray objectArrayBody = new JSONArray();
        objectArrayBody.add(docIdBody);
        if (doSyncServeDocument(url, objectArrayBody, docId)) {
            return;
        }

        JSONArray idArrayBody = new JSONArray();
        idArrayBody.add(docIdValue);
        if (doSyncServeDocument(url, idArrayBody, docId)) {
            return;
        }
        throw new ServiceException("DB-GPT文档同步失败，已尝试多种参数格式: docId=" + docId);
    }

    private boolean doSyncServeDocument(String url, Object body, String docId) {
        String bodyText = JSON.toJSONString(body);
        log.info("DB-GPT sync document: url={}, body={}", url, bodyText);
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(bodyText)
                .timeout(dbGptProperties.getTimeout() * 5)
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT sync document response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() != 200) {
                if (isDbGptAsyncSerializeBug(responseBody)) {
                    log.warn("DB-GPT文档同步接口返回coroutine序列化异常，继续尝试下一种参数格式: response={}", responseBody);
                    return false;
                }
                log.warn("DB-GPT文档同步失败，状态码: {}，响应: {}", response.getStatus(), responseBody);
                return false;
            }
            JSONObject result = JSON.parseObject(responseBody);
            if (result.containsKey("success") && !result.getBooleanValue("success")) {
                log.warn("DB-GPT文档同步失败: {}", result.getString("err_msg"));
                return false;
            }
            log.info("DB-GPT document sync triggered, checking document status");
            String status = getDocumentStatus(docId);
            if ("todo".equalsIgnoreCase(status)) {
                log.warn("DB-GPT文档同步请求返回成功但状态仍为todo，尝试下一种参数格式: body={}", bodyText);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("DB-GPT文档同步异常: {}", e.getMessage());
            return false;
        }
    }

    private boolean isDbGptAsyncSerializeBug(String responseBody) {
        return responseBody != null
                && responseBody.contains("Unable to serialize unknown type")
                && responseBody.contains("coroutine");
    }

    private Object parseNumericOrRaw(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return value;
        }
    }
}
