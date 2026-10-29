package com.datamaster.module.system.api.message;

import com.datamaster.module.system.api.message.dto.SystemNoticeReqDTO;

/** 内部站内通知；调用方负责空间授权和同事务发送标记，不提供HTTP入口。 */
public interface SystemNoticeApi {
    Boolean sendSystemNotice(SystemNoticeReqDTO request);
}
