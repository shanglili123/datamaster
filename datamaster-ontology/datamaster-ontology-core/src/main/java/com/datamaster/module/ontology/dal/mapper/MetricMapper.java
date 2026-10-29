package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

public interface MetricMapper extends BaseMapperX<MetricDO> {
    @Select("SELECT * FROM ont_metric WHERE id = #{id} FOR UPDATE")
    MetricDO selectForUpdate(@Param("id") Long id);

    default PageResult<MetricDO> selectPage(MetricPageReqVO req) {
        return selectPage(req, new LambdaQueryWrapperX<MetricDO>()
                .eq(MetricDO::getOntologyId, req.getOntologyId())
                .eq(req.getConceptId() != null, MetricDO::getConceptId, req.getConceptId())
                .like(StringUtils.isNotBlank(req.getName()), MetricDO::getName, req.getName())
                .eq(StringUtils.isNotBlank(req.getStatus()), MetricDO::getStatus, req.getStatus())
                .orderByDesc(MetricDO::getId));
    }
}
