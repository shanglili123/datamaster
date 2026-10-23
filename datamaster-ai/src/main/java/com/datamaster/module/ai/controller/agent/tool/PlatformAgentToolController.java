package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolCatalog;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tool 目录和受控人工调用入口；自主 Agent 的 Tool 注册由 LangChain4j AiServices 完成。 */
@RestController
@RequestMapping("/ai/agent/tools")
@PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
public class PlatformAgentToolController extends BaseController {

    @Resource
    private AgentToolCatalog toolCatalog;

    @Resource
    private ISpaceApi spaceApi;

    @GetMapping
    public CommonResult<?> list() {
        return CommonResult.success(toolCatalog.describe());
    }

    @PostMapping("/{name}/invoke")
    public CommonResult<AgentToolResult> invoke(@PathVariable String name,
                                                @RequestBody(required = false) Map<String, Object> arguments) {
        Map<String, Object> args = arguments == null ? Collections.emptyMap() : arguments;
        Long spaceId = longValue(args.get("spaceId"));
        String spaceCode = stringValue(args.get("spaceCode"));
        if (spaceApi == null || !spaceApi.hasUserSpace(getUserId(), spaceId, spaceCode)) {
            throw new ServiceException("当前用户无权访问该空间");
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("confirmed", Boolean.TRUE.equals(args.get("confirmed"))
                || "true".equalsIgnoreCase(stringValue(args.get("confirmed"))));
        attributes.put("confirmedExecutionId", longValue(args.get("confirmedExecutionId")));
        attributes.put("confirmedDatasourceId", longValue(args.get("confirmedDatasourceId")));
        attributes.put("confirmedServiceId", longValue(args.get("confirmedServiceId")));
        attributes.put("confirmedTaskId", longValue(args.get("confirmedTaskId")));
        attributes.put("conversationId", stringValue(args.get("conversationId")));
        AgentToolContext context = new AgentToolContext(
                stringValue(args.get("operationId")), getUserId(), getNickName(), spaceId, spaceCode,
                attributes);
        // 空间、确认和会话字段属于可信调用上下文，不作为 Tool DTO 参数暴露给模型或业务 Tool。
        Map<String, Object> toolArguments = new LinkedHashMap<>(args);
        toolArguments.remove("spaceId");
        toolArguments.remove("spaceCode");
        toolArguments.remove("operationId");
        toolArguments.remove("confirmed");
        toolArguments.remove("confirmedExecutionId");
        toolArguments.remove("confirmedDatasourceId");
        toolArguments.remove("confirmedServiceId");
        toolArguments.remove("confirmedTaskId");
        toolArguments.remove("conversationId");
        return CommonResult.success(toolCatalog.invoke(name, context, toolArguments));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
