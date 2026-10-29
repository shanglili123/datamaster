package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.Binding;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryRespVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IObjectInstanceQueryService;
import com.datamaster.module.ontology.service.impl.ActionWorkflowAccessGuard;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Definition-only preview. Never calls submission, a Function or a Webhook. */
@Component
public class DecisionBindingResolver {
    @Resource private ActionMapper actionMapper;
    @Resource private ActionWorkflowMapper workflowMapper;
    @Resource private ActionWorkflowNodeMapper nodeMapper;
    @Resource private ActionWorkflowEdgeMapper edgeMapper;
    @Resource private FunctionMapper functionMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private ConceptTableMapper tableMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private PropertyColumnMapper columnMapper;
    @Resource private RelationMapper relationMapper;
    @Resource private RelationTableMapper relationTableMapper;
    @Resource private RelationColumnMapper relationColumnMapper;
    @Resource private IAssetsTableGovernanceApiService governance;
    @Resource private IObjectInstanceQueryService objects;
    @Resource private ActionWorkflowAccessGuard workflowGuard;
    @Resource private com.datamaster.module.ontology.service.IActionApprovalService approvalService;

    public Binding resolve(Binding suggested, Long ontologyId, Long spaceId, String spaceCode) {
        if (suggested == null || StringUtils.isBlank(suggested.getTargetId())) {
            throw new ServiceException("候选尚未绑定已有动作或已发布 Workflow");
        }
        Binding binding = JSON.parseObject(JSON.toJSONString(suggested), Binding.class);
        JSONObject input = parseInput(binding.getInputParams());
        binding.setInputParams(canonical(input));
        List<Object> definitions = new ArrayList<>();
        Long targetId = positiveId(binding.getTargetId());
        if ("WORKFLOW".equalsIgnoreCase(binding.getType())) {
            binding.setType("WORKFLOW");
            ActionWorkflowDO workflow = workflowMapper.selectById(targetId);
            if (workflow == null || !Objects.equals(ontologyId, workflow.getOntologyId())
                    || !"PUBLISHED".equals(workflow.getStatus()) || !Boolean.TRUE.equals(workflow.getEnabled())) {
                throw new ServiceException("Workflow 不存在、未发布、未启用或不属于当前本体");
            }
            workflowGuard.assertWorkflowAccess(workflow);
            binding.setName(workflow.getName()); binding.setVersion(version(workflow.getVersion()));
            definitions.add(workflow);
            List<ActionWorkflowNodeDO> nodes = nodeMapper.selectByWorkflowId(targetId);
            definitions.add(nodes); definitions.add(edgeMapper.selectByWorkflowId(targetId));
            boolean approval = false;
            for (ActionWorkflowNodeDO node : nodes) {
                if (node.getActionId() != null) {
                    ActionDO action = requireAction(node.getActionId(), ontologyId);
                    collectAction(action, ontologyId, spaceId, spaceCode, definitions);
                    approval |= approval(action);
                }
                if (node.getCompensationActionId() != null) {
                    collectAction(requireAction(node.getCompensationActionId(), ontologyId), ontologyId,
                            spaceId, spaceCode, definitions);
                }
            }
            validateRequiredSchema(workflow.getInputSchema(), input);
            definitions.add(checkObject(workflow.getTriggerConceptId(), ontologyId, binding.getObjectKey(), spaceId, spaceCode));
            binding.setRequiresApproval(approval);
        } else if ("ACTION".equalsIgnoreCase(binding.getType()) || "COMPOSITE".equalsIgnoreCase(binding.getType())) {
            binding.setType("ACTION");
            ActionDO action = requireAction(targetId, ontologyId);
            if (("UPDATE".equals(action.getActionType()) || "DELETE".equals(action.getActionType()))
                    && StringUtils.isBlank(binding.getObjectKey()))
                throw new ServiceException("决策更新/删除必须明确业务对象主键，不允许隐式批量操作");
            binding.setName(action.getName()); binding.setVersion(version(action.getVersion()));
            binding.setRequiresApproval(approval(action));
            collectAction(action, ontologyId, spaceId, spaceCode, definitions);
            definitions.add(checkObject(action.getConceptId(), ontologyId, binding.getObjectKey(), spaceId, spaceCode));
            validateParameters(action.getParamConfig(), input, binding.getObjectKey());
            if ("COMPOSITE".equals(action.getActionType())) {
                for (Object step : JSON.parseArray(action.getExecutionSteps())) {
                    Object config = ((JSONObject) step).get("paramConfig");
                    validateParameters(config instanceof String ? (String) config : JSON.toJSONString(config), input, binding.getObjectKey());
                }
            }
            String criteria = approvalService.evaluateCriteria(action, binding.getInputParams(), binding.getObjectKey());
            Object checked = StringUtils.isBlank(criteria) ? null : JSON.parse(criteria);
            if (checked != null && (!(checked instanceof Map)
                    || (((Map<?, ?>) checked).containsKey("passed") && !Boolean.TRUE.equals(((Map<?, ?>) checked).get("passed")))))
                throw new ServiceException("动作前置条件未通过或无法确认，请检查当前业务状态");
        } else throw new ServiceException("只支持已有 ACTION/COMPOSITE 或已发布 WORKFLOW");
        binding.setDefinitionDigest(sha256(canonical(definitions)));
        return binding;
    }

