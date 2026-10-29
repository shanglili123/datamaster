package com.datamaster.module.ai.controller.agent.tool.metricdraft;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.controller.agent.tool.MetricAgentToolSupport;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.metric.MetricRules;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;

/** Metadata-only draft boundary. Never opens a source connection, runs SQL, or creates bindings. */
@Component
public class MetricDraftSupport {
    public static final String SCHEMA = "metric-draft-preview-v1";
    @Resource private MetricAgentToolSupport support;
    @Resource private ConceptMapper conceptMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private PropertyColumnMapper propertyColumnMapper;
    @Resource private IDatasourceApiService datasourceApiService;
    @Resource private IAiAgentOperationService operationService;

    public AiAgentOperationDO operation(AgentToolContext context) {
        AiAgentOperationDO operation = context.getOperationId() == null ? null : operationService.get(context.getOperationId());
        assertOperation(operation, context);
        return operation;
    }

    public void assertOperation(AiAgentOperationDO operation, AgentToolContext context) {
        if (operation == null || operation.getId() != null && operation.getId().startsWith("ASK_METRIC_")
                || Boolean.TRUE.equals(operation.getDelFlag())
                || !Objects.equals(operation.getId(), context.getOperationId())
                || !Objects.equals(operation.getCreatorId(), context.getUserId())) throw new IllegalArgumentException("操作归属无效");
        boolean scoped = false;
        if (context.getSpaceId() != null) {
            if (!Objects.equals(context.getSpaceId(), operation.getSpaceId())) throw new IllegalArgumentException("操作空间不一致");
            scoped = true;
        }
        if (context.getSpaceCode() != null && !context.getSpaceCode().isEmpty()) {
            if (operation.getSpaceCode() == null || !context.getSpaceCode().equalsIgnoreCase(operation.getSpaceCode())) {
                throw new IllegalArgumentException("操作空间不一致");
            }
            scoped = true;
        }
        if (!scoped) throw new IllegalArgumentException("缺少操作空间");
    }

    public ConceptDO concept(String ontology, String concept, AgentToolContext context) {
        Long ontologyId = support.positiveId(ontology);
        Long conceptId = support.positiveId(concept);
        support.assertOntology(ontologyId, context);
        Long current = support.currentOntology(context);
        if (!ontologyId.equals(current)) throw new IllegalArgumentException("需要当前本体的概念");
        AiAgentOperationDO operation = operation(context);
        if (operation.getOntologyId() != null && !ontologyId.equals(operation.getOntologyId())) {
            throw new IllegalArgumentException("操作本体不一致");
        }
        ConceptDO row = conceptMapper.selectById(conceptId);
        if (row == null || !ontologyId.equals(row.getOntologyId())) throw new IllegalArgumentException("概念不属于当前本体");
        return row;
    }

    public List<Map<String, Object>> preview(MetricDraftToolRequest.Preview request, AgentToolContext context) {
        ConceptDO concept = concept(request.getOntologyId(), request.getConceptId(), context);
        List<MetricDraftToolRequest.Candidate> suggestions = request.getSuggestions();
        if (suggestions == null || suggestions.isEmpty()) suggestions = automatic(concept, context);
        if (suggestions.size() > 5) throw new IllegalArgumentException("最多预览5个候选");
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (MetricDraftToolRequest.Candidate candidate : suggestions) {
            if (candidate == null) throw new IllegalArgumentException("候选不能为空");
            Map<String, Object> row = validated(request.getOntologyId(), request.getConceptId(), candidate, context);
            if (!codes.add(String.valueOf(((Map<?, ?>) row.get("definition")).get("code")))) {
                throw new IllegalArgumentException("候选编码重复");
            }
            row.put("index", result.size());
            row.put("requiredBeforePublish", Arrays.asList("确认候选业务口径、编码和名称", "补齐负责人owner和单位unit",
                    "业务确认一行粒度grain，unknown不代表已确认", "检查时间属性与维度；必要时补齐真实字段绑定",
                    "人工校验并发布；Doris/ADS命名检查仅为元数据检查，不代表查询授权"));
            result.add(row);
        }
        return result;
    }

