package com.datamaster.module.ai.controller.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.analysis.MetricResultEvidence;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.*;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.IMetricAnalysisService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** 确认精确绑定名称、说明及全部查询条件，而非沿用其他写操作的通用确认。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = false)
public class SaveMetricAnalysisTool {
    @Resource private MetricAgentToolSupport support;
    @Resource private IMetricAnalysisService analysisService;
    @Resource private IAiAgentOperationService operationService;
    @Resource private com.datamaster.module.ai.dal.mapper.agent.AiAgentOperationMapper operationMapper;
    @Resource private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Tool(name = "save_analysis", value = "保存本人分析条件（不保存结果）。先执行查询，再传入完全相同的query及名称。首次返回NEED_CONFIRMATION，用户确认后原样重放；任何名称、版本、日期或过滤变化必须重新确认。不发布指标、不修改Doris。")
    public AgentToolResult execute(AgentToolRequest.SaveMetricAnalysis input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:query", "ont:analysis:add");
        if (denied != null) return denied;
        try {
            if (input == null || input.getQuery() == null || input.getName() == null || input.getName().trim().isEmpty()
                    || input.getName().length() > 100 || (input.getDescription() != null && input.getDescription().length() > 1000)) {
                return support.result(AgentToolResult.NEED_INPUT, null, "需要名称、说明及先前执行成功的完整查询条件", context);
            }
            AgentToolRequest.ExecuteMetricAnalysis source = input.getQuery();
            Long id = support.positiveId(source.getMetricId());
            Integer version = support.positiveVersion(source.getVersion());
            MetricRespVO published = support.published(id, version, context);
            MetricQueryReqVO query = new MetricQueryReqVO();
            query.setMetricId(id); query.setVersion(version);
            List<Long> dimensions = new ArrayList<>();
            if (source.getDimensionPropertyIds() != null) for (String dimension : source.getDimensionPropertyIds()) dimensions.add(support.positiveId(dimension));
            query.setDimensionPropertyIds(dimensions); query.setFilters(support.filters(source.getFilters()));
            query.setStartTime(source.getStartTime()); query.setEndTime(source.getEndTime());
            query.setLimit(support.limit(source.getLimit(), 100, 100));
            com.datamaster.module.ontology.service.analysis.MetricQueryBuilder.compile(published, query);
            String hash = hash(input);
            AiAgentOperationDO operation = context.getOperationId() == null ? null : operationService.get(context.getOperationId());
            if (operation == null || !operation.getId().startsWith(com.datamaster.module.ai.agent.runtime.AskMetricOperationStore.PREFIX)
                    || !com.datamaster.module.ai.agent.runtime.AskMetricOperationStore.STEP.equals(operation.getCurrentStep())
                    || !Objects.equals(operation.getOntologyId(), published.getOntologyId())
                    || !Objects.equals(operation.getCreatorId(), context.getUserId())) throw new IllegalArgumentException("操作上下文无效");
            boolean sameSpace = context.getSpaceId() != null && operation.getSpaceId() != null
                    ? context.getSpaceId().equals(operation.getSpaceId())
                    : context.getSpaceCode() != null && context.getSpaceCode().equals(operation.getSpaceCode());
            if (!sameSpace) throw new IllegalArgumentException("操作不属于当前空间");
            support.currentOntology(context);
            boolean confirmed = Boolean.TRUE.equals(context.getAttributes().get("confirmed"));
            boolean pending = matchesPending(operation, hash);
            if (confirmed && pending) {
                MetricAnalysisSaveReqVO request = new MetricAnalysisSaveReqVO();
                request.setOntologyId(published.getOntologyId()); request.setName(input.getName());
                request.setDescription(input.getDescription()); request.setQuery(query);
                Long savedId = new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status -> {
                    AiAgentOperationDO locked = operationMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiAgentOperationDO>()
                            .eq("id", operation.getId()).eq("creator_id", context.getUserId()).eq("del_flag", false).last("FOR UPDATE"));
                    if (locked == null || !matchesPending(locked, hash)) throw new IllegalArgumentException("确认已消费或已变化");
                    Long createdId = analysisService.create(request);
                    // 同一确认只能消费一次；与分析创建同事务，避免并发确认生成多份记录。
                    com.datamaster.module.ai.agent.runtime.AgentRunResponse state = JSON.parseObject(locked.getResultJson(), com.datamaster.module.ai.agent.runtime.AgentRunResponse.class);
                    for (AgentToolResult prior : state.getToolResults()) {
                        if ("save_analysis".equals(prior.getToolName()) && hash.equals(prior.getMetadata().get("confirmationHash"))) {
                            prior.getMetadata().put("confirmationConsumed", true);
                            prior.getMetadata().put("analysisId", String.valueOf(createdId));
                            prior.setMessage("保存确认已消费");
                        }
                    }
                    locked.setStatus(AgentToolResult.SUCCEEDED);
                    locked.setResultJson(JSON.toJSONString(state));
                    operationMapper.updateById(locked);
                    return createdId;
                });
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("analysisId", String.valueOf(savedId)); data.put("name", input.getName());
                data.put("metricId", String.valueOf(id)); data.put("version", String.valueOf(version));
                return support.result(AgentToolResult.SUCCEEDED, data, "个人分析条件已保存，未保存数据结果或执行新查询", context);
            }
            // 必须有本轮同一查询成功证据；不接受模型编造查询再直接持久化。
            Map<?, ?> evidence = MetricResultEvidence.latest(String.valueOf(id), String.valueOf(version), context);
            Object evidenceQuery = evidence.get("queryDefinition");
            MetricQueryReqVO prior = JSON.parseObject(JSON.toJSONString(evidenceQuery), MetricQueryReqVO.class);
            if (!JSON.toJSONString(query).equals(JSON.toJSONString(prior))) throw new IllegalArgumentException("查询条件与执行证据不同");
            AgentToolResult result = support.result(AgentToolResult.NEED_CONFIRMATION, null, "请确认保存此名称及完整查询条件；只保存个人分析，不保存结果", context);
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("toolName", "save_analysis"); metadata.put("risk", "WRITE");
            metadata.put("confirmationHash", hash);
            metadata.put("arguments", com.datamaster.module.ai.agent.runtime.AgentPayloadSanitizer.sanitizeValue(null,
                    JSON.parseObject(JSON.toJSONString(input))));
            result.setMetadata(metadata);
            return result;
        } catch (Exception ex) {
            return support.result(AgentToolResult.FAILED, null, "保存分析失败，请重新执行查询并确认相同条件；检查空间及个人分析权限", context);
        }
    }

    private boolean matchesPending(AiAgentOperationDO operation, String hash) {
        AgentToolResult pending = com.datamaster.module.ai.agent.analysis.MetricSaveConfirmation.pending(
                operation.getStatus(), operation.getResultJson());
        return pending != null && hash.equals(pending.getMetadata().get("confirmationHash"));
    }

    private String hash(AgentToolRequest.SaveMetricAnalysis input) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(JSON.toJSONString(input).getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }
}
