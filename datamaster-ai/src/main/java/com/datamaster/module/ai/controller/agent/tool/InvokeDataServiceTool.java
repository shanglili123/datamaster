package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.service.api.service.api.ServiceApiService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 调用当前空间已发布的表单查询型数据服务。 */
@Component
@AgentTool
public class InvokeDataServiceTool {

    private static final int MAX_LIST_RESULTS = 200;

    @Resource
    private ServiceApiService serviceApiService;

    @Tool(name = "invoke_data_service", value = "调用当前空间已发布的表单查询型数据服务；只能传递服务声明的查询参数，不可修改 SQL、数据源或执行配置")
    public AgentToolResult execute(AgentToolRequest.InvokeDataService input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || context.getSpaceId() == null || context.getSpaceCode() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        if (input == null || input.getServiceId() == null || input.getServiceId().trim().isEmpty()) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "调用数据服务需要真实 serviceId", operationId(context));
        }
        try {
            Long serviceId = AgentToolIds.longValue(input.getServiceId());
            if (serviceId == null) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "调用数据服务需要真实 serviceId", operationId(context));
            }
            Map<String, Object> params = input.getParams() == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(input.getParams());
            normalizePagingParam(params, "pageNum");
            normalizePagingParam(params, "pageSize");
            Object pageSize = params.get("pageSize");
            if (pageSize instanceof Number && ((Number) pageSize).intValue() > MAX_LIST_RESULTS) {
                params.put("pageSize", MAX_LIST_RESULTS);
            }
            Object value = serviceApiService.invokeVisibleServiceApi(
                    serviceId, context.getSpaceId(), context.getSpaceCode(), params);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("serviceId", String.valueOf(serviceId));
            result.put("params", params);
            result.put("data", trimResult(value));
            return AgentToolResult.of(AgentToolResult.SUCCEEDED, result,
                    "数据服务调用成功", operationId(context));
        } catch (ServiceException ex) {
            return AgentToolResult.failure(ex.getMessage());
        } catch (Exception ex) {
            return AgentToolResult.failure("数据服务调用失败: " + ex.getMessage());
        }
    }

    private Object trimResult(Object value) {
        if (!(value instanceof List)) return value;
        List<?> rows = (List<?>) value;
        return rows.size() <= MAX_LIST_RESULTS ? rows : rows.subList(0, MAX_LIST_RESULTS);
    }

    /** JSON 数字进入 Map 后可能是 Double（例如 1.0），服务层分页参数需要整数文本。 */
    private void normalizePagingParam(Map<String, Object> params, String name) {
        Object value = params.get(name);
        if (value instanceof Number) {
            params.put(name, String.valueOf(((Number) value).intValue()));
        } else if (value != null) {
            String text = String.valueOf(value).trim();
            try {
                params.put(name, String.valueOf((int) Double.parseDouble(text)));
            } catch (NumberFormatException ignored) {
                // 服务层返回可读的参数错误，不在 Tool 层吞掉原值。
            }
        }
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
