package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;
import java.util.Locale;

/**
 * 动作定义 Mapper
 */
public interface ActionMapper extends BaseMapperX<ActionDO> {

    default PageResult<ActionDO> selectPage(ActionPageReqVO reqVO) {
        if (reqVO == null) {
            reqVO = new ActionPageReqVO();
        }
        Long id = reqVO.getId();
        Long ontologyId = reqVO.getOntologyId();
        String actionType = reqVO.getActionType();
        String name = reqVO.getName();
        String normalizedName = name == null ? null : name.trim();
        return selectPage(reqVO, new LambdaQueryWrapperX<ActionDO>()
                .eq(id != null, ActionDO::getId, id)
                .eq(ontologyId != null, ActionDO::getOntologyId, ontologyId)
                .eq(actionType != null, ActionDO::getActionType, actionType)
                .apply(normalizedName != null && !normalizedName.isEmpty(),
                        "LOWER(name) LIKE LOWER({0})", normalizedName == null ? null
                                : "%" + normalizedName.toLowerCase(Locale.ROOT) + "%")
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
