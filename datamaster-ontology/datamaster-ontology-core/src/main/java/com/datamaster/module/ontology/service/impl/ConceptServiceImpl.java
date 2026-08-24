package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.ConceptConvert;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptPageReqVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptRespVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.service.IConceptService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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

    @Override
    public PageResult<ConceptRespVO> getConceptPage(ConceptPageReqVO pageReqVO) {
        PageResult<ConceptDO> pageResult = conceptMapper.selectPage(pageReqVO);
        List<ConceptRespVO> list = ConceptConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public ConceptRespVO getConceptById(Long id) {
        ConceptDO concept = conceptMapper.selectById(id);
        return concept != null ? ConceptConvert.INSTANCE.convert(concept) : null;
    }

    @Override
    public Long createConcept(ConceptSaveReqVO createReqVO) {
        ConceptDO concept = ConceptConvert.INSTANCE.convert(createReqVO);
        conceptMapper.insert(concept);
        return concept.getId();
    }

    @Override
    public Integer updateConcept(ConceptSaveReqVO updateReqVO) {
        ConceptDO concept = ConceptConvert.INSTANCE.convert(updateReqVO);
        return conceptMapper.updateById(concept);
    }

    @Override
    public Integer deleteConcept(Long id) {
        return conceptMapper.deleteById(id);
    }
}
