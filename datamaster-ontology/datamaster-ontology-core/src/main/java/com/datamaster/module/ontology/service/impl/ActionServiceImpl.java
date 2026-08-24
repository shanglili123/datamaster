package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.convert.ActionConvert;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.service.IActionService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

/**
 * 动作定义 Service 实现
 */
@Service
@Validated
public class ActionServiceImpl implements IActionService {

    @Resource
    private ActionMapper actionMapper;

    @Override
    public Long createAction(ActionSaveReqVO createReqVO) {
        ActionDO action = ActionConvert.INSTANCE.convert(createReqVO);
        actionMapper.insert(action);
        return action.getId();
    }

    @Override
    public Integer updateAction(ActionSaveReqVO updateReqVO) {
        ActionDO action = ActionConvert.INSTANCE.convert(updateReqVO);
        return actionMapper.updateById(action);
    }

    @Override
    public Integer deleteAction(Long id) {
        return actionMapper.deleteById(id);
    }

    @Override
    public ActionRespVO getActionById(Long id) {
        ActionDO action = actionMapper.selectById(id);
        return ActionConvert.INSTANCE.convert(action);
    }

    @Override
    public PageResult<ActionRespVO> getActionPage(ActionPageReqVO pageReqVO) {
        PageResult<ActionDO> pageResult = actionMapper.selectPage(pageReqVO);
        return new PageResult<>(ActionConvert.INSTANCE.convertList(pageResult.getRows()), pageResult.getTotal());
    }

    @Override
    public List<ActionRespVO> getActionsByOntologyId(Long ontologyId) {
        List<ActionDO> list = actionMapper.selectByOntologyId(ontologyId);
        return ActionConvert.INSTANCE.convertList(list);
    }
}
