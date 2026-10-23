package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.service.api.api.dto.ServiceApiRespDTO;
import com.datamaster.module.service.api.service.api.ServiceApiService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 查询当前空间已发布的数据服务目录。 */
@Component
@AgentTool
public class ListDataServicesTool {

    @Resource
    private ServiceApiService serviceApiService;

    @Tool(name = "list_data_services", value = "查询当前空间已发布的数据服务，可按名称筛选；只返回服务契约摘要，不返回 SQL、连接配置或密钥")
    public AgentToolResult execute(AgentToolRequest.ListDataServices input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || context.getSpaceId() == null || context.getSpaceCode() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        AgentToolRequest.ListDataServices request = input == null
                ? new AgentToolRequest.ListDataServices() : input;
        int limit = request.getLimit() == null ? 20 : Math.max(1, Math.min(request.getLimit(), 100));
        List<ServiceApiRespDTO> services = serviceApiService.listVisibleServiceApis(
                context.getSpaceId(), context.getSpaceCode(), request.getName(), request.getStatus(), limit);
        List<Map<String, Object>> result = new ArrayList<>();
        if (services != null) {
            for (ServiceApiRespDTO service : services) {
                if (service != null) result.add(summary(service));
            }
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result,
                "已查询当前空间数据服务", operationId(context));
    }

    static Map<String, Object> summary(ServiceApiRespDTO service) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", service.getId() == null ? null : String.valueOf(service.getId()));
        item.put("name", service.getName());
        item.put("apiVersion", service.getApiVersion());
        item.put("apiUrl", service.getApiUrl());
        item.put("reqMethod", service.getReqMethod());
        item.put("apiServiceType", service.getApiServiceType());
        item.put("resDataType", service.getResDataType());
        item.put("catCode", service.getCatCode());
        item.put("description", service.getDescription());
        item.put("status", service.getStatus());
        item.put("requestFields", requestFields(service));
        return item;
    }

    private static List<Map<String, Object>> requestFields(ServiceApiRespDTO service) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (service.getReqParamsList() == null) return result;
        service.getReqParamsList().forEach(param -> {
            if (param == null) return;
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("name", param.getParamName());
            field.put("type", param.getParamType());
            field.put("nullable", param.getNullable());
            field.put("description", param.getParamComment());
            result.add(field);
        });
        return result;
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
