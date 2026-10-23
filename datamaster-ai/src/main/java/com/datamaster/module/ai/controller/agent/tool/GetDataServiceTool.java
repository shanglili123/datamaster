package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.service.api.api.dto.ServiceApiRespDTO;
import com.datamaster.module.service.api.service.api.ServiceApiService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 查询当前空间单个数据服务的安全契约摘要。 */
@Component
@AgentTool
public class GetDataServiceTool {

    @Resource
    private ServiceApiService serviceApiService;

    @Tool(name = "get_data_service", value = "按真实数据服务 ID 或名称查询当前空间的数据服务详情；不会返回 SQL、连接配置或密钥")
    public AgentToolResult execute(AgentToolRequest.GetDataService input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || context.getSpaceId() == null || context.getSpaceCode() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        AgentToolRequest.GetDataService request = input == null
                ? new AgentToolRequest.GetDataService() : input;
        try {
            Long id = AgentToolIds.longValue(request.getServiceId());
            if (id != null) {
                ServiceApiRespDTO service = serviceApiService.getVisibleServiceApi(
                        id, context.getSpaceId(), context.getSpaceCode());
                if (service == null) return AgentToolResult.failure("数据服务不存在或不属于当前空间");
                return AgentToolResult.of(AgentToolResult.SUCCEEDED,
                        ListDataServicesTool.summary(service), "已找到数据服务", operationId(context));
            }
            String name = request.getName() == null ? null : request.getName().trim();
            if (name == null || name.isEmpty()) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "请提供真实 serviceId 或数据服务名称", operationId(context));
            }
            List<ServiceApiRespDTO> candidates = serviceApiService.listVisibleServiceApis(
                    context.getSpaceId(), context.getSpaceCode(), name, "1", 20);
            if (candidates == null || candidates.isEmpty()) {
                return AgentToolResult.failure("当前空间未找到数据服务: " + name);
            }
            if (candidates.size() > 1) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (ServiceApiRespDTO candidate : candidates) rows.add(ListDataServicesTool.summary(candidate));
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, rows,
                        "数据服务名称匹配多条，请选择真实 serviceId", operationId(context));
            }
            return AgentToolResult.of(AgentToolResult.SUCCEEDED,
                    ListDataServicesTool.summary(candidates.get(0)), "已找到数据服务", operationId(context));
        } catch (ServiceException ex) {
            return AgentToolResult.failure(ex.getMessage());
        }
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
