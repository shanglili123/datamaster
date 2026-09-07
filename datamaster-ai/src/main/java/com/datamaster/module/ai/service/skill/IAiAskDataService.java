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

    SseEmitter chatWithDbGptStream(AiAskDataSqlReqVO reqVO);

    AiAskDataReportRespVO generateReport(AiAskDataReportReqVO reqVO);
}

