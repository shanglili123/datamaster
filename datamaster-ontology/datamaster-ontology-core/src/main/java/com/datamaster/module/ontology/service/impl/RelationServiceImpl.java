package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.RelationConvert;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationPageReqVO;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationRespVO;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import com.datamaster.module.ontology.dal.mapper.RelationColumnMapper;
import com.datamaster.module.ontology.dal.mapper.RelationTableMapper;
import com.datamaster.module.ontology.service.IRelationService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 本体关系 Service 实现
 */
@Service
@Validated
public class RelationServiceImpl implements IRelationService {

    @Resource
    private RelationMapper relationMapper;
    @Resource
    private RelationColumnMapper relationColumnMapper;
    @Resource
    private RelationTableMapper relationTableMapper;

    @Override
    public PageResult<RelationRespVO> getRelationPage(RelationPageReqVO pageReqVO) {
        PageResult<RelationDO> pageResult = relationMapper.selectPage(pageReqVO);
        List<RelationRespVO> list = RelationConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public RelationRespVO getRelationById(Long id) {
        RelationDO relation = relationMapper.selectById(id);
        return relation != null ? RelationConvert.INSTANCE.convert(relation) : null;
    }

    @Override
    public Long createRelation(RelationSaveReqVO createReqVO) {
        RelationDO relation = RelationConvert.INSTANCE.convert(createReqVO);
        relationMapper.insert(relation);
        return relation.getId();
    }

    @Override
    public Integer updateRelation(RelationSaveReqVO updateReqVO) {
        RelationDO relation = RelationConvert.INSTANCE.convert(updateReqVO);
        return relationMapper.updateById(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteRelation(Long id) {
        relationColumnMapper.deleteByRelationId(id);
        relationTableMapper.delete(new com.datamaster.mybatis.core.query.LambdaQueryWrapperX<com.datamaster.module.ontology.dal.dataobject.RelationTableDO>()
                .eq(com.datamaster.module.ontology.dal.dataobject.RelationTableDO::getRelationId, id));
        return relationMapper.deleteById(id);
    }
}
