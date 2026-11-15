package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import java.util.List;

public interface IMetricService {
    PageResult<MetricRespVO> getPage(MetricPageReqVO req);
    /** Formal discovery uses published names and exact immutable codes before pagination. */
    PageResult<MetricRespVO> getPublishedPage(MetricPageReqVO req, String code);
    MetricRespVO get(Long id);
    Long create(MetricSaveReqVO req);
    Integer update(MetricSaveReqVO req);
    Integer delete(Long id);
    void validate(Long id);
    void validate(Long id, Integer revision);
    void publish(Long id, Integer revision);
    void disable(Long id, Integer revision);
    Integer restoreDraft(Long id, Integer version, Integer revision);
    List<MetricVersionRespVO> versions(Long id);
    MetricRespVO version(Long id, Integer version);
}
