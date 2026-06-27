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
import java.nio.charset.StandardCharsets;
import com.datamaster.module.assets.config.DbGptProperties;
import com.datamaster.module.assets.model.dto.dbgpt.*;
import com.datamaster.module.assets.service.dbgpt.IDbGptClientService;
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
            return extractContent(object);
        } catch (Exception ignored) {
            return value;
        }
    }

    private String extractContent(JSONObject object) {
        if (object == null) {
            return null;
        }
        String content = firstNonBlank(object.getString("text"), object.getString("content"), object.getString("message"));
        if (!content.isEmpty()) {
            return content;
        }
        Object data = object.get("data");
        if (data instanceof String) {
            return (String) data;
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
            String content = firstNonBlank(
                    object.getString("text"),
                    object.getString("content"),
                    object.getString("message"),
                    object.getString("data"),
                    responseBody
            );
            return emptyResponse(content);
        } catch (Exception ignored) {
            log.error(ignored.getMessage());
        }
        return emptyResponse(responseBody);
    }

    private JSONObject buildReactAgentBody(DbGptChatCompletionRequest request) {
        String dbName = firstNonBlank(request.getChatParam(), request.getDbName());
        JSONObject body = new JSONObject();
        body.put("conv_uid", firstNonBlank(request.getConvUid(), "dm-" + System.currentTimeMillis()));
        body.put("chat_mode", "chat_react_agent");
        body.put("model_name", firstNonBlank(request.getModel(), dbGptProperties.getModel()));
        body.put("user_input", buildReactAgentInput(dbName, toUserInputText(request)));
        body.put("temperature", request.getTemperature() == null ? 0.6 : request.getTemperature());
        body.put("max_new_tokens", request.getMaxTokens() == null ? 4000 : request.getMaxTokens());
        body.put("select_param", "");

        JSONObject extInfo = new JSONObject();
        extInfo.put("database_name", dbName);
        extInfo.put("database_type", toDbGptDbType(request.getDbType()));
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
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            DbGptChatMessage message = request.getMessages().get(i);
            if ("user".equals(message.getRole())) {
                return message.getContent();
            }
        }
        return request.getMessages().get(request.getMessages().size() - 1).getContent();
    }

    private String buildReactAgentInput(String dbName, String userInput) {
        if (dbName == null || dbName.trim().isEmpty()) {
            return userInput;
        }
        return "[Database: " + dbName + "] " + firstNonBlank(userInput);
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
            return data != null ? data.getString("db_name") : null;
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

    @Override
    public String findDocumentIdByName(String spaceId, String fileName) {
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

            // 尝试多种可能的 JSON 路径提取文档列表
            JSONObject result = JSON.parseObject(responseBody);
            JSONArray items = null;
            if (result.containsKey("data")) {
                Object dataObj = result.get("data");
                if (dataObj instanceof JSONArray) {
                    items = (JSONArray) dataObj;
                } else if (dataObj instanceof JSONObject) {
                    // 可能有 data.items 或 data.records
                    items = ((JSONObject) dataObj).getJSONArray("items");
                    if (items == null) {
                        items = ((JSONObject) dataObj).getJSONArray("records");
                    }
                }
            }
            if (items == null) {
                items = result.getJSONArray("items");
            }
            if (items == null) {
                items = result.getJSONArray("records");
            }
            // 空数组判断
            if (items == null || items.isEmpty()) {
                log.warn("DB-GPT list documents returned no items, raw response={}", responseBody);
                return null;
            }

            // 尝试多个可能的文档名字段
            String[] nameFields = {"doc_name", "name", "document_name", "title", "file_name"};
            for (int i = 0; i < items.size(); i++) {
                JSONObject doc = items.getJSONObject(i);
                log.debug("DB-GPT doc[{}]: {}", i, doc.toJSONString());
                for (String field : nameFields) {
                    if (doc.containsKey(field) && fileName.equals(doc.getString(field))) {
                        return String.valueOf(doc.get("id"));
                    }
                }
            }

            // 没匹配到，打印所有文档名便于调试
            log.warn("DB-GPT no doc matched fileName='{}', existing docs:", fileName);
            for (int i = 0; i < items.size(); i++) {
                JSONObject doc = items.getJSONObject(i);
                log.warn("  doc[{}] id={}, name={}, doc_name={}, title={}",
                        i, doc.get("id"), doc.getString("name"), doc.getString("doc_name"), doc.getString("title"));
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
    public String uploadDocumentToKnowledge(String spaceId, String fileName, String content) {
        return doUploadDocument(spaceId, fileName, content, true);
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

                Object dataObj = result.get("data");
                if (dataObj instanceof JSONObject) {
                    Object idObj = ((JSONObject) dataObj).get("id");
                    if (idObj != null) {
                        return String.valueOf(idObj);
                    }
                }
                log.info("DB-GPT document '{}' uploaded to space '{}'", fileName, spaceId);
                return null;
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

    @Override
    public void syncDocument(String spaceId, String docId) {
        String url = dbGptProperties.getUrl() + KNOWLEDGE_DOCUMENTS_PATH + "/sync";
        JSONArray body = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("doc_id", Integer.parseInt(docId));
        item.put("space_id", Integer.parseInt(spaceId));
        body.add(item);

        log.info("DB-GPT sync document: url={}, body={}", url, body.toJSONString());
        try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(dbGptProperties.getTimeout() * 5)
                .execute()) {

            String responseBody = response.body();
            log.info("DB-GPT sync document response: status={}, body={}", response.getStatus(), responseBody);
            if (response.getStatus() != 200) {
                throw new ServiceException("DB-GPT文档同步失败，状态码: " + response.getStatus() + "，响应: " + responseBody);
            }
            JSONObject result = JSON.parseObject(responseBody);
            if (!result.getBooleanValue("success")) {
                throw new ServiceException("DB-GPT文档同步失败: " + result.getString("err_msg"));
            }
            log.info("DB-GPT document '{}' sync triggered for space '{}'", docId, spaceId);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("DB-GPT文档同步异常: " + e.getMessage());
        }
    }
}
