package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.RelationColumnDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 关系字段绑定 Mapper
 */
public interface RelationColumnMapper extends BaseMapperX<RelationColumnDO> {

    default List<RelationColumnDO> selectByRelationId(Long relationId) {
        return selectList(new LambdaQueryWrapperX<RelationColumnDO>()
                .eq(RelationColumnDO::getRelationId, relationId)
                .orderByAsc(RelationColumnDO::getId));
    }

    default int deleteByRelationId(Long relationId) {
        return delete(new LambdaQueryWrapperX<RelationColumnDO>()
                .eq(RelationColumnDO::getRelationId, relationId));
    }
}
