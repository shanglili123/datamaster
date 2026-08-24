package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.OntologyConvert;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyPageReqVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import com.datamaster.module.ontology.service.IOntologyService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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
    public Integer deleteOntology(Long id) {
        return ontologyMapper.deleteById(id);
    }
}
