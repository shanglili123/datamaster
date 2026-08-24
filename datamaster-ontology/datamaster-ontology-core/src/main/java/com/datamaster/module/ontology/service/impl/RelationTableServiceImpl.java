package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.RelationTableConvert;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTablePageReqVO;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTableRespVO;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTableSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.RelationTableDO;
import com.datamaster.module.ontology.dal.mapper.RelationTableMapper;
import com.datamaster.module.ontology.service.IRelationTableService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

/**
 * 关系关联表绑定 Service 实现
 */
@Service
@Validated
public class RelationTableServiceImpl implements IRelationTableService {

    @Resource
    private RelationTableMapper relationTableMapper;

    @Override
    public PageResult<RelationTableRespVO> getRelationTablePage(RelationTablePageReqVO pageReqVO) {
        PageResult<RelationTableDO> pageResult = relationTableMapper.selectPage(pageReqVO);
        List<RelationTableRespVO> list = RelationTableConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<RelationTableRespVO> getRelationTablesByRelationId(Long relationId) {
        List<RelationTableDO> list = relationTableMapper.selectByRelationId(relationId);
        return RelationTableConvert.INSTANCE.convertList(list);
    }

    @Override
    public RelationTableRespVO getRelationTableById(Long id) {
        RelationTableDO entity = relationTableMapper.selectById(id);
        return entity != null ? RelationTableConvert.INSTANCE.convert(entity) : null;
    }

    @Override
    public Long createRelationTable(RelationTableSaveReqVO createReqVO) {
        // 同一关系下同一张表只允许绑定一次
        Long exist = relationTableMapper.selectByRelationId(createReqVO.getRelationId()).stream()
                .filter(t -> t.getTableName().equals(createReqVO.getTableName()))
                .map(RelationTableDO::getId)
                .findFirst().orElse(null);
        if (exist != null) {
            throw new IllegalArgumentException("该表已绑定到此关系");
        }
        RelationTableDO entity = RelationTableConvert.INSTANCE.convert(createReqVO);
        relationTableMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Integer updateRelationTable(RelationTableSaveReqVO updateReqVO) {
        RelationTableDO entity = RelationTableConvert.INSTANCE.convert(updateReqVO);
        return relationTableMapper.updateById(entity);
    }

    @Override
    public Integer deleteRelationTable(Long id) {
        return relationTableMapper.deleteById(id);
    }
}
