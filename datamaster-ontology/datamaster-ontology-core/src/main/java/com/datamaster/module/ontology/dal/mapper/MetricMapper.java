package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

public interface MetricMapper extends BaseMapperX<MetricDO> {
    @Select("SELECT * FROM ont_metric WHERE id = #{id} FOR UPDATE")
    MetricDO selectForUpdate(@Param("id") Long id);

    /** Filter and paginate on the current immutable publication, never on draft names. */
    @Select({"<script>",
            "SELECT m.* FROM ont_metric m",
            "JOIN ont_metric_version v ON v.metric_id = m.id AND v.version = m.published_version",
            "WHERE m.ontology_id = #{req.ontologyId} AND m.status = 'PUBLISHED'",
            "<if test='req.conceptId != null'>AND m.concept_id = #{req.conceptId}</if>",
            "<if test='req.name != null and req.name.trim().length() > 0'>",
            "AND STRPOS(LOWER(CAST(v.snapshot_json AS jsonb) -&gt;&gt; 'name'), LOWER(#{req.name})) &gt; 0",
            "</if>",
            "<if test='code != null and code.trim().length() > 0'>AND LOWER(m.code) = LOWER(#{code})</if>",
            "ORDER BY m.id DESC",
            "</script>"})
    IPage<MetricDO> selectPublishedPage(IPage<MetricDO> page, @Param("req") MetricPageReqVO req,
                                       @Param("code") String code);

    default PageResult<MetricDO> selectPage(MetricPageReqVO req) {
        return selectPage(req, new LambdaQueryWrapperX<MetricDO>()
                .eq(MetricDO::getOntologyId, req.getOntologyId())
                .eq(req.getConceptId() != null, MetricDO::getConceptId, req.getConceptId())
                .like(StringUtils.isNotBlank(req.getName()), MetricDO::getName, req.getName())
                .eq(StringUtils.isNotBlank(req.getStatus()), MetricDO::getStatus, req.getStatus())
                .orderByDesc(MetricDO::getId));
    }
}
