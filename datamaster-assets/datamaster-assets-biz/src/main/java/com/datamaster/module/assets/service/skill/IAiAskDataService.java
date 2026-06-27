package com.datamaster.module.assets.service.skill;

import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataPrepareReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataPrepareRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataSqlReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataSqlRespVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI ask-data service interface.
 */
public interface IAiAskDataService {

    /**
     * Prepare ask-data context (search skills, build prompt).
     *
     * @param reqVO prepare request
     * @return prepared context
     */
    AiAskDataPrepareRespVO prepare(AiAskDataPrepareReqVO reqVO);

    /**
     * Generate SQL from user question.
     *
     * @param reqVO SQL generation request
     * @return SQL and explanation
     */
    AiAskDataSqlRespVO generateSql(AiAskDataSqlReqVO reqVO);

    /**
     * Execute confirmed SQL query.
     *
     * @param datasourceId datasource ID
     * @param sql          SQL to execute
     * @param maxRows      max rows to return
     * @return query results
     */
    List<Map<String, Object>> executeSql(Long datasourceId, String sql, Integer maxRows);

    /**
     * One-stop ask-data chat (prepare + generate + optional execute).
     *
     * @param reqVO chat request (same as SQL request)
     * @return full response with SQL and optional results
     */
    AiAskDataSqlRespVO chat(AiAskDataSqlReqVO reqVO);

    AiAskDataSqlRespVO chatWithDbGpt(AiAskDataSqlReqVO reqVO);

    SseEmitter chatWithDbGptStream(AiAskDataSqlReqVO reqVO);
}
