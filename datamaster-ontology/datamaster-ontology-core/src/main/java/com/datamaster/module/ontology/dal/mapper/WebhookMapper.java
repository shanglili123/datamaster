package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.WebhookDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.common.core.page.PageResult;

import java.util.List;

/**
 * Webhook 配置 Mapper
 */
public interface WebhookMapper extends BaseMapperX<WebhookDO> {

    default PageResult<WebhookDO> selectPage(WebhookPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WebhookDO>()
                .eqIfPresent(WebhookDO::getOntologyId, reqVO.getOntologyId())
                .eqIfPresent(WebhookDO::getActionId, reqVO.getActionId())
                .eqIfPresent(WebhookDO::getEnabled, reqVO.getEnabled())
                .likeIfPresent(WebhookDO::getName, reqVO.getName())
                .orderByDesc(WebhookDO::getId));
    }

    /** 查询绑定到某动作的启用回调（actionId 精确匹配） */
    default List<WebhookDO> selectEnabledByAction(Long actionId) {
        return selectList(new LambdaQueryWrapperX<WebhookDO>()
                .eq(WebhookDO::getActionId, actionId)
                .eq(WebhookDO::getEnabled, true)
                .orderByAsc(WebhookDO::getId));
    }

    /** 查询某本体下未绑定具体动作的所有启用回调（本体级兜底） */
    default List<WebhookDO> selectEnabledByOntologyScope(Long ontologyId) {
        return selectList(new LambdaQueryWrapperX<WebhookDO>()
                .eq(WebhookDO::getOntologyId, ontologyId)
                .isNull(WebhookDO::getActionId)
                .eq(WebhookDO::getEnabled, true)
                .orderByAsc(WebhookDO::getId));
    }
}