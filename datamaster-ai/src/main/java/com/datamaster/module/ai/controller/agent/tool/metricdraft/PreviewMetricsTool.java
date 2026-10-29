package com.datamaster.module.ai.controller.agent.tool.metricdraft;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.*;
import com.datamaster.module.ai.controller.agent.tool.MetricAgentToolSupport;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;

@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class PreviewMetricsTool {
    @Resource private MetricAgentToolSupport support;
    @Resource private MetricDraftSupport drafts;

    @Tool(name = "preview_metrics", value = "DataAgent建设侧指标候选预览，仅读取当前本体概念/属性/绑定和数据源元数据，不查Doris、不生成SQL、不保存指标。ontologyId/conceptId及全部业务ID必须为字符串。suggestions最多5个；不传则基于已有Doris/ADS绑定生成COUNT(*)及真实数值属性SUM候选。候选口径待业务确认，缺少合理绑定返回NEED_INPUT而不自动建绑定。使用返回的previewDigest和index调用save_metric_draft，保存前需用户确认。")
    public AgentToolResult execute(MetricDraftToolRequest.Preview input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:add", "ont:concept:query");
        if (denied != null) return denied;
        try {
            if (input == null) return support.result(AgentToolResult.NEED_INPUT, null, "需要ontologyId、conceptId", context);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("schemaVersion", MetricDraftSupport.SCHEMA);
            data.put("ontologyId", input.getOntologyId()); data.put("conceptId", input.getConceptId());
            data.put("previewOperationId", drafts.operation(context).getId());
            data.put("suggestions", drafts.preview(input, context));
            data.put("previewDigest", MetricDraftConfirmation.hash(data));
            return support.result(AgentToolResult.SUCCEEDED, data,
                    "仅生成候选预览；候选口径待业务确认。owner/unit可为空，grain为unknown时待补；未保存、未发布、未连接Doris。ADS命名并非访问授权。", context);
        } catch (IllegalArgumentException ex) {
            return support.result(AgentToolResult.NEED_INPUT, null, ex.getMessage(), context);
        } catch (Exception ex) {
            return support.result(AgentToolResult.FAILED, null, "无法验证当前空间、本体及真实Doris/ADS元数据，请检查绑定和权限", context);
        }
    }
}