    private ActionDO requireAction(Long id, Long ontologyId) {
        ActionDO action = actionMapper.selectById(id);
        if (action == null || !Objects.equals(action.getOntologyId(), ontologyId)) {
            throw new ServiceException("动作不存在或不属于当前本体");
        }
        return action;
    }

    private void collectAction(ActionDO action, Long ontologyId, Long spaceId, String spaceCode, List<Object> defs) {
        defs.add(action);
        collectConcept(action.getConceptId(), ontologyId, spaceId, spaceCode, defs);
        collectConcept(action.getSourceConceptId(), ontologyId, spaceId, spaceCode, defs);
        collectConcept(action.getOutputConceptId(), ontologyId, spaceId, spaceCode, defs);
        if (action.getFunctionId() != null) {
            FunctionDO function = functionMapper.selectById(action.getFunctionId());
            if (function == null || (function.getOntologyId() != null && !Objects.equals(ontologyId, function.getOntologyId()))) {
                throw new ServiceException("绑定函数不存在或不属于当前本体");
            }
            defs.add(function);
            collectConcept(function.getSourceConceptId(), ontologyId, spaceId, spaceCode, defs);
            collectConcept(function.getOutputConceptId(), ontologyId, spaceId, spaceCode, defs);
            if (StringUtils.isNotBlank(function.getSourceRelationIds())) {
                for (Object relationId : JSON.parseArray(function.getSourceRelationIds())) {
                    collectRelation(positiveId(String.valueOf(relationId)), ontologyId, spaceId, spaceCode, defs);
                }
            }
        }
        if ("COMPOSITE".equals(action.getActionType())) {
            JSONArray steps = JSON.parseArray(action.getExecutionSteps());
            if (steps == null || steps.isEmpty()) throw new ServiceException("复合动作缺少步骤");
            for (Object item : steps) {
                JSONObject step = (JSONObject) item;
                if (step.getLong("relationId") != null) {
                    collectRelation(step.getLong("relationId"), ontologyId, spaceId, spaceCode, defs);
                } else collectConcept(step.getLong("conceptId"), ontologyId, spaceId, spaceCode, defs);
            }
        }
        if (StringUtils.isNotBlank(action.getSourceRelationIds())) {
            for (Object relationId : JSON.parseArray(action.getSourceRelationIds())) {
                collectRelation(positiveId(String.valueOf(relationId)), ontologyId, spaceId, spaceCode, defs);
            }
        }
    }

