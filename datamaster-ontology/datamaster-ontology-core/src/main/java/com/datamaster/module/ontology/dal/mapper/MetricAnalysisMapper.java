package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricAnalysisPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.MetricAnalysisDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MetricAnalysisMapper extends BaseMapperX<MetricAnalysisDO> {
    @Select("SELECT * FROM ont_metric_analysis WHERE id = #{id} AND owner_id = #{ownerId} FOR UPDATE")
    MetricAnalysisDO selectOwnedForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);
    default PageResult<MetricAnalysisDO> selectOwnedPage(MetricAnalysisPageReqVO req, Long ownerId) {
        return selectPage(req, new LambdaQueryWrapperX<MetricAnalysisDO>()
                .eq(MetricAnalysisDO::getOntologyId, req.getOntologyId())
                .eq(MetricAnalysisDO::getOwnerId, ownerId)
                .like(req.getName() != null && !req.getName().trim().isEmpty(), MetricAnalysisDO::getName, req.getName())
                .orderByDesc(MetricAnalysisDO::getId));
    }
}
