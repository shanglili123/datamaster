package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTablePageReqVO;
import com.datamaster.module.ontology.dal.dataobject.RelationTableDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 关系关联表绑定 Mapper
 */
public interface RelationTableMapper extends BaseMapperX<RelationTableDO> {

    default PageResult<RelationTableDO> selectPage(RelationTablePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RelationTableDO>()
                .eq(reqVO.getRelationId() != null, RelationTableDO::getRelationId, reqVO.getRelationId())
                .orderByDesc(RelationTableDO::getId));
    }

    default List<RelationTableDO> selectByRelationId(Long relationId) {
        return selectList(new LambdaQueryWrapperX<RelationTableDO>()
                .eq(RelationTableDO::getRelationId, relationId)
                .orderByAsc(RelationTableDO::getId));
    }
}
