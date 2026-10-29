package com.datamaster.module.ai.controller.agent.tool.metricdraft;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datamaster.module.ai.agent.runtime.AgentRunResponse;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.*;
import com.datamaster.module.ai.controller.agent.tool.MetricAgentToolSupport;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.dal.mapper.agent.AiAgentOperationMapper;
import com.datamaster.module.ontology.service.IMetricService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.Resource;
import java.util.*;

@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = false)
public class SaveMetricDraftTool {
    @Resource private MetricAgentToolSupport support;
    @Resource private MetricDraftSupport drafts;
    @Resource private IMetricService metricService;
    @Resource private AiAgentOperationMapper operationMapper;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper metadataLockMapper;

    @Tool(name = "save_metric_draft", value = "仅将当前operation最近成功preview_metrics的一项保存为指标草稿。只接受previewOperationId(默认当前)、previewIndex(0到4)、previewDigest(预览返回原值)，不接受客户端指标定义。首次必定NEED_CONFIRMATION；用户确认后服务端原样恢复最后待确认参数并复核指纹，一次确认只保存一项。不会发布、生成SQL或写入Doris。")
    public AgentToolResult execute(MetricDraftToolRequest.Save input) {
        AgentToolContext context = AgentToolContextHolder.get();
        AgentToolResult denied = support.guard(context, "ont:metric:add", "ont:metric:query", "ont:concept:query");
        if (denied != null) return denied;
        try {
            if (input == null || input.getPreviewIndex() == null || input.getPreviewIndex() < 0 || input.getPreviewIndex() > 4
                    || input.getPreviewDigest() == null || !input.getPreviewDigest().matches("[0-9a-f]{64}")) {
                return support.result(AgentToolResult.NEED_INPUT, null, "需要真实预览返回的previewDigest和previewIndex(0到4)", context);
            }
            AiAgentOperationDO operation = drafts.operation(context);
            if (input.getPreviewOperationId() != null && !operation.getId().equals(input.getPreviewOperationId())) {
                throw new IllegalArgumentException("只允许保存当前操作的预览");
            }
            MetricDraftToolRequest.Save arguments = new MetricDraftToolRequest.Save();
            arguments.setPreviewOperationId(operation.getId()); arguments.setPreviewIndex(input.getPreviewIndex());
            arguments.setPreviewDigest(input.getPreviewDigest());
            boolean confirmed = Boolean.TRUE.equals(context.getAttributes().get("confirmed"));
            // Only an initial request may see an unpersisted same-turn preview; confirmation always uses DB.
            Selection selection = select(operation, arguments, context, !confirmed);
            String hash = confirmationHash(arguments, selection.row);
            if (consumed(operation.getResultJson(), hash)) {
                return support.result(AgentToolResult.NEED_INPUT, null, "此草稿确认已消费，请查看已保存指标或重新预览", context);
            }
            if (confirmed) {
                if (!matchesPending(operation, hash, arguments)) {
                    return support.result(AgentToolResult.NEED_INPUT, null, "最后待确认项已变化或已消费，请重新预览并确认", context);
                }
                for (AgentToolResult result : AgentToolContextHolder.results()) {
                    if (isPreview(result, operation.getId())) throw new IllegalArgumentException("确认轮出现新预览，必须重新确认");
                }
                Long savedId = new TransactionTemplate(transactionManager).execute(status -> {
                    AiAgentOperationDO locked = operationMapper.selectOne(new QueryWrapper<AiAgentOperationDO>()
                            .eq("id", operation.getId()).eq("creator_id", context.getUserId()).eq("del_flag", false).last("FOR UPDATE"));
                    drafts.assertOperation(locked, context);
                    if (!matchesPending(locked, hash, arguments)) throw new IllegalArgumentException("确认已变化或已消费");
                    if (metadataLockMapper.lockOntology(support.positiveId(selection.ontologyId)) == null) {
                        throw new IllegalArgumentException("本体不存在或已删除");
                    }
                    // Re-read the last persisted preview and ALL live references inside the row-lock transaction.
                    Selection fresh = select(locked, arguments, context, false);
                    if (!hash.equals(confirmationHash(arguments, fresh.row))) throw new IllegalArgumentException("预览元数据已变化");
                    Long id = metricService.create(drafts.definition(fresh.ontologyId, fresh.conceptId, fresh.candidate));
                    AgentRunResponse state = JSON.parseObject(locked.getResultJson(), AgentRunResponse.class);
                    for (AgentToolResult prior : state.getToolResults()) {
                        if (prior != null && MetricDraftConfirmation.TOOL.equals(prior.getToolName())
                                && hash.equals(prior.getMetadata().get("confirmationHash"))) {
                            Map<String, Object> metadata = new LinkedHashMap<>(prior.getMetadata());
                            metadata.put("confirmationConsumed", true); metadata.put("metricId", String.valueOf(id));
                            prior.setMetadata(metadata); prior.setMessage("草稿保存确认已消费");
                        }
                    }
                    state.setStatus(AgentToolResult.SUCCEEDED); state.setSuccess(true);
                    state.setMessage("草稿保存确认已消费"); state.setConfirmationType(null); state.setConfirmationMessage(null);
                    locked.setStatus(AgentToolResult.SUCCEEDED); locked.setConfirmationType(null); locked.setConfirmationMessage(null);
                    locked.setResultJson(JSON.toJSONString(state));
                    if (operationMapper.updateById(locked) != 1) throw new IllegalStateException("确认消费持久化失败");
                    return id;
                });
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", String.valueOf(savedId)); data.put("metricId", String.valueOf(savedId));
                data.put("name", selection.candidate.getName()); data.put("code", selection.candidate.getCode());
                data.put("status", "DRAFT"); data.put("publishedVersion", 0);
                AgentToolResult result = support.result(AgentToolResult.SUCCEEDED, data, "已保存一项指标草稿，未发布、未生成SQL、未写入Doris", context);
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("confirmationHash", hash); metadata.put("confirmationConsumed", true);
                metadata.put("toolName", MetricDraftConfirmation.TOOL); metadata.put("metricId", String.valueOf(savedId));
                result.setMetadata(metadata);
                return result;
            }
            AgentToolResult result = support.result(AgentToolResult.NEED_CONFIRMATION, selection.row,
                    "请确认保存所示名称、编码和完整候选口径为草稿；不发布指标、不查询或写入Doris。一次确认仅保存这一项。", context);
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("toolName", MetricDraftConfirmation.TOOL); metadata.put("risk", "WRITE");
            metadata.put("arguments", JSON.parseObject(MetricDraftConfirmation.canonicalJson(arguments)));
            metadata.put("confirmationHash", hash); metadata.put("confirmationConsumed", false);
            result.setMetadata(metadata);
            return result;
        } catch (IllegalArgumentException ex) {
            return support.result(AgentToolResult.NEED_INPUT, null, ex.getMessage(), context);
        } catch (Exception ex) {
            return support.result(AgentToolResult.FAILED, null, "保存草稿失败，请检查编码是否重复、当前空间/权限与元数据，并重新预览确认", context);
        }
    }

