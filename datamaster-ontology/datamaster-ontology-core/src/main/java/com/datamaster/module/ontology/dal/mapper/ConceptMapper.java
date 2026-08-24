package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 本体概念 Mapper
 *
 * 对应表 ONT_CONCEPT
 */
public interface ConceptMapper extends BaseMapperX<ConceptDO> {

    default PageResult<ConceptDO> selectPage(ConceptPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ConceptDO>()
                .eq(reqVO.getOntologyId() != null, ConceptDO::getOntologyId, reqVO.getOntologyId())
                .like(StringUtils.isNotBlank(reqVO.getName()), ConceptDO::getName, reqVO.getName())
                .like(StringUtils.isNotBlank(reqVO.getCode()), ConceptDO::getCode, reqVO.getCode())
                .eq(reqVO.getStatus() != null, ConceptDO::getStatus, reqVO.getStatus())
                .orderByAsc(ConceptDO::getSortOrder)
                .orderByDesc(ConceptDO::getId));
    }
}
