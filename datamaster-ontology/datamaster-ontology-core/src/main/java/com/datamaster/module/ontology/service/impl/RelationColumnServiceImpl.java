package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.convert.RelationColumnConvert;
import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnRespVO;
import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.RelationColumnDO;
import com.datamaster.module.ontology.dal.mapper.RelationColumnMapper;
import com.datamaster.module.ontology.service.IRelationColumnService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

/**
 * 关系字段绑定 Service 实现
 */
@Service
@Validated
public class RelationColumnServiceImpl implements IRelationColumnService {

    @Resource
    private RelationColumnMapper relationColumnMapper;

    @Override
    public List<RelationColumnRespVO> getRelationColumnsByRelationId(Long relationId) {
        List<RelationColumnDO> list = relationColumnMapper.selectByRelationId(relationId);
        return RelationColumnConvert.INSTANCE.convertList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveRelationColumns(Long relationId, List<RelationColumnSaveReqVO> list) {
        relationColumnMapper.deleteByRelationId(relationId);
        if (list != null && !list.isEmpty()) {
            for (RelationColumnSaveReqVO vo : list) {
                vo.setRelationId(relationId);
                RelationColumnDO entity = RelationColumnConvert.INSTANCE.convert(vo);
                relationColumnMapper.insert(entity);
            }
        }
    }

    @Override
    public Integer deleteRelationColumn(Long id) {
        return relationColumnMapper.deleteById(id);
    }
}
