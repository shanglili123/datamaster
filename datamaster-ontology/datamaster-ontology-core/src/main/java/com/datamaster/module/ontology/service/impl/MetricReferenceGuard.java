package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper;
import com.datamaster.module.ontology.dal.mapper.MetricMapper;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 在级联删除任何绑定之前检查指标引用，停用指标也保留历史依赖。 */
@Component
public class MetricReferenceGuard {
    @Resource
    private MetricMapper metricMapper;
    @Resource
    private MetricMetadataLockMapper metadataLockMapper;
    @Resource
    private ConceptMapper conceptMapper;
    @Resource
    private ConceptTableMapper conceptTableMapper;

    public void assertOntologyDeletable(Long ontologyId) {
        lockOntology(ontologyId);
        if (metricMapper.selectCount(new LambdaQueryWrapperX<MetricDO>()
                .eq(MetricDO::getOntologyId, ontologyId)) > 0) {
            throw new ServiceException("本体仍有指标引用，不能删除；已发布指标需保留历史版本");
        }
    }

    public void assertConceptDeletable(Long conceptId) {
        ConceptDO concept = conceptMapper.selectById(conceptId);
        if (concept == null) throw new ServiceException("概念不存在");
        lockOntology(concept.getOntologyId());
        if (metricMapper.selectCount(new LambdaQueryWrapperX<MetricDO>()
                .eq(MetricDO::getConceptId, conceptId)) > 0) {
            throw new ServiceException("概念仍有指标引用，不能删除；请先处理指标草稿及发布历史");
        }
    }

    public void assertTableBindingDeletable(Long conceptTableId) {
        ConceptTableDO binding = conceptTableMapper.selectById(conceptTableId);
        if (binding == null) throw new ServiceException("表绑定不存在");
        ConceptDO concept = conceptMapper.selectById(binding.getConceptId());
        if (concept == null) throw new ServiceException("表绑定所属概念不存在");
        lockOntology(concept.getOntologyId());
        if (metricMapper.selectCount(new LambdaQueryWrapperX<MetricDO>()
                .eq(MetricDO::getConceptTableId, conceptTableId)) > 0) {
            throw new ServiceException("表绑定仍有指标引用，不能删除；请先处理引用指标");
        }
    }

    private void lockOntology(Long ontologyId) {
        if (ontologyId == null || metadataLockMapper.lockOntology(ontologyId) == null) {
            throw new ServiceException("本体不存在或已删除");
        }
    }
}
