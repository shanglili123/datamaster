

package com.datamaster.module.system.service.message.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.system.dal.dataobject.message.MessageTemplateDO;
import com.datamaster.module.system.dal.mapper.message.MessageTemplateMapper;
import com.datamaster.module.system.service.message.IMessageTemplateService;

import javax.annotation.Resource;

/**
 * 消息模板Service业务层处理
 *
 * @author DATAMASTER
 * @date 2024-10-31
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MessageTemplateServiceImpl  extends ServiceImpl<MessageTemplateMapper,MessageTemplateDO> implements IMessageTemplateService {
    @Resource
    private MessageTemplateMapper messageTemplateMapper;

}
