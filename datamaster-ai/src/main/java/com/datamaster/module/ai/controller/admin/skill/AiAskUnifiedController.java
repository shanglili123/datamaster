package com.datamaster.module.ai.controller.admin.skill;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ai.agent.runtime.*;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.controller.admin.skill.vo.*;
import com.datamaster.module.ai.controller.agent.tool.AgentToolResourceGuard;
import com.datamaster.module.ai.dal.dataobject.skill.AiAskSessionDO;
import com.datamaster.module.ai.dal.mapper.skill.AiAskSessionMapper;
import com.datamaster.module.ai.service.skill.IAiAskSessionService;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Unified ask-data entry point. Management stays on the separate Data Agent page. */
@RestController
@RequestMapping("/ai/ask-data")
public class AiAskUnifiedController {
    @Resource private ISpaceApi spaceApi;
    @Resource private IAiAskSessionService sessions;
    @Resource private AiAskSessionMapper sessionMapper;
    @Resource private OntologyMapper ontologyMapper;
    @Resource private AgentToolResourceGuard resourceGuard;
    @Resource private AskMetricOperationStore operations;
    @Resource private DecisionAgentService decisions;
    @Resource private UnifiedAskAgentService unifiedAsk;
    @Resource private com.datamaster.module.ai.service.skill.AiAskOntologyContextService ontologyContexts;
    private final ExecutorService executor = new ThreadPoolExecutor(2, 8, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(64), new ThreadPoolExecutor.AbortPolicy());
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    @Operation(summary = "统一问数：正式指标或探索明细，不执行平台管理")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AskUnifiedReqVO request) {
        // Every authorization below runs synchronously with the actual authenticated request.
        LoginUser user = SecurityUtils.getLoginUser();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (user == null || user.getUserId() == null || authentication == null || !authentication.isAuthenticated())
            throw new ServiceException("请先登录");
        normalizeSpace(request, user.getUserId());
        validateSession(request, user.getUserId());
        if (request.getSessionId() != null) {
            String conversation = String.valueOf(request.getSessionId());
            if (StringUtils.isNotBlank(request.getConversationId()) && !conversation.equals(request.getConversationId()))
                throw new ServiceException("会话标识不一致");
            request.setConversationId(conversation);
        } else if (StringUtils.isBlank(request.getConversationId())) {
            request.setConversationId(UUID.randomUUID().toString());
        }
        if (request.getConversationId().length() > 64 || !request.getConversationId().matches("[A-Za-z0-9_-]+"))
            throw new ServiceException("会话标识无效");
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("ontologyId", request.getOntologyId()); attributes.put("sessionId", request.getSessionId());
        attributes.put("conversationId", request.getConversationId()); attributes.put("confirmed", false);
        attributes.put("entrypoint", "ASK_DATA");
        AgentToolContext context = new AgentToolContext(null, user.getUserId(), user.getUsername(),
                request.getSpaceId(), request.getSpaceCode(), attributes);
        validateOntology(request);
        // Validate any supplied selection before dispatch, but don't require an exploratory
        // source for formal metrics/decisions, which resolve their own approved sources.
        if (request.getOntologyId() != null && request.getDatasourceId() != null)
            ontologyContexts.resolveDatasource(request.getOntologyId(), request.getDatasourceId(),
                    request.getSpaceId(), request.getSpaceCode(), user.getUserId(), false);
        if (request.getDatasourceId() != null && !resourceGuard.datasourceVisible(request.getDatasourceId(), context))
            throw new ServiceException("数据源不属于当前可访问空间", 403);
        if (Boolean.TRUE.equals(request.getConfirmed()) || StringUtils.isNotBlank(request.getOperationId())) {
            return immediate(request, AgentToolResult.NEED_INPUT,
                    "请在原提案中确认动作；个人分析条件可在指标分析页面保存");
        }
        if (StringUtils.isNotBlank(request.getAnalysisOperationId())) {
            if (!unifiedAsk.isUnifiedReference(request.getAnalysisOperationId())) operations.restoreDecisionGoal(request, user.getUserId());
            else if (StringUtils.isNotBlank(request.getContinuationOperationId())) throw new ServiceException("分析引用不能与续接同时使用");
            request.setResponseMode("decision");
        }
        if (StringUtils.isNotBlank(request.getContinuationOperationId())) {
            if (unifiedAsk.isUnifiedReference(request.getContinuationOperationId())) {
                unifiedAsk.restoreInput(request, context);
            } else if (request.getContinuationOperationId().startsWith("ASK_DECISION_")) {
                decisions.restoreInput(request, context);
                request.setResponseMode("decision");
            } else {
                operations.restoreInput(request, user.getUserId());
            }
        }
        return start(request, context, authentication);
    }

    private SseEmitter start(AskUnifiedReqVO request, AgentToolContext context, Authentication authentication) {
        SseEmitter emitter = new SseEmitter(300000L);
        AtomicBoolean cancelled = new AtomicBoolean();
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> { cancelled.set(true); emitter.complete(); });
        emitter.onError(error -> cancelled.set(true));
        String key = context.getUserId() + ":" + context.getSpaceId() + ":" + request.getConversationId();
        if (!inFlight.add(key)) {
            send(emitter, cancelled, "result", UnifiedAskAgentService.response(request, null, AgentToolResult.NEED_INPUT, "当前会话已有请求执行中，请等待完成；不会自动取消或重开SQL"));
            emitter.complete(); return emitter;
        }
        try {
            executor.execute(() -> {
                SecurityContext previous = SecurityContextHolder.getContext();
                SecurityContext captured = SecurityContextHolder.createEmptyContext();
                captured.setAuthentication(authentication);
                SecurityContextHolder.setContext(captured);
                try {
                    if (cancelled.get()) return;
                    AgentRunResponse response = unifiedAsk.run(request, context, cancelled,
                            step -> send(emitter, cancelled, "steps", step),
                            report -> send(emitter, cancelled, "report", report),
                            chunk -> send(emitter, cancelled, "answer_delta", com.alibaba.fastjson2.JSON.toJSONString(chunk)));
                    // A disconnected/timed-out request must not publish a late result.
                    if (cancelled.get()) return;
                    send(emitter, cancelled, "result", response);
                    send(emitter, cancelled, "done", "[DONE]");
                    emitter.complete();
                } catch (Exception ex) {
                    if (!cancelled.get()) {
                        send(emitter, cancelled, "result", UnifiedAskAgentService.response(request, request.getOperationId(), AgentToolResult.FAILED, "问数未完成，请检查权限、配置和查询条件后重试"));
                        send(emitter, cancelled, "error", "问数未完成，请稍后重试");
                        emitter.complete();
                    }
                } finally {
                    AgentToolContextHolder.clear();
                    SecurityContextHolder.clearContext();
                    if (previous.getAuthentication() != null) SecurityContextHolder.setContext(previous);
                    inFlight.remove(key);
                }
            });
        } catch (RejectedExecutionException ex) {
            inFlight.remove(key);
            send(emitter, cancelled, "result", UnifiedAskAgentService.response(request, null, AgentToolResult.FAILED, "问数服务繁忙，请稍后重试"));
            emitter.complete();
        }
        return emitter;
    }

    private void normalizeSpace(AskUnifiedReqVO request, Long userId) {
        Long id = request.getSpaceId();
        String code = request.getSpaceCode();
        if (id == null && StringUtils.isNotBlank(code)) id = spaceApi.getSpaceIdBySpaceCode(code);
        if (id == null || id <= 0) throw new ServiceException("请先选择当前空间");
        String actual = spaceApi.getSpaceCodeBySpaceId(id);
        if (StringUtils.isBlank(actual) || StringUtils.isNotBlank(code) && !actual.equals(code))
            throw new ServiceException("空间标识不一致");
        if (!spaceApi.hasUserSpace(userId, id, actual)) throw new ServiceException("当前用户无权访问该空间");
        request.setSpaceId(id); request.setSpaceCode(actual);
    }

    private void validateSession(AskUnifiedReqVO request, Long userId) {
        if (request.getSessionId() == null) return;
        // The real session service owns authorization; the mapper is only for category inspection.
        sessions.listMessages(userId, request.getSpaceId(), request.getSessionId(), null, null, 1);
        AiAskSessionDO session = sessionMapper.selectById(request.getSessionId());
        if (session == null || Boolean.TRUE.equals(session.getDelFlag()) || !Objects.equals(userId, session.getUserId())
                || !Objects.equals(request.getSpaceId(), session.getSpaceId()) || !"ASK_DATA".equals(session.getCategory()))
            throw new ServiceException("请选择当前空间的普通AI问数会话，不能使用Data Agent会话");
    }

    private void validateOntology(AskUnifiedReqVO request) {
        if (request.getOntologyId() == null) return;
        OntologyDO ontology = ontologyMapper.selectById(request.getOntologyId());
        if (ontology == null || ontology.getDelFlag() != null && ontology.getDelFlag() != 0
                || !Objects.equals(request.getSpaceId(), ontology.getSpaceId())
                || StringUtils.isNotBlank(ontology.getSpaceCode()) && !request.getSpaceCode().equals(ontology.getSpaceCode()))
            throw new ServiceException("本体不属于当前可访问空间");
    }

    private SseEmitter immediate(AskUnifiedReqVO request, String status, String message) {
        SseEmitter emitter = new SseEmitter(90000L);
        AtomicBoolean cancelled = new AtomicBoolean();
        send(emitter, cancelled, "result", UnifiedAskAgentService.response(request, request.getOperationId(), status, message));
        send(emitter, cancelled, "done", "[DONE]"); emitter.complete(); return emitter;
    }

    private void send(SseEmitter emitter, AtomicBoolean cancelled, String event, Object data) {
        if (cancelled.get()) return;
        try { emitter.send(SseEmitter.event().name(event).data(data)); }
        catch (IOException | IllegalStateException ex) { cancelled.set(true); }
    }

    @PreDestroy
    public void shutdown() { executor.shutdownNow(); }
}