    private void collectRelation(Long id, Long ontologyId, Long spaceId, String spaceCode, List<Object> defs) {
        RelationDO relation = relationMapper.selectById(id);
        if (relation == null || !Objects.equals(ontologyId, relation.getOntologyId())) {
            throw new ServiceException("关系不属于当前本体");
        }
        defs.add(relation);
        collectConcept(relation.getSourceConceptId(), ontologyId, spaceId, spaceCode, defs);
        collectConcept(relation.getTargetConceptId(), ontologyId, spaceId, spaceCode, defs);
        List<RelationTableDO> tables = relationTableMapper.selectByRelationId(id);
        defs.add(tables); defs.add(relationColumnMapper.selectByRelationId(id));
        for (RelationTableDO table : tables) checkTable(table.getDatasourceId(), table.getTableName(), spaceId, spaceCode);
    }

    private void collectConcept(Long id, Long ontologyId, Long spaceId, String spaceCode, List<Object> defs) {
        if (id == null) return;
        ConceptDO concept = conceptMapper.selectById(id);
        if (concept == null || !Objects.equals(ontologyId, concept.getOntologyId())) {
            throw new ServiceException("动作涉及的概念不属于当前本体");
        }
        defs.add(concept);
        List<ConceptTableDO> tables = tableMapper.selectByConceptId(id);
        defs.add(tables);
        defs.add(propertyMapper.selectList(new LambdaQueryWrapper<PropertyDO>()
                .eq(PropertyDO::getConceptId, id).orderByAsc(PropertyDO::getId)));
        for (ConceptTableDO table : tables) {
            checkTable(table.getDatasourceId(), table.getTableName(), spaceId, spaceCode);
            defs.add(columnMapper.selectList(new LambdaQueryWrapper<PropertyColumnDO>()
                    .eq(PropertyColumnDO::getConceptTableId, table.getId()).orderByAsc(PropertyColumnDO::getId)));
        }
    }

    private void checkTable(Long datasourceId, String tableName, Long spaceId, String spaceCode) {
        AssetsTableGovernanceReqDTO req = new AssetsTableGovernanceReqDTO();
        req.setDatasourceId(datasourceId); req.setTableName(tableName);
        req.setSpaceId(spaceId); req.setSpaceCode(spaceCode); req.setEntrance("ONTOLOGY_ACTION");
        governance.checkTableAccess(req);
    }

    private String checkObject(Long conceptId, Long ontologyId, String objectKey, Long spaceId, String spaceCode) {
        if (StringUtils.isBlank(objectKey)) return "NO_OBJECT"; // CREATE and context-free Functions may have no existing object.
        if (conceptId == null) throw new ServiceException("对象主键缺少目标概念，不能安全定位");
        ObjectInstanceQueryReqVO req = new ObjectInstanceQueryReqVO();
        req.setOntologyId(ontologyId); req.setConceptId(conceptId);
        req.setSpaceId(spaceId); req.setSpaceCode(spaceCode); req.setPageSize(1);
        // Query service supplies the authoritative semantic-to-physical mapping and governance checks.
        req.setFilters("{\"columns\":[]}");
        ObjectInstanceQueryRespVO meta = objects.queryObjects(req);
        List<com.datamaster.module.ontology.api.dto.SemanticPropertyDTO> keys = new ArrayList<>();
        if (meta.getProperties() != null) for (com.datamaster.module.ontology.api.dto.SemanticPropertyDTO p : meta.getProperties()) {
            if (Boolean.TRUE.equals(p.getIsPrimary())) keys.add(p);
        }
        if (keys.isEmpty()) throw new ServiceException("对象没有可验证的主键语义映射");
        JSONObject values = objectKey.trim().startsWith("{") ? JSON.parseObject(objectKey) : null;
        if (values == null && keys.size() != 1) throw new ServiceException("复合主键必须完整提供 JSON 对象");
        List<Map<String, Object>> filters = new ArrayList<>();
        for (com.datamaster.module.ontology.api.dto.SemanticPropertyDTO key : keys) {
            Object value = values == null ? objectKey : values.get(key.getPropertyCode());
            if (value == null && values != null) value = values.get(key.getPhysicalColumnName());
            if (value == null) throw new ServiceException("目标对象主键不完整");
            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put("field", key.getPhysicalColumnName()); filter.put("op", "eq"); filter.put("value", value);
            filters.add(filter);
        }
        Map<String, Object> group = new LinkedHashMap<>(); group.put("connector", "AND"); group.put("filters", filters);
        req.setFilters(JSON.toJSONString(Collections.singletonMap("groups", Collections.singletonList(group))));
        ObjectInstanceQueryRespVO result = objects.queryObjects(req);
        if (result.getRows() == null || result.getRows().size() != 1) throw new ServiceException("目标对象不存在或无权访问");
        return sha256(canonical(result.getRows().get(0)));
    }

