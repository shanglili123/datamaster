package com.datamaster.module.ai.service.dbgpt;

import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatCompletionRequest;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatCompletionResponse;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptDatasourceCreateRequest;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptDatasourceResponse;

import java.util.List;
import java.util.function.Consumer;

public interface IDbGptClientService {

    DbGptChatCompletionResponse chatCompletion(DbGptChatCompletionRequest request);

    DbGptChatCompletionResponse chatCompletionV1(DbGptChatCompletionRequest request);

    void chatCompletionV1Stream(DbGptChatCompletionRequest request, Consumer<String> onMessage);

    String chatCompletionStream(DbGptChatCompletionRequest request);

    Integer createDatasource(DbGptDatasourceCreateRequest request);

    List<DbGptDatasourceResponse> listDatasources(String dbType);

    void deleteDatasource(Integer id);

    String createKnowledgeSpace(String name, String vectorType, String owner);

    String getKnowledgeSpaceId(String name);

    void deleteKnowledgeSpace(String spaceId);

    String getDatasourceDbName(Integer datasourceId);

    String uploadSkill(String fileName, String content);

    String uploadDocumentToKnowledge(String spaceId, String fileName, String content);

    void syncDocument(String spaceId, String docId, String fileName);

    String findDocumentIdByName(String spaceId, String fileName);

    String getDocumentStatus(String docId);

    void deleteDocument(String docId);
}

