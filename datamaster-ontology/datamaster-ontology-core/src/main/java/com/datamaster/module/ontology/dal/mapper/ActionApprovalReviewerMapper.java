package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.ActionApprovalReviewerDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 审阅人决策审计 Mapper
 */
public interface ActionApprovalReviewerMapper extends BaseMapperX<ActionApprovalReviewerDO> {

    /** 查询某关卡的全部审阅决策（永久审计，只追加） */
    default List<ActionApprovalReviewerDO> selectByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<ActionApprovalReviewerDO>()
                .eq(ActionApprovalReviewerDO::getTaskId, taskId)
                .orderByAsc(ActionApprovalReviewerDO::getId));
    }

    /** 是否已有同审阅人对此关卡作过决策（防止重复审批） */
    default boolean existsReviewerDecision(Long taskId, Long reviewerId) {
        return selectCount(new LambdaQueryWrapperX<ActionApprovalReviewerDO>()
                .eq(ActionApprovalReviewerDO::getTaskId, taskId)
                .eq(ActionApprovalReviewerDO::getReviewerId, reviewerId)) > 0;
    }
}