    private List<MetricDraftToolRequest.Candidate> automatic(ConceptDO concept, AgentToolContext context) {
        ConceptTableDO table = chooseTable(concept.getId(), context);
        List<PropertyDO> properties = propertyMapper.selectList(new QueryWrapper<PropertyDO>()
                .eq("concept_id", concept.getId()).orderByAsc("id"));
        Map<Long, PropertyColumnDO> mappings = columns(table);
        String time = null;
        for (PropertyDO property : properties) {
            if (mappings.containsKey(property.getId()) && temporal(property.getDataType())) {
                time = String.valueOf(property.getId());
                break;
            }
        }
        List<MetricDraftToolRequest.Candidate> result = new ArrayList<>();
        MetricDraftToolRequest.Candidate count = generated(concept, table, "COUNT", null, time);
        count.setCode("candidate_" + concept.getId() + "_count");
        count.setName(MetricDraftConfirmation.text(concept.getName(), 150) + "记录数（候选）");
        result.add(count);
        for (PropertyDO property : properties) {
            if (result.size() == 5) break;
            if (!mappings.containsKey(property.getId()) || !MetricRules.numeric(property.getDataType())) continue;
            MetricDraftToolRequest.Candidate sum = generated(concept, table, "SUM", String.valueOf(property.getId()), time);
            sum.setCode("candidate_" + concept.getId() + "_sum_" + property.getId());
            sum.setName(MetricDraftConfirmation.text(property.getName(), 150) + "合计（候选）");
            result.add(sum);
        }
        return result;
    }

    private MetricDraftToolRequest.Candidate generated(ConceptDO concept, ConceptTableDO table, String aggregation,
                                                     String value, String time) {
        MetricDraftToolRequest.Candidate result = new MetricDraftToolRequest.Candidate();
        result.setAggregation(aggregation); result.setConceptTableId(String.valueOf(table.getId()));
        result.setValuePropertyId(value); result.setTimePropertyId(time);
        result.setGrain("unknown");
        result.setDescription("候选口径待业务确认；" + ("COUNT".equals(aggregation)
                ? "按绑定表记录计数COUNT(*)，不代表业务对象去重数量。" : "真实数值属性求和，不假设可加性或业务含义。")
                + "时间属性仅来自真实date/datetime映射；粒度unknown待补，自动名称仅供预览，未保存。");
        return result;
    }

