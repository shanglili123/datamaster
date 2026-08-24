package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 本体 Mapper
 *
 * 对应表 ONT_ONTOLOGY
 */
public interface OntologyMapper extends BaseMapperX<OntologyDO> {

    default PageResult<OntologyDO> selectPage(OntologyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OntologyDO>()
                .like(StringUtils.isNotBlank(reqVO.getName()), OntologyDO::getName, reqVO.getName())
                .like(StringUtils.isNotBlank(reqVO.getCode()), OntologyDO::getCode, reqVO.getCode())
                .eq(reqVO.getStatus() != null, OntologyDO::getStatus, reqVO.getStatus())
                .orderByDesc(OntologyDO::getId));
    }
}