    private Selection select(AiAgentOperationDO operation, MetricDraftToolRequest.Save arguments,
                             AgentToolContext context, boolean includeCurrentTurn) {
        AgentToolResult preview = null;
        if (includeCurrentTurn) {
            List<AgentToolResult> current = AgentToolContextHolder.results();
            for (int i = current.size() - 1; i >= 0; i--) {
                if (isPreview(current.get(i), operation.getId())) { preview = current.get(i); break; }
            }
        }
        if (preview == null && operation.getResultJson() != null) {
            AgentRunResponse state = JSON.parseObject(operation.getResultJson(), AgentRunResponse.class);
            List<AgentToolResult> results = state == null ? null : state.getToolResults();
            if (results != null) for (int i = results.size() - 1; i >= 0; i--) {
                if (isPreview(results.get(i), operation.getId())) { preview = results.get(i); break; }
            }
        }
        if (preview == null || !(preview.getData() instanceof Map)) throw new IllegalArgumentException("需要当前操作的成功预览记录");
        Map<String, Object> data = JSON.parseObject(JSON.toJSONString(preview.getData()));
        if (!MetricDraftSupport.SCHEMA.equals(data.get("schemaVersion"))
                || !operation.getId().equals(data.get("previewOperationId"))
                || !arguments.getPreviewDigest().equals(data.get("previewDigest"))) {
            throw new IllegalArgumentException("预览已变化，请使用最近成功预览的digest");
        }
        Map<String, Object> digestInput = new LinkedHashMap<>(data);
        digestInput.remove("previewDigest");
        if (!arguments.getPreviewDigest().equals(MetricDraftConfirmation.hash(digestInput))) {
            throw new IllegalArgumentException("预览完整性校验失败，请重新预览");
        }
        Object raw = data.get("suggestions");
        if (!(raw instanceof List) || ((List<?>) raw).isEmpty() || ((List<?>) raw).size() > 5
                || arguments.getPreviewIndex() >= ((List<?>) raw).size()) throw new IllegalArgumentException("候选索引无效");
        Object item = ((List<?>) raw).get(arguments.getPreviewIndex());
        if (!(item instanceof Map)) throw new IllegalArgumentException("候选格式无效");
        Map<?, ?> row = (Map<?, ?>) item;
        if (!String.valueOf(arguments.getPreviewIndex()).equals(String.valueOf(row.get("index")))) throw new IllegalArgumentException("候选索引不一致");
        String ontologyId = String.valueOf(data.get("ontologyId"));
        String conceptId = String.valueOf(data.get("conceptId"));
        MetricDraftToolRequest.Candidate candidate = JSON.parseObject(JSON.toJSONString(row.get("definition")), MetricDraftToolRequest.Candidate.class);
        if (candidate == null) throw new IllegalArgumentException("缺少服务端预览定义");
        Map<String, Object> revalidated = drafts.validated(ontologyId, conceptId, candidate, context);
        if (!Objects.equals(row.get("candidateHash"), revalidated.get("candidateHash"))
                || !Objects.equals(row.get("canonicalDefinition"), revalidated.get("canonicalDefinition"))) {
            throw new IllegalArgumentException("候选或真实元数据发生变化，需要重新预览");
        }
        return new Selection(ontologyId, conceptId, candidate, revalidated);
    }

