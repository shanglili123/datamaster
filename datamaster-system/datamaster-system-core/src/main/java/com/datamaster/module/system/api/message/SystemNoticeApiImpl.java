package com.datamaster.module.system.api.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.enums.UserStatus;
import com.datamaster.module.system.api.message.dto.SystemNoticeReqDTO;
import com.datamaster.module.system.controller.admin.system.message.websocket.WebSocketMessageServer;
import com.datamaster.module.system.dal.dataobject.message.MessageDO;
import com.datamaster.module.system.dal.dataobject.message.enums.MessageHasReadEnums;
import com.datamaster.module.system.service.ISysUserService;
import com.datamaster.module.system.service.message.IMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import java.net.URI;

/**
 * 复用站内消息存储的内部系统通知适配器。
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class SystemNoticeApiImpl implements SystemNoticeApi {

    private static final long SYSTEM_ID = 0L;
    private static final String SYSTEM_NAME = "SYSTEM";
    // 现有字典：message_category 0=通知，message_level 1=一般；module 默认 0。
    private static final int NOTICE_CATEGORY = 0;
    private static final int NORMAL_LEVEL = 1;
    private static final int DEFAULT_MODULE = 0;

    @Resource
    private IMessageService messageService;
    @Resource
    private ISysUserService userService;

    @Override
    public Boolean sendSystemNotice(SystemNoticeReqDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("系统通知不能为空");
        }
        Long receiverId = request.getReceiverId();
        if (receiverId == null || receiverId <= 0) {
            throw new IllegalArgumentException("接收人不能为空且必须为有效用户 ID");
        }
        String safeTitle = plainText(request.getTitle(), "通知标题");
        String safeContent = plainText(request.getContent(), "通知内容");
        validateEntityUrl(request.getEntityUrl());
        SysUser receiver = userService.selectUserById(receiverId);
        if (receiver == null || !UserStatus.OK.getCode().equals(receiver.getDelFlag())
                || !UserStatus.OK.getCode().equals(receiver.getStatus())) {
            throw new IllegalArgumentException("通知接收人不存在、已删除或已停用");
        }

        MessageDO message = new MessageDO();
        message.setSenderId(SYSTEM_ID);
        message.setReceiverId(receiverId);
        message.setTitle(safeTitle);
        message.setContent(safeContent);
        message.setEntityId(request.getEntityId());
        message.setEntityUrl(request.getEntityUrl());
        message.setCategory(NOTICE_CATEGORY);
        message.setMsgLevel(NORMAL_LEVEL);
        message.setModule(DEFAULT_MODULE);
        message.setHasRead(MessageHasReadEnums.WD.code);
        message.setHasRetraction(0);
        // Existing PostgreSQL column is SMALLINT DEFAULT 1; leave it to its numeric default.
        message.setDelFlag(0);
        message.setCreatorId(SYSTEM_ID);
        message.setCreateBy(SYSTEM_NAME);
        message.setUpdaterId(SYSTEM_ID);
        message.setUpdateBy(SYSTEM_NAME);
        if (!messageService.save(message)) {
            throw new IllegalStateException("系统通知保存失败");
        }
        // 当前 MyMetaObjectHandler 会无条件覆盖登录态审计字段；用无实体更新恢复 SYSTEM，
        // 不触发实体自动填充，且与消息、调用方业务标记使用同一本地事务。
        LambdaUpdateWrapper<MessageDO> attribution = new LambdaUpdateWrapper<MessageDO>()
                .eq(MessageDO::getId, message.getId())
                .set(MessageDO::getCreatorId, SYSTEM_ID)
                .set(MessageDO::getCreateBy, SYSTEM_NAME)
                .set(MessageDO::getUpdaterId, SYSTEM_ID)
                .set(MessageDO::getUpdateBy, SYSTEM_NAME);
        if (!messageService.update(null, attribution)) {
            throw new IllegalStateException("系统通知归属保存失败");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshUnread(receiverId);
            }
        });
        return Boolean.TRUE;
    }

    private void refreshUnread(Long receiverId) {
        try {
            // getReceiverWDNum 的旧实现查询 delFlag=1 且按单一 userId 查 session。
            // 这里复用现有持久层统计未删除消息，并使用现有多会话刷新入口。
            long unread = messageService.count(new LambdaQueryWrapper<MessageDO>()
                    .eq(MessageDO::getReceiverId, receiverId)
                    .eq(MessageDO::getHasRead, MessageHasReadEnums.WD.code));
            WebSocketMessageServer.sendMessageToUser(receiverId.toString(), Long.toString(unread));
        } catch (Exception e) {
            log.warn("系统通知已提交，刷新接收人 {} 的未读消息失败", receiverId, e);
        }
    }

    private static String plainText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        // 不接受富文本；转义可变组件名等文本，避免任何 HTML 渲染入口执行标签。
        return HtmlUtils.htmlEscape(value);
    }

    private static void validateEntityUrl(String entityUrl) {
        if (entityUrl == null || entityUrl.isEmpty()) {
            return;
        }
        if (entityUrl.length() > 256 || !entityUrl.startsWith("/") || entityUrl.startsWith("//")
                || entityUrl.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("通知链接必须为不超过256个字符的站内路径");
        }
        try {
            URI uri = URI.create(entityUrl);
            if (uri.isAbsolute() || uri.getRawAuthority() != null) {
                throw new IllegalArgumentException("通知链接必须为站内路径");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("通知链接格式无效", e);
        }
    }
}
