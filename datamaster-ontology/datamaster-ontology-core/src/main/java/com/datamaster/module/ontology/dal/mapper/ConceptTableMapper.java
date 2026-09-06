package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTablePageReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 概念表绑定 Mapper
 */
public interface ConceptTableMapper extends BaseMapperX<ConceptTableDO> {

    default PageResult<ConceptTableDO> selectPage(ConceptTablePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ConceptTableDO>()
                .eq(reqVO.getConceptId() != null, ConceptTableDO::getConceptId, reqVO.getConceptId())
                .orderByDesc(ConceptTableDO::getId));
    }

    default List<ConceptTableDO> selectByConceptId(Long conceptId) {
        return selectList(new LambdaQueryWrapperX<ConceptTableDO>()
                .eq(ConceptTableDO::getConceptId, conceptId)
                .orderByAsc(ConceptTableDO::getId));
    }

    default List<ConceptTableDO> selectByDatasourceId(Long datasourceId) {
        return selectList(new LambdaQueryWrapperX<ConceptTableDO>()
                .eq(ConceptTableDO::getDatasourceId, datasourceId)
                .orderByAsc(ConceptTableDO::getId));
    }
}