    private boolean isPreview(AgentToolResult result, String operationId) {
        return result != null && "preview_metrics".equals(result.getToolName())
                && AgentToolResult.SUCCEEDED.equals(result.getStatus()) && operationId.equals(result.getOperationId());
    }

    private String confirmationHash(MetricDraftToolRequest.Save arguments, Map<String, Object> row) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("toolName", MetricDraftConfirmation.TOOL); definition.put("arguments", arguments);
        definition.put("canonicalDefinition", row.get("canonicalDefinition")); definition.put("candidateHash", row.get("candidateHash"));
        return MetricDraftConfirmation.hash(definition);
    }

    private boolean matchesPending(AiAgentOperationDO operation, String hash, MetricDraftToolRequest.Save arguments) {
        AgentToolResult pending = MetricDraftConfirmation.pending(operation.getStatus(), operation.getResultJson());
        return pending != null && hash.equals(pending.getMetadata().get("confirmationHash"))
                && MetricDraftConfirmation.canonicalJson(arguments).equals(
                        MetricDraftConfirmation.canonicalJson(MetricDraftConfirmation.arguments(pending)));
    }

    private boolean consumed(String json, String hash) {
        if (json == null) return false;
        AgentRunResponse state = JSON.parseObject(json, AgentRunResponse.class);
        if (state != null && state.getToolResults() != null) for (AgentToolResult result : state.getToolResults()) {
            if (result != null && MetricDraftConfirmation.TOOL.equals(result.getToolName())
                    && hash.equals(result.getMetadata().get("confirmationHash"))
                    && Boolean.TRUE.equals(result.getMetadata().get("confirmationConsumed"))) return true;
        }
        return false;
    }

    private static final class Selection {
        private final String ontologyId;
        private final String conceptId;
        private final MetricDraftToolRequest.Candidate candidate;
        private final Map<String, Object> row;
        private Selection(String ontologyId, String conceptId, MetricDraftToolRequest.Candidate candidate, Map<String, Object> row) {
            this.ontologyId = ontologyId; this.conceptId = conceptId; this.candidate = candidate; this.row = row;
        }
    }
}