    private void validateParameters(String configText, JSONObject input, String objectKey) {
        if (StringUtils.isBlank(configText) || "null".equals(configText)) return;
        JSONArray configs = JSON.parseArray(configText);
        if (configs == null) return;
        for (Object value : configs) {
            if (!(value instanceof JSONObject)) throw new ServiceException("动作参数定义无效");
            JSONObject config = (JSONObject) value;
            if (!Boolean.TRUE.equals(config.getBoolean("required"))) continue;
            String template = config.getString("valueTemplate");
            if (template == null) continue;
            String name = template.trim();
            if (!name.startsWith("${") || !name.endsWith("}")) continue;
            name = name.substring(2, name.length() - 1);
            boolean fromObject = name.startsWith("object.") || Boolean.TRUE.equals(config.getBoolean("objectValue"))
                    || (Boolean.TRUE.equals(config.getBoolean("condition")) && !"PARAM".equalsIgnoreCase(config.getString("conditionValueSource")));
            if (fromObject) {
                if (StringUtils.isBlank(objectKey)) throw new ServiceException("必填对象引用缺少业务对象主键");
            } else if (!input.containsKey(name) || input.get(name) == null) {
                throw new ServiceException("缺少必填执行参数：" + name);
            }
        }
    }

    private void validateRequiredSchema(String schemaText, JSONObject input) {
        if (StringUtils.isBlank(schemaText)) return;
        JSONObject schema = JSON.parseObject(schemaText);
        JSONArray required = schema.getJSONArray("required");
        if (required != null) for (Object key : required) {
            if (!input.containsKey(String.valueOf(key)) || input.get(String.valueOf(key)) == null) {
                throw new ServiceException("Workflow 缺少必填参数：" + key);
            }
        }
    }

    static JSONObject parseInput(String value) {
        try {
            Object parsed = JSON.parse(StringUtils.defaultIfBlank(value, "{}"));
            if (!(parsed instanceof JSONObject)) throw new ServiceException("执行参数必须为 JSON 对象");
            return (JSONObject) parsed;
        } catch (ServiceException e) { throw e; }
        catch (RuntimeException e) { throw new ServiceException("执行参数不是合法 JSON 对象"); }
    }
    static Long positiveId(String value) {
        try { Long id = Long.valueOf(value); if (id > 0) return id; }
        catch (RuntimeException ignored) { }
        throw new ServiceException("无效的资源 ID");
    }
    static String canonical(Object value) { return JSON.toJSONString(sort(JSON.toJSON(value))); }
    private static Object sort(Object value) {
        if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            ((Map<?, ?>) value).forEach((k, v) -> sorted.put(String.valueOf(k), sort(v))); return sorted;
        }
        if (value instanceof Collection) {
            List<Object> list = new ArrayList<>(); for (Object item : (Collection<?>) value) list.add(sort(item)); return list;
        }
        return value;
    }
    static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(); for (byte b : digest) result.append(String.format("%02x", b & 0xff));
            return result.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static int version(Integer value) { return value == null ? 1 : value; }
    private boolean approval(ActionDO action) {
        return Boolean.TRUE.equals(action.getNeedsApproval()) || (action.getApprovalLevels() != null && action.getApprovalLevels() > 0);
    }
}
