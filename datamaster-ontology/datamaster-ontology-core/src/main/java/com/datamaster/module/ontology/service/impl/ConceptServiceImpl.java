package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.ConceptConvert;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptPageReqVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptRespVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.service.IConceptService;
import com.datamaster.module.ontology.service.IConceptTableService;
import com.datamaster.module.ontology.service.IPropertyService;
import com.datamaster.module.ontology.service.IRelationService;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 本体概念 Service 实现
 */
@Service
@Validated
public class ConceptServiceImpl implements IConceptService {

    @Resource
    private ConceptMapper conceptMapper;
    @Resource
    private PropertyMapper propertyMapper;
    @Resource
    private ConceptTableMapper conceptTableMapper;
    @Resource
    private RelationMapper relationMapper;
    @Resource
    private ActionMapper actionMapper;
    @Resource
    private IPropertyService propertyService;
    @Resource
    private IConceptTableService conceptTableService;
    @Resource
    private IRelationService relationService;
    @Resource
    private MetricReferenceGuard metricReferenceGuard;
    @Resource private com.datamaster.module.ontology.service.analysis.MetricAnalysisAccessGuard ownership;
    @Resource private com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper metadataLocks;

    @Override
    public PageResult<ConceptRespVO> getConceptPage(ConceptPageReqVO pageReqVO) {
        ownership.ontology(pageReqVO.getOntologyId());
        PageResult<ConceptDO> pageResult = conceptMapper.selectPage(pageReqVO);
        List<ConceptRespVO> list = ConceptConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public ConceptRespVO getConceptById(Long id) {
        ConceptDO concept = conceptMapper.selectById(id);
        if (concept != null) ownership.ontology(concept.getOntologyId());
        return concept != null ? ConceptConvert.INSTANCE.convert(concept) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConcept(ConceptSaveReqVO createReqVO) {
        if (createReqVO == null || createReqVO.getId() != null) throw new com.datamaster.common.exception.ServiceException("新建概念不得指定已有ID");
        ownership.ontology(createReqVO.getOntologyId());
        if (metadataLocks.lockOntology(createReqVO.getOntologyId()) == null) throw new com.datamaster.common.exception.ServiceException("本体不存在");
        ownership.ontology(createReqVO.getOntologyId());
        ConceptDO concept = ConceptConvert.INSTANCE.convert(createReqVO);
        if (conceptMapper.insert(concept) != 1) throw new com.datamaster.common.exception.ServiceException("概念保存失败");
        return concept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateConcept(ConceptSaveReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) throw new com.datamaster.common.exception.ServiceException("编辑概念必须指定ID");
        ConceptDO existing = conceptMapper.selectById(updateReqVO.getId());
        if (existing == null) throw new com.datamaster.common.exception.ServiceException("概念不存在");
        ownership.ontology(existing.getOntologyId());
        Long ontologyId = existing.getOntologyId();
        if (metadataLocks.lockOntology(ontologyId) == null) throw new com.datamaster.common.exception.ServiceException("本体不存在");
        existing = conceptMapper.selectById(updateReqVO.getId());
        if (existing == null || !java.util.Objects.equals(ontologyId, existing.getOntologyId())) throw new com.datamaster.common.exception.ServiceException("概念已变化，请刷新");
        ownership.ontology(ontologyId);
        if (updateReqVO.getOntologyId() != null && !java.util.Objects.equals(existing.getOntologyId(), updateReqVO.getOntologyId())) throw new com.datamaster.common.exception.ServiceException("普通编辑不能更改概念所属本体");
        ConceptDO concept = ConceptConvert.INSTANCE.convert(updateReqVO);
        concept.setOntologyId(existing.getOntologyId());
        return conceptMapper.updateById(concept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteConcept(Long id) {
        ConceptDO existing = conceptMapper.selectById(id);
        if (existing == null) throw new com.datamaster.common.exception.ServiceException("概念不存在");
        ownership.ontology(existing.getOntologyId());
        metricReferenceGuard.assertConceptDeletable(id);
        for (RelationDO relation : relationMapper.selectList(new LambdaQueryWrapperX<RelationDO>()
                .and(wrapper -> wrapper.eq(RelationDO::getSourceConceptId, id)
                        .or().eq(RelationDO::getTargetConceptId, id)))) {
            relationService.deleteRelation(relation.getId());
        }
        for (PropertyDO property : propertyMapper.selectList(new LambdaQueryWrapperX<PropertyDO>()
                .eq(PropertyDO::getConceptId, id))) {
            propertyService.deleteProperty(property.getId());
        }
        for (ConceptTableDO conceptTable : conceptTableMapper.selectByConceptId(id)) {
            conceptTableService.deleteConceptTable(conceptTable.getId());
        }
        actionMapper.delete(new LambdaQueryWrapperX<ActionDO>().eq(ActionDO::getConceptId, id));
        return conceptMapper.deleteById(id);
    }
}