    public Map<String, Object> validated(String ontologyId, String conceptId, MetricDraftToolRequest.Candidate input,
                                          AgentToolContext context) {
        ConceptDO concept = concept(ontologyId, conceptId, context);
        ConceptTableDO table = input.getConceptTableId() == null ? chooseTable(concept.getId(), context)
                : conceptTableMapper.selectById(support.positiveId(input.getConceptTableId()));
        DatasourceRespDTO datasource = eligible(table, concept.getId(), context);
        MetricDraftToolRequest.Candidate candidate = JSON.parseObject(JSON.toJSONString(input), MetricDraftToolRequest.Candidate.class);
        candidate.setConceptTableId(String.valueOf(table.getId()));
        candidate.setCode(MetricDraftConfirmation.text(candidate.getCode(), 100));
        candidate.setName(MetricDraftConfirmation.text(candidate.getName(), 200));
        candidate.setDescription(MetricDraftConfirmation.text(candidate.getDescription(), 4000));
        if (!MetricRules.notBlank(candidate.getDescription())) candidate.setDescription("候选口径待业务确认");
        else if (!candidate.getDescription().contains("候选口径待业务确认")) {
            candidate.setDescription(candidate.getDescription() + "；候选口径待业务确认");
        }
        candidate.setOwner(MetricDraftConfirmation.text(candidate.getOwner(), 200));
        candidate.setUnit(MetricDraftConfirmation.text(candidate.getUnit(), 100));
        candidate.setGrain(MetricDraftConfirmation.text(candidate.getGrain(), 1000));
        if (!MetricRules.notBlank(candidate.getGrain())) candidate.setGrain("unknown");
        String aggregation = candidate.getAggregation();
        if (!Arrays.asList("COUNT", "COUNT_DISTINCT", "SUM", "AVG", "MIN", "MAX").contains(aggregation)) {
            throw new IllegalArgumentException("不支持的候选聚合方式");
        }
        if (candidate.getDimensionPropertyIds() == null) candidate.setDimensionPropertyIds(new ArrayList<>());
        if (candidate.getDimensionPropertyIds().size() > 5
                || new HashSet<>(candidate.getDimensionPropertyIds()).size() != candidate.getDimensionPropertyIds().size()) {
            throw new IllegalArgumentException("维度最多5个且不能重复");
        }
        MetricSaveReqVO definition = definition(ontologyId, conceptId, candidate);
        MetricRules.normalize(definition);
        if (!"COUNT".equals(aggregation) && definition.getValuePropertyId() == null) {
            throw new IllegalArgumentException("聚合需要真实度量属性");
        }
        Map<Long, PropertyColumnDO> columns = columns(table);
        List<Map<String, Object>> used = new ArrayList<>();
        for (Long propertyId : MetricRules.usedProperties(definition)) {
            PropertyDO property = propertyMapper.selectById(propertyId);
            PropertyColumnDO column = columns.get(propertyId);
            if (property == null || !concept.getId().equals(property.getConceptId()) || column == null) {
                throw new IllegalArgumentException("属性必须属于该概念且唯一映射当前表的真实字段");
            }
            if (propertyId.equals(definition.getValuePropertyId()) && !"COUNT".equals(aggregation)
                    && !"COUNT_DISTINCT".equals(aggregation) && !MetricRules.numeric(property.getDataType())) {
                throw new IllegalArgumentException("度量属性必须为数值类型");
            }
            if (propertyId.equals(definition.getTimePropertyId()) && !temporal(property.getDataType())) {
                throw new IllegalArgumentException("时间属性必须为真实date/datetime属性");
            }
            Map<String, Object> mapping = new LinkedHashMap<>();
            mapping.put("propertyId", String.valueOf(propertyId)); mapping.put("propertyCode", property.getCode());
            mapping.put("propertyName", property.getName()); mapping.put("dataType", property.getDataType());
            mapping.put("columnName", column.getColumnName()); used.add(mapping);
        }
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("conceptTableId", String.valueOf(table.getId())); source.put("datasourceId", String.valueOf(table.getDatasourceId()));
        source.put("datasourceType", datasource.getDatasourceType()); source.put("databaseName", table.getDatabaseName());
        source.put("schemaName", table.getSchemaName()); source.put("tableName", table.getTableName()); source.put("propertyColumns", used);
        source.put("validationScope", "METADATA_ONLY_NOT_ACCESS_AUTHORIZATION");
        // Only whitelisted business fields and source metadata are serialized, never datasource DTO/credentials.
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("definition", JSON.parseObject(MetricDraftConfirmation.canonicalJson(candidate)));
        row.put("sourceMetadata", JSON.parseObject(MetricDraftConfirmation.canonicalJson(source)));
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("ontologyId", ontologyId); fingerprint.put("conceptId", conceptId);
        fingerprint.put("definition", row.get("definition")); fingerprint.put("sourceMetadata", row.get("sourceMetadata"));
        String canonical = MetricDraftConfirmation.canonicalJson(fingerprint);
        // Hash the audit-safe representation too, so sanitizer cannot silently change a confirmed definition.
        if (!canonical.equals(MetricDraftConfirmation.canonicalJson(
                com.datamaster.module.ai.agent.runtime.AgentPayloadSanitizer.sanitizeValue(null, JSON.parse(canonical))))) {
            throw new IllegalArgumentException("元数据包含需脱敏内容，不能作为稳定预览保存");
        }
        row.put("canonicalDefinition", canonical); row.put("candidateHash", MetricDraftConfirmation.hash(fingerprint));
        return row;
    }

