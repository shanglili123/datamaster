package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
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
    @Resource private MetricReferenceGuard metricReferenceGuard;
    @Resource private ISpaceApi spaceApi;

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
        if (createReqVO == null || createReqVO.getId() != null) throw new ServiceException("新建本体不得指定已有ID");
        OntologyDO ontology = OntologyConvert.INSTANCE.convert(createReqVO);
        normalizeSpace(ontology);
        ontologyMapper.insert(ontology);
        return ontology.getId();
    }

    @Override
    public Integer updateOntology(OntologySaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null) throw new ServiceException("本体ID不能为空");
        OntologyDO existing = ontologyMapper.selectById(updateReqVO.getId());
        if (existing == null) throw new ServiceException("本体不存在");
        // Published metrics derive their policy from this ownership; ordinary edits must not migrate it.
        normalizeSpace(existing);
        if ((updateReqVO.getSpaceId() != null && !existing.getSpaceId().equals(updateReqVO.getSpaceId()))
                || (updateReqVO.getSpaceCode() != null && !updateReqVO.getSpaceCode().isBlank()
                && !existing.getSpaceCode().equals(updateReqVO.getSpaceCode()))) {
            throw new ServiceException("普通编辑不能更改本体所属空间；跨空间迁移须单独审核并重新授权指标");
        }
        OntologyDO ontology = OntologyConvert.INSTANCE.convert(updateReqVO);
        ontology.setSpaceId(existing.getSpaceId());
        ontology.setSpaceCode(existing.getSpaceCode());
        return ontologyMapper.updateById(ontology);
    }

    private void normalizeSpace(OntologyDO ontology) {
        Long id = ontology.getSpaceId();
        String code = ontology.getSpaceCode();
        if (id == null && code != null && !code.isBlank()) id = spaceApi.getSpaceIdBySpaceCode(code);
        if (id == null || id <= 0) throw new ServiceException("本体必须明确所属空间，历史缺失归属需先维护");
        String actualCode = spaceApi.getSpaceCodeBySpaceId(id);
        if (actualCode == null || actualCode.isBlank()
                || (code != null && !code.isBlank() && !actualCode.equals(code))) {
            throw new ServiceException("本体空间标识不一致或空间不存在");
        }
        if (!spaceApi.hasUserSpace(SecurityUtils.getUserId(), id, actualCode)) throw new ServiceException("无权维护此空间本体");
        ontology.setSpaceId(id); ontology.setSpaceCode(actualCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteOntology(Long id) {
        metricReferenceGuard.assertOntologyDeletable(id);
        for (ConceptDO concept : conceptMapper.selectList(new LambdaQueryWrapperX<ConceptDO>()
                .eq(ConceptDO::getOntologyId, id))) {
            conceptService.deleteConcept(concept.getId());
        }
        actionExecutionMapper.delete(new LambdaQueryWrapperX<ActionExecutionDO>().eq(ActionExecutionDO::getOntologyId, id));
        // 函数执行表的实际数据库结构通过 function_id 关联函数定义，不包含 ontology_id。
        List<Long> functionIds = functionMapper.selectByOntologyId(id).stream()
                .map(FunctionDO::getId)
                .collect(java.util.stream.Collectors.toList());
        functionExecutionMapper.deleteByFunctionIds(functionIds);
        actionMapper.delete(new LambdaQueryWrapperX<ActionDO>().eq(ActionDO::getOntologyId, id));
        functionMapper.delete(new LambdaQueryWrapperX<FunctionDO>().eq(FunctionDO::getOntologyId, id));
        return ontologyMapper.deleteById(id);
    }
}
