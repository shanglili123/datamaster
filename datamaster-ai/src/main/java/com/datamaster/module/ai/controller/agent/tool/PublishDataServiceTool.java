package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.service.api.api.dto.ServiceApiRespDTO;
import com.datamaster.module.service.api.service.api.ServiceApiService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/** 发布当前空间内已确认的数据服务草稿。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true, confirmationArgument = "serviceId")
public class PublishDataServiceTool {

    @Resource
    private ServiceApiService serviceApiService;
    @Resource
    private AgentToolResourceGuard resourceGuard;

    @Tool(name = "publish_data_service", value = "发布当前空间内的数据服务草稿；必须先用 get_data_service 确认真实 serviceId，并且需要用户确认同一个 serviceId")
    public AgentToolResult execute(AgentToolRequest.PublishDataService input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "发布数据服务前请先确定当前空间", operationId(context));
        }
        if (input == null || input.getServiceId() == null || input.getServiceId().trim().isEmpty()) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "发布数据服务需要 serviceId", operationId(context));
        }
        Long serviceId = AgentToolIds.longValue(input.getServiceId());
        ServiceApiRespDTO service = serviceApiService.getVisibleServiceApi(
                serviceId, context.getSpaceId(), context.getSpaceCode());
        if (service == null) return AgentToolResult.failure("数据服务不存在或不属于当前空间");
        if ("1".equals(service.getStatus())) {
            return AgentToolResult.of(AgentToolResult.SUCCEEDED, summary(service),
                    "数据服务已经发布，无需重复发布", operationId(context));
        }
        serviceApiService.publishDataService(serviceId, context.getSpaceId(), context.getSpaceCode(),
                context.getUserId(), context.getUserName());
        Map<String, Object> result = summary(service);
        result.put("status", "1");
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result,
                "数据服务发布成功", operationId(context));
    }

    private Map<String, Object> summary(ServiceApiRespDTO service) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceId", service.getId() == null ? null : String.valueOf(service.getId()));
        result.put("name", service.getName());
        result.put("apiUrl", service.getApiUrl());
        result.put("status", service.getStatus());
        return result;
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