    public MetricSaveReqVO definition(String ontologyId, String conceptId, MetricDraftToolRequest.Candidate candidate) {
        MetricSaveReqVO vo = new MetricSaveReqVO();
        vo.setOntologyId(support.positiveId(ontologyId)); vo.setConceptId(support.positiveId(conceptId));
        vo.setCode(candidate.getCode()); vo.setName(candidate.getName()); vo.setDescription(candidate.getDescription());
        vo.setOwner(candidate.getOwner()); vo.setUnit(candidate.getUnit()); vo.setGrain(candidate.getGrain());
        vo.setAggregation(candidate.getAggregation()); vo.setConceptTableId(support.positiveId(candidate.getConceptTableId()));
        vo.setValuePropertyId(candidate.getValuePropertyId() == null ? null : support.positiveId(candidate.getValuePropertyId()));
        vo.setTimePropertyId(candidate.getTimePropertyId() == null ? null : support.positiveId(candidate.getTimePropertyId()));
        List<Long> dimensions = new ArrayList<>();
        if (candidate.getDimensionPropertyIds() != null) for (String id : candidate.getDimensionPropertyIds()) dimensions.add(support.positiveId(id));
        vo.setDimensionPropertyIds(dimensions);
        return vo;
    }

    private ConceptTableDO chooseTable(Long conceptId, AgentToolContext context) {
        for (ConceptTableDO table : conceptTableMapper.selectByConceptId(conceptId)) {
            try { eligible(table, conceptId, context); return table; }
            catch (IllegalArgumentException ignored) { /* Try the next existing binding; never invent/create one. */ }
        }
        throw new IllegalArgumentException("需要已有、当前空间可见的Doris/ADS概念表绑定；不会自动创建绑定");
    }

    private DatasourceRespDTO eligible(ConceptTableDO table, Long conceptId, AgentToolContext context) {
        if (table == null || !conceptId.equals(table.getConceptId()) || table.getDatasourceId() == null
                || !MetricRules.notBlank(table.getDatabaseName()) || !MetricRules.adsSource(table.getDatabaseName(), table.getTableName())
                || !datasourceApiService.hasDatasourceSpace(table.getDatasourceId(), context.getSpaceId(), context.getSpaceCode())) {
            throw new IllegalArgumentException("需要当前空间真实Doris/ADS绑定；命名检查并非数据访问授权");
        }
        DatasourceRespDTO datasource = datasourceApiService.getDatasourceById(table.getDatasourceId());
        if (datasource == null || !"DORIS".equalsIgnoreCase(datasource.getDatasourceType())) {
            throw new IllegalArgumentException("需要Doris数据源元数据");
        }
        return datasource;
    }

    private Map<Long, PropertyColumnDO> columns(ConceptTableDO table) {
        Map<Long, PropertyColumnDO> result = new LinkedHashMap<>();
        Set<Long> ambiguous = new HashSet<>();
        for (PropertyColumnDO column : propertyColumnMapper.selectByConceptTableId(table.getId())) {
            if (!Objects.equals(column.getConceptTableId(), table.getId()) || column.getPropertyId() == null
                    || !MetricRules.notBlank(column.getColumnName())) continue;
            if (result.containsKey(column.getPropertyId())) ambiguous.add(column.getPropertyId());
            result.put(column.getPropertyId(), column);
        }
        for (Long id : ambiguous) result.remove(id);
        return result;
    }

    private boolean temporal(String type) { return "date".equalsIgnoreCase(type) || "datetime".equalsIgnoreCase(type); }
}
