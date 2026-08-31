package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.OntologyConvert;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyPageReqVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.dal.mapper.FunctionMapper;
import com.datamaster.module.ontology.dal.mapper.ActionExecutionMapper;
import com.datamaster.module.ontology.dal.mapper.FunctionExecutionMapper;
import com.datamaster.module.ontology.service.IOntologyService;
import com.datamaster.module.ontology.service.IConceptService;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.FunctionDO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.dataobject.FunctionExecutionDO;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 本体 Service 实现
 */
@Service
@Validated
public class OntologyServiceImpl implements IOntologyService {

    @Resource
    private OntologyMapper ontologyMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private ActionMapper actionMapper;
    @Resource private FunctionMapper functionMapper;
    @Resource private ActionExecutionMapper actionExecutionMapper;
    @Resource private FunctionExecutionMapper functionExecutionMapper;
    @Resource private IConceptService conceptService;

    @Override
    public PageResult<OntologyRespVO> getOntologyPage(OntologyPageReqVO pageReqVO) {
        PageResult<OntologyDO> pageResult = ontologyMapper.selectPage(pageReqVO);
        List<OntologyRespVO> list = OntologyConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public OntologyRespVO getOntologyById(Long id) {
        OntologyDO ontology = ontologyMapper.selectById(id);
        return ontology != null ? OntologyConvert.INSTANCE.convert(ontology) : null;
    }

    @Override
    public Long createOntology(OntologySaveReqVO createReqVO) {
        OntologyDO ontology = OntologyConvert.INSTANCE.convert(createReqVO);
        ontologyMapper.insert(ontology);
        return ontology.getId();
    }

    @Override
    public Integer updateOntology(OntologySaveReqVO updateReqVO) {
        OntologyDO ontology = OntologyConvert.INSTANCE.convert(updateReqVO);
        return ontologyMapper.updateById(ontology);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteOntology(Long id) {
        for (ConceptDO concept : conceptMapper.selectList(new LambdaQueryWrapperX<ConceptDO>()
                .eq(ConceptDO::getOntologyId, id))) {
            conceptService.deleteConcept(concept.getId());
        }
        actionExecutionMapper.delete(new LambdaQueryWrapperX<ActionExecutionDO>().eq(ActionExecutionDO::getOntologyId, id));
        functionExecutionMapper.delete(new LambdaQueryWrapperX<FunctionExecutionDO>().eq(FunctionExecutionDO::getOntologyId, id));
        actionMapper.delete(new LambdaQueryWrapperX<ActionDO>().eq(ActionDO::getOntologyId, id));
        functionMapper.delete(new LambdaQueryWrapperX<FunctionDO>().eq(FunctionDO::getOntologyId, id));
        return ontologyMapper.deleteById(id);
    }
}
