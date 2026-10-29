package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import java.util.List;

public interface IMetricService {
    PageResult<MetricRespVO> getPage(MetricPageReqVO req);
    MetricRespVO get(Long id);
    Long create(MetricSaveReqVO req);
    Integer update(MetricSaveReqVO req);
    Integer delete(Long id);
    void validate(Long id);
    void publish(Long id, Integer revision);
    void disable(Long id, Integer revision);
    List<MetricVersionRespVO> versions(Long id);
    MetricRespVO version(Long id, Integer version);
}
