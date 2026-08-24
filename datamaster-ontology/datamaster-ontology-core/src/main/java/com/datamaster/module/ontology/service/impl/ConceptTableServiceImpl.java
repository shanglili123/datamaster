package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.ConceptTableConvert;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTablePageReqVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableRespVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.service.IConceptTableService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

/**
 * 概念表绑定 Service 实现
 */
@Service
@Validated
public class ConceptTableServiceImpl implements IConceptTableService {

    @Resource
    private ConceptTableMapper conceptTableMapper;

    @Override
    public PageResult<ConceptTableRespVO> getConceptTablePage(ConceptTablePageReqVO pageReqVO) {
        PageResult<ConceptTableDO> pageResult = conceptTableMapper.selectPage(pageReqVO);
        List<ConceptTableRespVO> list = ConceptTableConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<ConceptTableRespVO> getConceptTableByConceptId(Long conceptId) {
        List<ConceptTableDO> list = conceptTableMapper.selectByConceptId(conceptId);
        return ConceptTableConvert.INSTANCE.convertList(list);
    }

    @Override
    public ConceptTableRespVO getConceptTableById(Long id) {
        ConceptTableDO entity = conceptTableMapper.selectById(id);
        return entity != null ? ConceptTableConvert.INSTANCE.convert(entity) : null;
    }

    @Override
    public Long createConceptTable(ConceptTableSaveReqVO createReqVO) {
        ConceptTableDO entity = ConceptTableConvert.INSTANCE.convert(createReqVO);
        conceptTableMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Integer updateConceptTable(ConceptTableSaveReqVO updateReqVO) {
        ConceptTableDO entity = ConceptTableConvert.INSTANCE.convert(updateReqVO);
        return conceptTableMapper.updateById(entity);
    }

    @Override
    public Integer deleteConceptTable(Long id) {
        return conceptTableMapper.deleteById(id);
    }
}
