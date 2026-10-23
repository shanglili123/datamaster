package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyPageReqVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.service.IOntologyService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 按本体 ID、名称、编码或状态查询当前空间的本体候选。 */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class SearchOntologiesTool {

    @Resource private IOntologyService ontologyService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "search_ontologies", value = "查询当前空间可见的本体候选；支持 ontologyId、name、code、status 条件，适合在缺少 ontologyId 时按用户说的本体名称查找；只查询不修改")
    public AgentToolResult execute(AgentToolRequest.SearchOntologies input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        OntologyPageReqVO request = new OntologyPageReqVO();
        request.setId(input == null ? null : AgentToolIds.longValue(input.getOntologyId()));
        request.setName(ontologyText(input == null ? null : input.getName()));
        request.setCode(text(input == null ? null : input.getCode()));
        request.setStatus(input == null ? null : input.getStatus());
        // 空间 ID 是主键级别的归属，优先使用它。历史本体可能只写入了
        // spaceId 或 spaceCode，两个字段同时 AND 会把合法本体过滤掉。
        if (context.getSpaceId() != null) request.setSpaceId(context.getSpaceId());
        else request.setSpaceCode(context.getSpaceCode());
        request.setPageNum(1);
        request.setPageSize(limit(input == null ? null : input.getLimit()));
        PageResult<OntologyRespVO> page = ontologyService.getOntologyPage(request);
        List<OntologyRespVO> rows = page == null || page.getRows() == null
                ? java.util.Collections.emptyList() : page.getRows();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", rows);
        data.put("total", page == null || page.getTotal() == null ? rows.size() : page.getTotal());
        data.put("uniqueMatch", rows.size() == 1);
        data.put("searchedName", input == null ? null : ontologyText(input.getName()));
        data.put("searchedCode", input == null ? null : text(input.getCode()));
        data.put("searchedSpaceId", context.getSpaceId());
        if (rows.size() == 1 && rows.get(0) != null) data.put("ontologyId", rows.get(0).getId());
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, data,
                rows.size() == 1 ? "已找到唯一匹配本体，可继续调用 generate_actions"
                        : rows.isEmpty()
                        ? "当前查询条件未找到本体；已按当前空间并忽略英文大小写查询。请先调整名称/编码条件或去掉名称重新查询，不要据此创建新本体"
                        : "已返回本体候选，请根据名称、编码和状态选择 ontologyId",
                operationId(context));
    }

    private int limit(Integer value) { return value == null ? 20 : Math.max(1, Math.min(value, 100)); }
    private String text(Object value) { return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim(); }
    /**
     * 模型从自然语言中抽取本体名称时，可能保留中文/英文引号，或把名称连同
     * “本体名称是”等提示词一起传入。查询层统一归一化，避免合法本体被误判为不存在。
     */
    private String ontologyText(Object value) {
        String valueText = text(value);
        if (valueText == null) return null;
        String normalized = valueText
                .replace('\u201c', ' ').replace('\u201d', ' ')
                .replace('\u2018', ' ').replace('\u2019', ' ')
                .replace('"', ' ').replace('\'', ' ')
                .trim();
        normalized = normalized.replaceFirst("^(本体(?:名称)?|ontology(?:Name)?)\\s*(?:为|是|叫|[=:：])\\s*", "");
        normalized = normalized.replaceFirst("^(?:当前|系统里已有的|已有的)\\s*", "").trim();
        return normalized.isEmpty() ? null : normalized;
    }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
