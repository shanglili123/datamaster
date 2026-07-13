package com.datamaster.module.assets.service.skill;

import com.datamaster.module.assets.controller.admin.skill.vo.AiAskMessageRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskMessageSaveReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskMessageWindowRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskSessionRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskSessionSaveReqVO;

import java.util.List;

public interface IAiAskSessionService {

    List<AiAskSessionRespVO> listRecent(Long userId, Long projectId, Integer limit);

    AiAskSessionRespVO create(Long userId, String username, AiAskSessionSaveReqVO reqVO);

    AiAskSessionRespVO update(Long userId, String username, Long sessionId, AiAskSessionSaveReqVO reqVO);

    Integer delete(Long userId, Long projectId, Long sessionId);

    AiAskMessageWindowRespVO listMessages(Long userId, Long projectId, Long sessionId, Long beforeId, Long afterId, Integer limit);

    AiAskMessageRespVO appendMessage(Long userId, String username, Long projectId, Long sessionId, AiAskMessageSaveReqVO reqVO);

    Integer deleteMessage(Long userId, Long projectId, Long sessionId, Long messageId);

    Integer clearMessages(Long userId, Long projectId, Long sessionId);
}
