package com.datamaster.module.ai.controller.agent;

import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.ai.agent.runtime.AgentRunRequest;
import com.datamaster.module.ai.agent.runtime.AgentRunResponse;
import com.datamaster.module.ai.agent.runtime.IAutonomousAgentService;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** LangChain4j 自主 Tool 调度入口。 */
@RestController
@RequestMapping("/ai/agent/autonomous")
@PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
public class AutonomousAgentController extends BaseController {

    @Resource
    private IAutonomousAgentService autonomousAgentService;
    @Resource
    private ISpaceApi spaceApi;

    /**
     * 统一自主 Agent 消息入口。新问题、继续、补充信息和人工确认都走同一条链路；
     * 是否恢复已有操作、调用哪个 Tool 以及下一步动作由会话记忆和模型自行判断。
     */
    @PostMapping("/message")
    public CommonResult<AgentRunResponse> message(@RequestBody AgentRunRequest request) {
        requireAuthorizedSpace(request);
        if (request != null && (request.getOperationId() == null || request.getOperationId().trim().isEmpty())) {
            request.setOperationId(UUID.randomUUID().toString());
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("entrypoint", "DATA_AGENT");
        attributes.put("confirmed", Boolean.TRUE.equals(request == null ? null : request.getConfirmed()));
        attributes.put("confirmedExecutionId", request == null ? null : request.getConfirmedExecutionId());
        attributes.put("confirmedDatasourceId", request == null ? null : request.getConfirmedDatasourceId());
        attributes.put("confirmedServiceId", request == null ? null : request.getConfirmedServiceId());
        attributes.put("conversationId", request == null ? null : request.getConversationId());
        // 页面路径/页面详情由前端注入给用户可读的 goal；把其中的本体 ID
        // 提升为可信上下文，避免模型必须从自然语言中猜测或要求普通用户手填 ID。
        Long pageOntologyId = extractOntologyId(request == null ? null : request.getGoal());
        if (pageOntologyId != null) attributes.put("ontologyId", pageOntologyId);
        AgentToolContext context = new AgentToolContext(
                request == null ? null : request.getOperationId(), getUserId(), getNickName(),
                request == null ? null : request.getSpaceId(), request == null ? null : request.getSpaceCode(), attributes);
        return CommonResult.success(autonomousAgentService.run(request, context));
    }

    private Long extractOntologyId(String goal) {
        if (goal == null || goal.trim().isEmpty()) return null;
        Matcher matcher = Pattern.compile("(?i)(?:ontologyId|本体ID)\\s*[=:：]\\s*[\\\"']?(\\d+)").matcher(goal);
        if (!matcher.find()) return null;
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @GetMapping("/{id}")
    public CommonResult<AgentRunResponse> get(@PathVariable String id,
                                            @RequestParam(required = false) Long spaceId,
                                            @RequestParam(required = false) String spaceCode) {
        return CommonResult.success(autonomousAgentService.get(id,
                new AgentToolContext(id, getUserId(), getNickName(), spaceId, spaceCode, new LinkedHashMap<>())));
    }

    /**
     * 普通自主调度不能因为进入 Agent 入口就自动获得写权限；只有用户明确确认
     * 才把确认标记传给 Tool 治理切面。资源 ID 仅用于由切面做精确绑定校验，
     * 本身不能充当通用确认开关。
     */
    private void requireAuthorizedSpace(AgentRunRequest request) {
        if (request == null || spaceApi == null
                || !spaceApi.hasUserSpace(getUserId(), request.getSpaceId(), request.getSpaceCode())) {
            throw new com.datamaster.common.exception.ServiceException("当前用户无权访问该空间");
        }
    }
}
