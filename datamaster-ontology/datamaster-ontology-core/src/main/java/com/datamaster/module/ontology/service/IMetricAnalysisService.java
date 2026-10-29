package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;

public interface IMetricAnalysisService {
    MetricQueryRespVO query(MetricQueryReqVO req);
    PageResult<MetricAnalysisRespVO> page(MetricAnalysisPageReqVO req);
    Long create(MetricAnalysisSaveReqVO req);
    Integer update(MetricAnalysisSaveReqVO req);
    MetricAnalysisRespVO get(Long id);
    Integer delete(Long id);
}
