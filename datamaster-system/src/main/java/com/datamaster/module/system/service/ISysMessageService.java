

package com.datamaster.module.system.service;

import com.datamaster.module.system.api.message.dto.MessageSaveReqDTO;

public interface ISysMessageService {
    public Boolean send(Long templateId, MessageSaveReqDTO messageSaveReqDTO, Object entity);

    /**
     * 数据发现使用
     * @param receiverId
     * @param entity
     * @return
     */
    public Boolean sendDbChangeMessage( Long receiverId, Object entity);


}
