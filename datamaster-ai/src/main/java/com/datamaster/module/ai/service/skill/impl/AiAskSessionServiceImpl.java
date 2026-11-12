package com.datamaster.module.ai.service.skill.impl;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import java.util.Objects;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskMessageRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskMessageSaveReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskMessageWindowRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskSessionRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskSessionSaveReqVO;
import com.datamaster.module.ai.dal.dataobject.skill.AiAskMessageDO;
import com.datamaster.module.ai.dal.dataobject.skill.AiAskSessionDO;
import com.datamaster.module.ai.dal.mapper.skill.AiAskMessageMapper;
import com.datamaster.module.ai.dal.mapper.skill.AiAskSessionMapper;
import com.datamaster.module.ai.service.skill.IAiAskSessionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class AiAskSessionServiceImpl implements IAiAskSessionService {

    @Resource
    private AiAskSessionMapper sessionMapper;
    @Resource
    private AiAskMessageMapper aiAskMessageMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private ISpaceApi spaceApi;
    @Resource
    private OntologyMapper ontologyMapper;

    @Override
    public List<AiAskSessionRespVO> listRecent(Long userId, Long spaceId, Integer limit) {
        return listRecent(userId, spaceId, null, limit);
    }

    @Override
    public List<AiAskSessionRespVO> listRecent(Long userId, Long spaceId, String category, Integer limit) {
        requireAuthorizedSpace(userId, spaceId, null);
        ensureTablesReady();
        return BeanUtils.toBean(sessionMapper.selectRecent(userId, spaceId, category, limit), AiAskSessionRespVO.class);
    }

    @Override
    public AiAskSessionRespVO create(Long userId, String username, AiAskSessionSaveReqVO reqVO) {
        validateSelection(userId, reqVO);
        ensureTablesReady();
        AiAskSessionDO session = new AiAskSessionDO();
        session.setUserId(userId);
        applySessionFields(session, reqVO);
        session.setTitle(defaultText(reqVO == null ? null : reqVO.getTitle(), "新问数对话"));
        session.setMode(defaultText(reqVO == null ? null : reqVO.getMode(), "qa"));
        session.setCategory(defaultText(reqVO == null ? null : reqVO.getCategory(), "ASK_DATA"));
        session.setReturnSql(reqVO != null && Boolean.TRUE.equals(reqVO.getReturnSql()));
        session.setMessageCount(0);
        session.setDelFlag(Boolean.FALSE);
        session.setCreatorId(userId);
        session.setCreateBy(username);
        session.setCreateTime(new Date());
        session.setUpdatorId(userId);
        session.setUpdateBy(username);
        session.setUpdateTime(new Date());
        sessionMapper.insert(session);
        return BeanUtils.toBean(session, AiAskSessionRespVO.class);
    }

    @Override
    public AiAskSessionRespVO update(Long userId, String username, Long sessionId, AiAskSessionSaveReqVO reqVO) {
        validateSelection(userId, reqVO);
        ensureTablesReady();
        AiAskSessionDO old = requireSession(userId, reqVO.getSpaceId(), sessionId);
        Date now = new Date();
        sessionMapper.update(null, Wrappers.lambdaUpdate(AiAskSessionDO.class)
                // Explicit SET preserves null as a clear operation (not MyBatis non-null merge).
                .set(AiAskSessionDO::getOntologyId, reqVO.getOntologyId())
                .set(AiAskSessionDO::getSpaceId, old.getSpaceId())
                .set(AiAskSessionDO::getSpaceCode, reqVO.getSpaceCode())
                .set(AiAskSessionDO::getTitle, defaultText(reqVO == null ? null : reqVO.getTitle(), old.getTitle()))
                .set(AiAskSessionDO::getMode, defaultText(reqVO == null ? null : reqVO.getMode(), old.getMode()))
                .set(AiAskSessionDO::getCategory, defaultText(reqVO == null ? null : reqVO.getCategory(), old.getCategory()))
                .set(AiAskSessionDO::getDatasourceId, reqVO == null ? old.getDatasourceId() : reqVO.getDatasourceId())
                .set(AiAskSessionDO::getDatasourceName, reqVO == null ? old.getDatasourceName() : reqVO.getDatasourceName())
                .set(AiAskSessionDO::getSkillId, reqVO == null ? old.getSkillId() : reqVO.getSkillId())
                .set(AiAskSessionDO::getTemplateId, reqVO == null ? old.getTemplateId() : reqVO.getTemplateId())
                .set(AiAskSessionDO::getReturnSql, reqVO != null && Boolean.TRUE.equals(reqVO.getReturnSql()))
                .set(AiAskSessionDO::getUpdateBy, username)
                .set(AiAskSessionDO::getUpdateTime, now)
                .eq(AiAskSessionDO::getId, sessionId)
                .eq(AiAskSessionDO::getSpaceId, old.getSpaceId())
                .eq(AiAskSessionDO::getUserId, userId));
        return BeanUtils.toBean(sessionMapper.selectById(sessionId), AiAskSessionRespVO.class);
    }

    @Override
    public Integer delete(Long userId, Long spaceId, Long sessionId) {
        ensureTablesReady();
        requireSession(userId, spaceId, sessionId);
        aiAskMessageMapper.deleteBySessionId(sessionId);
        return sessionMapper.deleteById(sessionId);
    }

    @Override
    public AiAskMessageWindowRespVO listMessages(Long userId, Long spaceId, Long sessionId,
                                                  Long beforeId, Long afterId, Integer limit) {
        ensureTablesReady();
        requireSession(userId, spaceId, sessionId);
        List<AiAskMessageDO> rows;
        if (beforeId != null) {
            rows = aiAskMessageMapper.selectBefore(sessionId, beforeId, limit == null ? 5 : limit);
        } else if (afterId != null) {
            rows = aiAskMessageMapper.selectAfter(sessionId, afterId, limit == null ? 5 : limit);
        } else {
            rows = aiAskMessageMapper.selectLatest(sessionId, limit == null ? 10 : limit);
        }
        AiAskMessageWindowRespVO respVO = new AiAskMessageWindowRespVO();
        respVO.setRows(BeanUtils.toBean(rows, AiAskMessageRespVO.class));
        respVO.setHasBefore(!rows.isEmpty() && aiAskMessageMapper.selectBefore(sessionId, rows.get(0).getId(), 1).size() > 0);
        respVO.setHasAfter(!rows.isEmpty() && aiAskMessageMapper.selectAfter(sessionId, rows.get(rows.size() - 1).getId(), 1).size() > 0);
        return respVO;
    }

    @Override
    public AiAskMessageRespVO appendMessage(Long userId, String username, Long spaceId, Long sessionId, AiAskMessageSaveReqVO reqVO) {
        ensureTablesReady();
        AiAskSessionDO session = requireSession(userId, spaceId, sessionId);
        AiAskMessageDO message = new AiAskMessageDO();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setSpaceId(session.getSpaceId());
        message.setRole(reqVO.getRole());
        message.setContent(reqVO.getContent());
        message.setDisplayContent(reqVO.getDisplayContent());
        message.setPayloadJson(reqVO.getPayloadJson());
        message.setDelFlag(Boolean.FALSE);
        message.setCreatorId(userId);
        message.setCreateBy(username);
        message.setCreateTime(new Date());
        message.setUpdatorId(userId);
        message.setUpdateBy(username);
        message.setUpdateTime(new Date());
        aiAskMessageMapper.insert(message);
        touchSession(userId, username, sessionId, session.getTitle(), session.getMessageCount() == null ? 1 : session.getMessageCount() + 1);
        return BeanUtils.toBean(message, AiAskMessageRespVO.class);
    }

    @Override
    public Integer deleteMessage(Long userId, Long spaceId, Long sessionId, Long messageId) {
        ensureTablesReady();
        requireSession(userId, spaceId, sessionId);
        AiAskMessageDO message = aiAskMessageMapper.selectById(messageId);
        if (message == null || !sessionId.equals(message.getSessionId())) {
            throw new ServiceException("聊天记录不存在");
        }
        Integer rows = aiAskMessageMapper.deleteById(messageId);
        refreshMessageCount(userId, sessionId);
        return rows;
    }

    @Override
    public Integer clearMessages(Long userId, Long spaceId, Long sessionId) {
        ensureTablesReady();
        requireSession(userId, spaceId, sessionId);
        Integer rows = aiAskMessageMapper.deleteBySessionId(sessionId);
        refreshMessageCount(userId, sessionId);
        return rows;
    }

    private void applySessionFields(AiAskSessionDO session, AiAskSessionSaveReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        session.setOntologyId(reqVO.getOntologyId());
        session.setSpaceId(reqVO.getSpaceId());
        session.setSpaceCode(reqVO.getSpaceCode());
        session.setTitle(reqVO.getTitle());
        session.setMode(reqVO.getMode());
        session.setCategory(reqVO.getCategory());
        session.setDatasourceId(reqVO.getDatasourceId());
        session.setDatasourceName(reqVO.getDatasourceName());
        session.setSkillId(reqVO.getSkillId());
        session.setTemplateId(reqVO.getTemplateId());
        session.setReturnSql(reqVO.getReturnSql());
    }

    private void validateSelection(Long userId, AiAskSessionSaveReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("会话保存请求不能为空");
        }
        String canonicalCode = requireAuthorizedSpace(userId, reqVO.getSpaceId(), reqVO.getSpaceCode());
        reqVO.setSpaceCode(canonicalCode);
        if (reqVO.getOntologyId() == null) {
            return;
        }
        OntologyDO ontology = ontologyMapper.selectById(reqVO.getOntologyId());
        if (ontology == null || ontology.getDelFlag() != null && ontology.getDelFlag() != 0
                || !Objects.equals(reqVO.getSpaceId(), ontology.getSpaceId())
                || StringUtils.isNotBlank(ontology.getSpaceCode()) && !canonicalCode.equals(ontology.getSpaceCode())) {
            throw new ServiceException("本体不属于当前可访问空间");
        }
    }

    private String requireAuthorizedSpace(Long userId, Long spaceId, String spaceCode) {
        if (userId == null || spaceId == null || spaceId <= 0) {
            throw new ServiceException("请先选择当前空间");
        }
        String canonicalCode = spaceApi.getSpaceCodeBySpaceId(spaceId);
        if (StringUtils.isBlank(canonicalCode)
                || StringUtils.isNotBlank(spaceCode) && !canonicalCode.equals(spaceCode)) {
            throw new ServiceException("空间标识不一致");
        }
        if (!spaceApi.hasUserSpace(userId, spaceId, canonicalCode)) {
            throw new ServiceException("当前用户无权访问该空间");
        }
        return canonicalCode;
    }

    private AiAskSessionDO requireSession(Long userId, Long spaceId, Long sessionId) {
        if (sessionId == null) {
            throw new ServiceException("会话ID不能为空");
        }
        AiAskSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || Boolean.TRUE.equals(session.getDelFlag()) || !Objects.equals(userId, session.getUserId())) {
            throw new ServiceException("会话不存在");
        }
        if (spaceId != null && !spaceId.equals(session.getSpaceId())) {
            throw new ServiceException("会话不属于当前空间");
        }
        requireAuthorizedSpace(userId, session.getSpaceId(), session.getSpaceCode());
        return session;
    }

    private void touchSession(Long userId, String username, Long sessionId, String title, Integer messageCount) {
        sessionMapper.update(null, Wrappers.lambdaUpdate(AiAskSessionDO.class)
                .set(AiAskSessionDO::getTitle, title)
                .set(AiAskSessionDO::getMessageCount, messageCount)
                .set(AiAskSessionDO::getUpdateBy, username)
                .set(AiAskSessionDO::getUpdateTime, new Date())
                .eq(AiAskSessionDO::getId, sessionId)
                .eq(AiAskSessionDO::getUserId, userId));
    }

    private void refreshMessageCount(Long userId, Long sessionId) {
        Long count = aiAskMessageMapper.countBySessionId(sessionId);
        sessionMapper.update(null, Wrappers.lambdaUpdate(AiAskSessionDO.class)
                .set(AiAskSessionDO::getMessageCount, count == null ? 0 : count.intValue())
                .eq(AiAskSessionDO::getId, sessionId)
                .eq(AiAskSessionDO::getUserId, userId));
    }

    private void ensureTablesReady() {
        if (!tableExists("ai_ask_session")) {
            initSessionTable();
        }
        // Existing tables are upgraded only by reviewed manual migrations.
        if (!tableExists("ai_ask_message")) {
            initMessageTable();
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND lower(table_name) = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private void initSessionTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS AI_ASK_SESSION ("
                + "ID BIGINT PRIMARY KEY, USER_ID BIGINT NOT NULL, SPACE_ID BIGINT, SPACE_CODE VARCHAR(128), ONTOLOGY_ID BIGINT,"
                + "TITLE VARCHAR(255) NOT NULL, MODE VARCHAR(32) DEFAULT 'qa', CATEGORY VARCHAR(32) DEFAULT 'ASK_DATA', DATASOURCE_ID BIGINT,"
                + "DATASOURCE_NAME VARCHAR(255), SKILL_ID BIGINT, TEMPLATE_ID BIGINT, RETURN_SQL BOOLEAN DEFAULT FALSE,"
                + "MESSAGE_COUNT INTEGER DEFAULT 0, CREATOR_ID BIGINT, CREATE_BY VARCHAR(64), CREATE_TIME TIMESTAMP,"
                + "UPDATER_ID BIGINT, UPDATE_BY VARCHAR(64), UPDATE_TIME TIMESTAMP, REMARK VARCHAR(500),"
                + "DEL_FLAG BOOLEAN DEFAULT FALSE)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_AI_ASK_SESSION_SCOPE "
                + "ON AI_ASK_SESSION (USER_ID, SPACE_ID, UPDATE_TIME DESC)");
    }

    private void initMessageTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS AI_ASK_MESSAGE ("
                + "ID BIGINT PRIMARY KEY, SESSION_ID BIGINT NOT NULL, USER_ID BIGINT NOT NULL, SPACE_ID BIGINT,"
                + "ROLE VARCHAR(32) NOT NULL, CONTENT TEXT, DISPLAY_CONTENT TEXT, PAYLOAD_JSON TEXT,"
                + "CREATOR_ID BIGINT, CREATE_BY VARCHAR(64), CREATE_TIME TIMESTAMP,"
                + "UPDATER_ID BIGINT, UPDATE_BY VARCHAR(64), UPDATE_TIME TIMESTAMP, REMARK VARCHAR(500),"
                + "DEL_FLAG BOOLEAN DEFAULT FALSE)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_AI_ASK_MESSAGE_SESSION "
                + "ON AI_ASK_MESSAGE (SESSION_ID, ID)");
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }
}

