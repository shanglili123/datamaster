

package com.datamaster.module.system.service.message;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.module.system.controller.admin.system.message.vo.MessageTemplatePageReqVO;
import com.datamaster.module.system.convert.message.MessageTemplateConvert;
import com.datamaster.module.system.dal.dataobject.message.MessageTemplateDO;

import java.util.List;

/**
 * 消息模板Service接口
 *
 * @author DATAMASTER
 * @date 2024-10-31
 */
public interface IMessageTemplateService extends IService<MessageTemplateDO> {

    default PageResult<MessageTemplateDO> getMessageTemplatePage(MessageTemplatePageReqVO messageTemplate) {
        QueryWrapper<MessageTemplateDO> queryWrapper = new QueryWrapper<>(MessageTemplateConvert.INSTANCE.convertToDO(messageTemplate));
        if (PageParam.PAGE_SIZE_NONE.equals(messageTemplate.getPageSize())) {
            List<MessageTemplateDO> list = list(queryWrapper);
            return new PageResult<>(list, (long) list.size());
        }
        Page<MessageTemplateDO> page = page(new Page<>(messageTemplate.getPageNum(), messageTemplate.getPageSize()), queryWrapper);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }
}
