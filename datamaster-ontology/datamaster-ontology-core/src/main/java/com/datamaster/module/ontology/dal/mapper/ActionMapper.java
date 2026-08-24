package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 动作定义 Mapper
 */
public interface ActionMapper extends BaseMapperX<ActionDO> {

    default PageResult<ActionDO> selectPage(ActionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ActionDO>()
                .eq(reqVO.getOntologyId() != null, ActionDO::getOntologyId, reqVO.getOntologyId())
                .eq(reqVO.getActionType() != null, ActionDO::getActionType, reqVO.getActionType())
                .like(reqVO.getName() != null, ActionDO::getName, reqVO.getName())
                .orderByDesc(ActionDO::getId));
    }

    default List<ActionDO> selectByOntologyId(Long ontologyId) {
        return selectList(new LambdaQueryWrapperX<ActionDO>()
                .eq(ActionDO::getOntologyId, ontologyId)
                .orderByAsc(ActionDO::getId));
    }
}
