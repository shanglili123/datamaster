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

    /** 数据到达触发匹配：命中 triggerRef 的已启用动作（triggerRef 非空即视为启用） */
    default List<ActionDO> selectByTriggerRef(String triggerRef) {
        if (triggerRef == null || triggerRef.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return selectList(new LambdaQueryWrapperX<ActionDO>()
                .eq(ActionDO::getTriggerRef, triggerRef));
    }
}
