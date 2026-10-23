package com.datamaster.module.ai.service.skill;

import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlRespVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI ask-data service interface.
 */
public interface IAiAskDataService {

    AiAskDataSqlRespVO chatWithDbGpt(AiAskDataSqlReqVO reqVO);

    /** Agent 后台线程调用时显式传递已认证的用户身份，避免依赖丢失的 SecurityContext。 */
    AiAskDataSqlRespVO chatWithDbGpt(AiAskDataSqlReqVO reqVO, Long userId);

    SseEmitter chatWithDbGptStream(AiAskDataSqlReqVO reqVO);

    AiAskDataReportRespVO generateReport(AiAskDataReportReqVO reqVO);

    /** Agent 后台线程调用时显式传递已认证的用户身份。 */
    AiAskDataReportRespVO generateReport(AiAskDataReportReqVO reqVO, Long userId);
}

