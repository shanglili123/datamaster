package com.datamaster.module.ontology.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.convert.ActionConvert;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.module.ontology.dal.dataobject.RelationColumnDO;
import com.datamaster.module.ontology.dal.dataobject.RelationTableDO;
import com.datamaster.module.ontology.dal.mapper.ActionExecutionMapper;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import com.datamaster.module.ontology.dal.mapper.RelationColumnMapper;
import com.datamaster.module.ontology.dal.mapper.RelationTableMapper;
import com.datamaster.module.ontology.service.IActionService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 动作定义 Service 实现
 */
@Service
@Validated
public class ActionServiceImpl implements IActionService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ActionMapper actionMapper;
    @Resource
    private ActionExecutionMapper executionMapper;
    @Resource
    private ConceptMapper conceptMapper;
    @Resource
    private ConceptTableMapper conceptTableMapper;
    @Resource
    private RelationMapper relationMapper;
    @Resource
    private RelationColumnMapper relationColumnMapper;
    @Resource
    private RelationTableMapper relationTableMapper;

    @Override
    public Long createAction(ActionSaveReqVO createReqVO) {
        normalizeApprovalConfig(createReqVO);
        validateActionBinding(createReqVO);
        ActionDO action = convertSaveRequest(createReqVO);
        action.setVersion(1);
        actionMapper.insert(action);
        return action.getId();
    }

    @Override
    public Integer updateAction(ActionSaveReqVO updateReqVO) {
        normalizeApprovalConfig(updateReqVO);
        validateActionBinding(updateReqVO);
        ActionDO existing = actionMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw new RuntimeException("动作不存在: " + updateReqVO.getId());
        }
        ActionDO action = convertSaveRequest(updateReqVO);
        action.setVersion((existing.getVersion() == null ? 1 : existing.getVersion()) + 1);
        LambdaUpdateWrapper<ActionDO> wrapper = new LambdaUpdateWrapper<ActionDO>()
                .eq(ActionDO::getId, updateReqVO.getId())
                // 触发来源属于运行时上下文；动作定义不再保存人工/数据到达选择。
                .set(ActionDO::getTriggerRef, null);
        if (existing.getVersion() == null) {
            wrapper.isNull(ActionDO::getVersion);
        } else {
            wrapper.eq(ActionDO::getVersion, existing.getVersion());
        }
        int updated = actionMapper.update(action, wrapper);
        if (updated == 0) {
            throw new RuntimeException("动作已被其他请求修改，请刷新后重试");
        }
        return updated;
    }

    /** 动作绑定校验：FUNCTION 绑定函数；数据动作绑定触发对象；多目标动作还必须包含执行步骤。 */
    private void validateActionBinding(ActionSaveReqVO reqVO) {
        if ("FUNCTION".equals(reqVO.getActionType())) {
            if (reqVO.getFunctionId() == null) {
                throw new RuntimeException("函数类型动作必须绑定共享函数");
            }
        } else if (reqVO.getConceptId() == null) {
            throw new RuntimeException("非函数类型动作必须选择触发对象类型");
        } else if ("COMPOSITE".equals(reqVO.getActionType())
                && (reqVO.getExecutionSteps() == null || reqVO.getExecutionSteps().trim().isEmpty())) {
            throw new RuntimeException("多目标动作必须配置至少一个执行步骤");
        } else if ("COMPOSITE".equals(reqVO.getActionType())) {
            try {
                JsonNode steps = objectMapper.readTree(reqVO.getExecutionSteps());
                if (!steps.isArray() || steps.size() == 0) {
                    throw new RuntimeException("多目标动作必须配置至少一个执行步骤");
                }
                Long datasourceId = null;
                for (int i = 0; i < steps.size(); i++) {
                    JsonNode step = steps.get(i);
                    if (!step.hasNonNull("actionType")) {
                        throw new RuntimeException("执行步骤 " + (i + 1) + " 缺少操作类型");
                    }
                    String type = step.path("actionType").asText("").toUpperCase();
                    if (!("CREATE".equals(type) || "UPDATE".equals(type) || "DELETE".equals(type))) {
                        throw new RuntimeException("执行步骤 " + (i + 1) + " 的操作类型不受支持: " + type);
                    }
                    JsonNode paramConfig = step.path("paramConfig");
                    int targetCount = 0;
                    int conditionCount = 0;
                    if (paramConfig.isArray()) {
                        for (JsonNode config : paramConfig) {
                            if (config.path("condition").asBoolean(false)) conditionCount++;
                            else if (config.hasNonNull("propertyCode")) targetCount++;
                        }
                    }
                    // relationId 是关系步骤的权威标识。兼容旧数据中 targetType 缺失或误存为 CONCEPT，
                    // 避免关系更新被普通对象校验误判为“至少需要一个条件”。
                    String targetType = step.hasNonNull("relationId")
                            ? "RELATION"
                            : step.path("targetType").asText("CONCEPT").toUpperCase();
                    Long currentDatasourceId;
                    if ("RELATION".equals(targetType)) {
                        if (!step.hasNonNull("relationId")) {
                            throw new RuntimeException("执行步骤 " + (i + 1) + " 缺少目标关系");
                        }
                        Long relationId = step.path("relationId").asLong();
                        RelationDO relation = relationMapper.selectById(relationId);
                        if (relation == null || !Objects.equals(relation.getOntologyId(), reqVO.getOntologyId())) {
                            throw new RuntimeException("执行步骤 " + (i + 1) + " 的目标关系不属于当前本体");
                        }
                        if ("UPDATE".equals(type)
                                && !Objects.equals(reqVO.getConceptId(), relation.getSourceConceptId())
                                && !Objects.equals(reqVO.getConceptId(), relation.getTargetConceptId())) {
                            throw new RuntimeException("执行步骤 " + (i + 1)
                                    + " 更新关系时，动作触发对象必须是该关系的主体或客体");
                        }
                        validateRelationEndpointValues(paramConfig, i + 1);
                        List<RelationTableDO> tables = relationTableMapper.selectByRelationId(relationId);
                        if (tables != null && !tables.isEmpty()) {
                            RelationTableDO relationTable = tables.get(0);
                            if (relationTable.getDatasourceId() == null) {
                                throw new RuntimeException("执行步骤 " + (i + 1) + " 的目标关系关联表缺少数据源");
                            }
                            validateRelationStepColumns(paramConfig, relationTable, i + 1);
                            currentDatasourceId = relationTable.getDatasourceId();
                        } else {
                            List<RelationColumnDO> bindings = relationColumnMapper.selectByRelationId(relationId);
                            if (bindings == null || bindings.isEmpty()) {
                                throw new RuntimeException("执行步骤 " + (i + 1) + " 的目标关系未配置字段映射");
                            }
                            if ("many_to_many".equalsIgnoreCase(relation.getRelationType())) {
                                throw new RuntimeException("执行步骤 " + (i + 1) + " 的多对多关系必须绑定关系表");
                            }
                            RelationColumnDO binding = bindings.get(0);
                            ConceptTableDO sourceTable = binding.getSourceConceptTableId() == null
                                    ? null : conceptTableMapper.selectById(binding.getSourceConceptTableId());
                            ConceptTableDO targetTable = binding.getTargetConceptTableId() == null
                                    ? null : conceptTableMapper.selectById(binding.getTargetConceptTableId());
                            if (sourceTable == null || targetTable == null
                                    || sourceTable.getDatasourceId() == null || targetTable.getDatasourceId() == null) {
                                throw new RuntimeException("执行步骤 " + (i + 1) + " 的关系两端未完成数据源绑定");
                            }
                            if (!Objects.equals(sourceTable.getDatasourceId(), targetTable.getDatasourceId())) {
                                throw new RuntimeException("执行步骤 " + (i + 1) + " 的直接外键关系两端必须位于同一数据源");
                            }
                            if (binding.getSourceColumn() == null || binding.getSourceColumn().trim().isEmpty()
                                    || binding.getTargetColumn() == null || binding.getTargetColumn().trim().isEmpty()) {
                                throw new RuntimeException("执行步骤 " + (i + 1) + " 的关系字段映射不完整");
                            }
                            for (JsonNode config : paramConfig) {
                                if (!config.hasNonNull("propertyCode") || config.hasNonNull("relationEndpoint")) continue;
                                throw new RuntimeException("执行步骤 " + (i + 1) + " 使用直接外键关系，不能配置关系表属性");
                            }
                            currentDatasourceId = sourceTable.getDatasourceId();
                        }
                    } else if ("CONCEPT".equals(targetType)) {
                        if (("CREATE".equals(type) || "UPDATE".equals(type)) && targetCount == 0) {
                            throw new RuntimeException("执行步骤 " + (i + 1) + " 至少需要一个目标属性");
                        }
                        if (("UPDATE".equals(type) || "DELETE".equals(type)) && conditionCount == 0) {
                            throw new RuntimeException("执行步骤 " + (i + 1) + " 至少需要一个条件");
                        }
                        if (!step.hasNonNull("conceptId")) {
                            throw new RuntimeException("执行步骤 " + (i + 1) + " 缺少目标对象类型");
                        }
                        Long conceptId = step.path("conceptId").asLong();
                        ConceptDO concept = conceptMapper.selectById(conceptId);
                        if (concept == null || !Objects.equals(concept.getOntologyId(), reqVO.getOntologyId())) {
                            throw new RuntimeException("执行步骤 " + (i + 1) + " 的目标对象类型不属于当前本体");
                        }
                        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
                        if (tables == null || tables.isEmpty() || tables.get(0).getDatasourceId() == null) {
                            throw new RuntimeException("执行步骤 " + (i + 1) + " 的目标对象类型未完成数据源绑定");
                        }
                        currentDatasourceId = tables.get(0).getDatasourceId();
                    } else {
                        throw new RuntimeException("执行步骤 " + (i + 1) + " 的目标类型不受支持: " + targetType);
                    }
                    if (datasourceId == null) {
                        datasourceId = currentDatasourceId;
                    } else if (!datasourceId.equals(currentDatasourceId)) {
                        throw new RuntimeException("多目标动作的所有执行步骤必须位于同一数据源");
                    }
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("多目标动作执行步骤格式错误: " + e.getMessage(), e);
            }
        }
    }

    private void validateRelationStepColumns(JsonNode paramConfig, RelationTableDO table, int stepNo) {
        Set<String> writableColumns = new LinkedHashSet<>();
        String sourceColumn = null;
        String targetColumn = null;
        try {
            JsonNode configured = objectMapper.readTree(table.getColumnNames() == null ? "[]" : table.getColumnNames());
            if (configured.isArray()) {
                for (JsonNode column : configured) {
                    if (column.isTextual() && !column.asText().trim().isEmpty()) writableColumns.add(column.asText());
                }
                if (configured.size() > 0) sourceColumn = configured.get(0).asText(null);
                if (configured.size() > 1) targetColumn = configured.get(1).asText(null);
            } else if (configured.isObject()) {
                sourceColumn = configured.path("sourceColumn").asText(null);
                targetColumn = configured.path("targetColumn").asText(null);
                if (sourceColumn != null) writableColumns.add(sourceColumn);
                if (targetColumn != null) writableColumns.add(targetColumn);
                JsonNode attributes = configured.has("attributeColumns")
                        ? configured.path("attributeColumns") : configured.path("columns");
                if (attributes.isArray()) {
                    for (JsonNode column : attributes) {
                        if (column.isTextual() && !column.asText().trim().isEmpty()) writableColumns.add(column.asText());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("执行步骤 " + stepNo + " 的关系字段配置格式错误: " + e.getMessage());
        }
        if (sourceColumn == null || targetColumn == null) {
            throw new RuntimeException("执行步骤 " + stepNo + " 的目标关系尚未配置主体外键列和客体外键列");
        }
        if (!paramConfig.isArray()) return;
        for (JsonNode config : paramConfig) {
            String propertyCode = config.path("propertyCode").asText(null);
            String endpoint = config.path("relationEndpoint").asText("").toUpperCase();
            if ("SOURCE".equals(endpoint) || "TARGET".equals(endpoint)) {
                continue;
            }
            if (propertyCode != null && !writableColumns.contains(propertyCode)) {
                throw new RuntimeException("执行步骤 " + stepNo + " 的关系字段不存在或不可写: " + propertyCode);
            }
        }
    }

    private void validateRelationEndpointValues(JsonNode paramConfig, int stepNo) {
        boolean source = false;
        boolean target = false;
        if (paramConfig.isArray()) {
            for (JsonNode config : paramConfig) {
                String endpoint = config.path("relationEndpoint").asText("").toUpperCase();
                if ("SOURCE".equals(endpoint)) source = true;
                if ("TARGET".equals(endpoint)) target = true;
            }
        }
        if (!source || !target) {
            throw new RuntimeException("执行步骤 " + stepNo + " 必须同时配置主体值和客体值");
        }
    }

    /**
     * 当前动作确认模型固定为 0 或 1 次人工确认。
     * 触发来源（人工、数据到达、API、工作流）不改变该配置。
     */
    private void normalizeApprovalConfig(ActionSaveReqVO reqVO) {
        reqVO.setTriggerRef(null);
        boolean needsApproval = Boolean.TRUE.equals(reqVO.getNeedsApproval())
                || (reqVO.getApprovalLevels() != null && reqVO.getApprovalLevels() > 0);
        reqVO.setNeedsApproval(needsApproval);
        reqVO.setApprovalLevels(needsApproval ? 1 : 0);
        reqVO.setApprovalReviewers(needsApproval
                ? normalizeSingleReviewer(reqVO.getApprovalReviewers()) : null);
    }

    private String normalizeSingleReviewer(String reviewersJson) {
        if (reviewersJson == null || reviewersJson.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(reviewersJson);
            if (!root.isArray() || root.size() == 0) {
                return null;
            }
            JsonNode selected = null;
            for (JsonNode node : root) {
                if (node.path("stage").asInt(1) == 1) {
                    selected = node;
                    break;
                }
            }
            if (selected == null) {
                selected = root.get(0);
            }
            if (selected == null || !selected.hasNonNull("userId")) {
                return null;
            }
            ObjectNode reviewer = objectMapper.createObjectNode();
            reviewer.put("stage", 1);
            if (selected.get("userId").isNumber()) {
                reviewer.put("userId", selected.get("userId").longValue());
            } else {
                reviewer.put("userId", selected.get("userId").asText());
            }
            if (selected.hasNonNull("userName")) {
                reviewer.put("userName", selected.get("userName").asText());
            }
            ArrayNode result = objectMapper.createArrayNode();
            result.add(reviewer);
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("审批人配置格式错误: " + e.getMessage(), e);
        }
    }

    @Override
    public Integer deleteAction(Long id) {
        // 防误删：动作已有执行/审批记录（含行操作内置动作）时拒绝物理删除，
        // 历史记录/审计/血缘依赖该定义，需保留；确需清理可先清理 ONT_ACTION_EXECUTION。
        Long execCount = executionMapper.selectCount(new LambdaQueryWrapperX<ActionExecutionDO>()
                .eq(ActionExecutionDO::getActionId, id));
        if (execCount != null && execCount > 0) {
            throw new RuntimeException("动作已产生 " + execCount
                    + " 条执行记录，为保障历史审计/血缘不可删除；如需下线请调整动作状态而非删除");
        }
        return actionMapper.deleteById(id);
    }

    @Override
    public ActionRespVO getActionById(Long id) {
        ActionDO action = actionMapper.selectById(id);
        return convertResponse(action);
    }

    @Override
    public PageResult<ActionRespVO> getActionPage(ActionPageReqVO pageReqVO) {
        PageResult<ActionDO> pageResult = actionMapper.selectPage(pageReqVO);
        return new PageResult<>(convertResponseList(pageResult.getRows()), pageResult.getTotal());
    }

    @Override
    public List<ActionRespVO> getActionsByOntologyId(Long ontologyId) {
        List<ActionDO> list = actionMapper.selectByOntologyId(ontologyId);
        return convertResponseList(list);
    }

    /**
     * executionSteps 是后加字段。显式赋值可兼容 IDE 增量编译时尚未重新生成的 MapStruct
     * ActionConvertImpl，避免校验通过但 INSERT/UPDATE 丢失多目标步骤。
     */
    private ActionDO convertSaveRequest(ActionSaveReqVO reqVO) {
        ActionDO action = ActionConvert.INSTANCE.convert(reqVO);
        action.setExecutionSteps(reqVO.getExecutionSteps());
        return action;
    }

    /** 查询响应同样显式回填，避免对象浏览器和修改弹窗把已有步骤误判为空。 */
    private ActionRespVO convertResponse(ActionDO action) {
        if (action == null) {
            return null;
        }
        ActionRespVO response = ActionConvert.INSTANCE.convert(action);
        response.setExecutionSteps(action.getExecutionSteps());
        return response;
    }

    private List<ActionRespVO> convertResponseList(List<ActionDO> actions) {
        if (actions == null) {
            return java.util.Collections.emptyList();
        }
        return actions.stream().map(this::convertResponse).collect(java.util.stream.Collectors.toList());
    }
}
