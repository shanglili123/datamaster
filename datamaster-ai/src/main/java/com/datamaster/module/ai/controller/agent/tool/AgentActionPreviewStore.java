package com.datamaster.module.ai.controller.agent.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewRespVO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存同一 Agent 流程最近一次动作预览，供用户确认后的保存动作按索引引用。
 *
 * <p>动作预览通常包含较长的脚本或执行步骤。让模型在确认阶段重新回显整段脚本
 * 容易触发工具参数截断/JSON 解析失败，因此确认阶段只传 previewIndex。</p>
 */
@Component
public class AgentActionPreviewStore {

    private final Map<String, AiActionPreviewRespVO> previews = new ConcurrentHashMap<>();

    public void put(String operationId, AiActionPreviewRespVO preview) {
        if (operationId != null && preview != null) {
            previews.put(operationId, preview);
        }
    }

    public AiActionPreviewRespVO.GeneratedActionPreview get(String operationId, int index) {
        if (operationId == null || index < 0) return null;
        AiActionPreviewRespVO preview = previews.get(operationId);
        if (preview == null || preview.getActions() == null || index >= preview.getActions().size()) return null;
        return preview.getActions().get(index);
    }

    /**
     * 服务重启或页面恢复后，从已持久化的 Agent 结果中恢复最近一次动作预览。
     * resultJson 是 AgentRunResponse，预览位于 toolResults[].data 中。
     */
    public AiActionPreviewRespVO.GeneratedActionPreview restoreFromResultJson(
            String operationId, String resultJson, int index) {
        if (operationId == null || resultJson == null || resultJson.trim().isEmpty() || index < 0) return null;
        try {
            JSONObject root = JSON.parseObject(resultJson);
            JSONArray toolResults = root == null ? null : root.getJSONArray("toolResults");
            if (toolResults == null) return null;
            for (int i = toolResults.size() - 1; i >= 0; i--) {
                JSONObject toolResult = toolResults.getJSONObject(i);
                if (toolResult == null || !"generate_actions".equals(toolResult.getString("toolName"))) continue;
                Object data = toolResult.get("data");
                if (data == null) continue;
                AiActionPreviewRespVO preview = JSON.parseObject(JSON.toJSONString(data), AiActionPreviewRespVO.class);
                if (preview == null || preview.getActions() == null || index >= preview.getActions().size()) continue;
                put(operationId, preview);
                return preview.getActions().get(index);
            }
        } catch (Exception ignored) {
            // A malformed historical result must not prevent a normal new preview from being generated.
        }
        return null;
    }
}
