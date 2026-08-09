package com.datamaster.module.system.service.monitor;

import com.datamaster.module.system.controller.admin.monitor.vo.AiOpsReportRespVO;

/**
 * AI ops diagnosis service.
 */
public interface IAiOpsService {

    AiOpsReportRespVO diagnose() throws Exception;
}
