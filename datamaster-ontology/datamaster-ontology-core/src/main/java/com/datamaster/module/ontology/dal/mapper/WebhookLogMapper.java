package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookLogPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.WebhookLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.common.core.page.PageResult;

/**
 * Webhook 回调日志 Mapper
 */
public interface WebhookLogMapper extends BaseMapperX<WebhookLogDO> {

    default PageResult<WebhookLogDO> selectPage(WebhookLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WebhookLogDO>()
                .eqIfPresent(WebhookLogDO::getWebhookId, reqVO.getWebhookId())
                .eqIfPresent(WebhookLogDO::getExecutionType, reqVO.getExecutionType())
                .eqIfPresent(WebhookLogDO::getActionExecutionId, reqVO.getActionExecutionId())
                .eqIfPresent(WebhookLogDO::getStatus, reqVO.getStatus())
                .orderByDesc(WebhookLogDO::getId));
    }
}