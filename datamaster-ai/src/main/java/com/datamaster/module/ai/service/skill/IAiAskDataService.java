package com.datamaster.module.ai.service.skill;

import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataPrepareReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataPrepareRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlRespVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
     * One-stop ask-data chat.
     *
     * @param reqVO chat request (same as SQL request)
     * @return full response with SQL and optional results
     */
    AiAskDataSqlRespVO chat(AiAskDataSqlReqVO reqVO);

    AiAskDataSqlRespVO chatWithDbGpt(AiAskDataSqlReqVO reqVO);

    SseEmitter chatWithDbGptStream(AiAskDataSqlReqVO reqVO);

    AiAskDataReportRespVO generateReport(AiAskDataReportReqVO reqVO);
}

