package com.datamaster.module.ai.controller.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewRespVO;
import org.junit.Test;

import java.util.Collections;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 动作预览在 Agent 操作恢复时必须能够从持久化结果重新建立索引。 */
public class AgentActionPreviewStoreTest {

    @Test
    public void restoresPreviewFromPersistedAgentResult() {
        AiActionPreviewRespVO preview = new AiActionPreviewRespVO();
        AiActionPreviewRespVO.GeneratedActionPreview action = new AiActionPreviewRespVO.GeneratedActionPreview();
        action.setActionName("排产");
        action.setActionType("FUNCTION");
        preview.setActions(Collections.singletonList(action));

        AgentToolResult toolResult = AgentToolResult.of(
                AgentToolResult.SUCCEEDED, preview, "动作预览生成完成，需审核后保存", "op-1");
        toolResult.setToolName("generate_actions");
        AgentRunResultEnvelope envelope = new AgentRunResultEnvelope(Collections.singletonList(toolResult));

        AgentActionPreviewStore store = new AgentActionPreviewStore();
        AiActionPreviewRespVO.GeneratedActionPreview restored = store.restoreFromResultJson(
                "op-1", JSON.toJSONString(envelope), 0);

        assertNotNull(restored);
        assertEquals("排产", restored.getActionName());
        assertEquals("FUNCTION", restored.getActionType());
    }

    @Test
    public void restoresPreviewWhenConfirmationResultIsAppendedAfterGeneration() {
        AiActionPreviewRespVO preview = new AiActionPreviewRespVO();
        AiActionPreviewRespVO.GeneratedActionPreview action = new AiActionPreviewRespVO.GeneratedActionPreview();
        action.setActionName("ATP可用承诺计算预览");
        action.setActionType("FUNCTION");
        preview.setActions(Collections.singletonList(action));

        AgentToolResult generated = AgentToolResult.of(
                AgentToolResult.SUCCEEDED, preview, "动作预览生成完成，需审核后保存", "op-2");
        generated.setToolName("generate_actions");
        AgentToolResult confirmation = AgentToolResult.of(
                AgentToolResult.NEED_CONFIRMATION, null, "Tool create_action属于写操作，需要用户确认后执行", "op-2");
        confirmation.setToolName("create_action");

        AgentRunResultEnvelope envelope = new AgentRunResultEnvelope(
                Arrays.asList(generated, confirmation));
        AgentActionPreviewStore store = new AgentActionPreviewStore();

        AiActionPreviewRespVO.GeneratedActionPreview restored = store.restoreFromResultJson(
                "op-2", JSON.toJSONString(envelope), 0);

        assertNotNull(restored);
        assertEquals("ATP可用承诺计算预览", restored.getActionName());
    }

    private static class AgentRunResultEnvelope {
        private final java.util.List<AgentToolResult> toolResults;

        private AgentRunResultEnvelope(java.util.List<AgentToolResult> toolResults) {
            this.toolResults = toolResults;
        }

        public java.util.List<AgentToolResult> getToolResults() {
            return toolResults;
        }
    }
}
