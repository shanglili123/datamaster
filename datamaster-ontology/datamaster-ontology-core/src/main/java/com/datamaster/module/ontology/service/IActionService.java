package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.*;

import java.util.List;

/**
 * 动作定义 Service
 */
public interface IActionService {

    /** 创建动作 */
    Long createAction(ActionSaveReqVO createReqVO);

    /** 更新动作 */
    Integer updateAction(ActionSaveReqVO updateReqVO);

    /** 删除动作 */
    Integer deleteAction(Long id);

    /** 获取动作详情 */
    ActionRespVO getActionById(Long id);

    /** 分页查询动作 */
    PageResult<ActionRespVO> getActionPage(ActionPageReqVO pageReqVO);

    /** 按本体ID查询动作列表 */
    List<ActionRespVO> getActionsByOntologyId(Long ontologyId);
}
