package com.datamaster.module.ontology.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.workflow.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IActionWorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActionWorkflowServiceImpl implements IActionWorkflowService {
    private static final Set<String> NODE_TYPES = new HashSet<>(Arrays.asList(
            "START", "ACTION", "CONDITION", "PARALLEL_SPLIT", "PARALLEL_JOIN", "END"));
    private static final Set<String> FAILURE_POLICIES = new HashSet<>(Arrays.asList("STOP", "MANUAL", "RETRY"));
    private static final Set<String> CONDITION_OPERATORS = new HashSet<>(Arrays.asList(
            "EQ", "NE", "GT", "GE", "LT", "LE", "CONTAINS", "IN", "IS_EMPTY", "NOT_EMPTY"));
    private static final Set<String> REFERENCE_SOURCES = new HashSet<>(Arrays.asList(
            "OBJECT", "INPUT", "STEP_OUTPUT", "CONTEXT"));

    @Resource private ActionWorkflowMapper workflowMapper;
    @Resource private ActionWorkflowNodeMapper nodeMapper;
    @Resource private ActionWorkflowEdgeMapper edgeMapper;
    @Resource private ActionMapper actionMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private ActionWorkflowVersionMapper versionMapper;
    @Resource private ActionWorkflowAccessGuard accessGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkflow(ActionWorkflowSaveReqVO reqVO) {
        accessGuard.assertOntologyAccess(reqVO.getOntologyId());
        normalizeWorkflow(reqVO);
        validateInputSchema(reqVO.getInputSchema());
        validateOutputSchema(reqVO.getOutputSchema());
        reqVO.setTriggerConceptId(resolveTriggerConceptId(reqVO.getOntologyId(), reqVO.getNodes()));
        if (workflowMapper.selectByCode(reqVO.getOntologyId(), reqVO.getCode()) != null) {
            throw new RuntimeException("编排编码已存在: " + reqVO.getCode());
        }
        validateTriggerConcept(reqVO.getOntologyId(), reqVO.getTriggerConceptId());
        validateGraph(reqVO.getOntologyId(), reqVO.getTriggerConceptId(), reqVO.getNodes(), reqVO.getEdges());
        ActionWorkflowDO workflow = toWorkflow(reqVO);
        workflow.setVersion(1);
        workflow.setStatus("DRAFT");
        workflow.setEnabled(false);
        workflowMapper.insert(workflow);
        replaceGraph(workflow.getId(), reqVO.getNodes(), reqVO.getEdges());
        return workflow.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateWorkflow(ActionWorkflowSaveReqVO reqVO) {
        if (reqVO.getId() == null) throw new RuntimeException("编排ID不能为空");
        ActionWorkflowDO existing = requireWorkflow(reqVO.getId());
        accessGuard.assertWorkflowAccess(existing);
        if (!Objects.equals(existing.getOntologyId(), reqVO.getOntologyId())) {
            throw new RuntimeException("不能修改动作编排所属的本体");
        }
        if (!StringUtils.hasText(reqVO.getCode())) reqVO.setCode(existing.getCode());
        normalizeWorkflow(reqVO);
        validateInputSchema(reqVO.getInputSchema());
        validateOutputSchema(reqVO.getOutputSchema());
        reqVO.setTriggerConceptId(resolveTriggerConceptId(reqVO.getOntologyId(), reqVO.getNodes()));
        ActionWorkflowDO sameCode = workflowMapper.selectByCode(reqVO.getOntologyId(), reqVO.getCode());
        if (sameCode != null && !sameCode.getId().equals(reqVO.getId())) {
            throw new RuntimeException("编排编码已存在: " + reqVO.getCode());
        }
        validateTriggerConcept(reqVO.getOntologyId(), reqVO.getTriggerConceptId());
        validateGraph(reqVO.getOntologyId(), reqVO.getTriggerConceptId(), reqVO.getNodes(), reqVO.getEdges());
        ActionWorkflowDO workflow = toWorkflow(reqVO);
        int currentVersion = existing.getVersion() == null ? 1 : existing.getVersion();
        workflow.setVersion("PUBLISHED".equals(existing.getStatus()) ? currentVersion + 1 : currentVersion);
        workflow.setStatus("DRAFT");
        workflow.setEnabled(false);
        int updated = workflowMapper.updateById(workflow);
        replaceGraph(reqVO.getId(), reqVO.getNodes(), reqVO.getEdges());
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteWorkflow(Long id) {
        ActionWorkflowDO workflow = requireWorkflow(id);
        accessGuard.assertWorkflowAccess(workflow);
        if (Boolean.TRUE.equals(workflow.getEnabled())) throw new RuntimeException("已发布编排请先停用后再删除");
        edgeMapper.deleteByWorkflowId(id);
        nodeMapper.deleteByWorkflowId(id);
        versionMapper.deleteByWorkflowId(id);
        return workflowMapper.deleteById(id);
    }

    @Override
    public ActionWorkflowRespVO getWorkflow(Long id) {
        ActionWorkflowDO workflow = requireWorkflow(id);
        accessGuard.assertWorkflowAccess(workflow);
        return toResponse(workflow, true);
    }

    @Override
    public ActionWorkflowRespVO getWorkflowVersion(Long id, Integer version) {
        ActionWorkflowDO workflow = requireWorkflow(id);
        accessGuard.assertWorkflowAccess(workflow);
        ActionWorkflowVersionDO snapshot = requireWorkflowVersion(id, version);
        try {
            ActionWorkflowRespVO result = JSON.parseObject(snapshot.getSnapshotJson(), ActionWorkflowRespVO.class);
            if (result == null) throw new RuntimeException("Workflow 版本快照为空");
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Workflow 版本快照无法读取: " + version, e);
        }
    }

    @Override
    public List<ActionWorkflowVersionRespVO> listWorkflowVersions(Long id) {
        ActionWorkflowDO workflow = requireWorkflow(id);
        accessGuard.assertWorkflowAccess(workflow);
        return versionMapper.selectByWorkflowId(id).stream().map(this::toVersionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackWorkflow(Long id, Integer version) {
        ActionWorkflowDO workflow = requireWorkflow(id);
        accessGuard.assertWorkflowAccess(workflow);
        ActionWorkflowRespVO snapshot = getWorkflowVersion(id, version);
        ActionWorkflowSaveReqVO request = new ActionWorkflowSaveReqVO();
        request.setId(id);
        request.setOntologyId(snapshot.getOntologyId());
        request.setCode(snapshot.getCode());
        request.setName(snapshot.getName());
        request.setTriggerConceptId(snapshot.getTriggerConceptId());
        request.setTriggerRef(snapshot.getTriggerRef());
        request.setInputSchema(snapshot.getInputSchema());
        request.setOutputSchema(snapshot.getOutputSchema());
        request.setFailurePolicy(snapshot.getFailurePolicy());
        request.setDescription(snapshot.getDescription());
        request.setNodes(snapshot.getNodes());
        request.setEdges(snapshot.getEdges());
        updateWorkflow(request);
    }

    @Override
    public PageResult<ActionWorkflowRespVO> getWorkflowPage(ActionWorkflowPageReqVO reqVO) {
        // 普通用户必须带本体上下文，守卫会校验该本体所属空间；只有全局运维角色允许跨本体查询。
        if (reqVO == null || reqVO.getOntologyId() == null) {
            accessGuard.assertOntologyAccess(null);
        } else {
            accessGuard.assertOntologyAccess(reqVO.getOntologyId());
        }
        PageResult<ActionWorkflowDO> page = workflowMapper.selectPage(reqVO);
        List<ActionWorkflowRespVO> rows = page.getRows().stream()
                .map(item -> toResponse(item, false)).collect(Collectors.toList());
        return new PageResult<>(rows, page.getTotal());
    }

    @Override
    public void validateWorkflow(Long id) {
        ActionWorkflowDO workflow = requireWorkflow(id);
        accessGuard.assertWorkflowAccess(workflow);
        validateInputSchema(workflow.getInputSchema());
        validateOutputSchema(workflow.getOutputSchema());
        List<ActionWorkflowNodeVO> nodes = toNodeVOs(nodeMapper.selectByWorkflowId(id));
        Long triggerConceptId = resolveTriggerConceptId(workflow.getOntologyId(), nodes);
        validateTriggerConcept(workflow.getOntologyId(), triggerConceptId);
        validateGraph(workflow.getOntologyId(), triggerConceptId, nodes, toEdgeVOs(edgeMapper.selectByWorkflowId(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishWorkflow(Long id) {
        ActionWorkflowDO workflow = requireWorkflow(id);
        accessGuard.assertWorkflowAccess(workflow);
        validateWorkflow(id);
        if ("PUBLISHED".equals(workflow.getStatus()) && Boolean.TRUE.equals(workflow.getEnabled())) {
            return;
        }
        if (workflow.getVersion() == null) workflow.setVersion(1);
        workflow.setStatus("PUBLISHED");
        workflow.setEnabled(true);
        workflowMapper.updateById(workflow);
        savePublishedVersionSnapshot(workflow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableWorkflow(Long id) {
        ActionWorkflowDO workflow = requireWorkflow(id);
        accessGuard.assertWorkflowAccess(workflow);
        workflow.setEnabled(false);
        workflowMapper.updateById(workflow);
    }

    private ActionWorkflowVersionDO requireWorkflowVersion(Long workflowId, Integer version) {
        if (version == null || version < 1) throw new RuntimeException("Workflow 版本号必须是正整数");
        ActionWorkflowVersionDO snapshot = versionMapper.selectByWorkflowIdAndVersion(workflowId, version);
        if (snapshot == null) throw new RuntimeException("Workflow 版本不存在: " + version);
        return snapshot;
    }

    private void savePublishedVersionSnapshot(ActionWorkflowDO workflow) {
        if (workflow == null || workflow.getId() == null || workflow.getVersion() == null) return;
        ActionWorkflowVersionDO snapshot = versionMapper.selectByWorkflowIdAndVersion(
                workflow.getId(), workflow.getVersion());
        if (snapshot == null) {
            snapshot = new ActionWorkflowVersionDO();
            snapshot.setWorkflowId(workflow.getId());
            snapshot.setVersion(workflow.getVersion());
        }
        snapshot.setStatus(workflow.getStatus());
        snapshot.setSnapshotJson(JSON.toJSONString(toResponse(workflow, true)));
        if (snapshot.getId() == null) versionMapper.insert(snapshot);
        else versionMapper.updateById(snapshot);
    }

    private ActionWorkflowVersionRespVO toVersionResponse(ActionWorkflowVersionDO data) {
        ActionWorkflowVersionRespVO vo = new ActionWorkflowVersionRespVO();
        vo.setId(data.getId());
        vo.setWorkflowId(data.getWorkflowId());
        vo.setVersion(data.getVersion());
        vo.setStatus(data.getStatus());
        vo.setCreateTime(data.getCreateTime());
        vo.setCreatorId(data.getCreatorId());
        vo.setCreateBy(data.getCreateBy());
        return vo;
    }

    private void replaceGraph(Long workflowId, List<ActionWorkflowNodeVO> nodes, List<ActionWorkflowEdgeVO> edges) {
        edgeMapper.deleteByWorkflowId(workflowId);
        nodeMapper.deleteByWorkflowId(workflowId);
        for (ActionWorkflowNodeVO node : safe(nodes)) {
            ActionWorkflowNodeDO data = ActionWorkflowNodeDO.builder()
                    .workflowId(workflowId).nodeKey(node.getNodeKey()).name(node.getName())
                    .nodeType(node.getNodeType()).actionId(node.getActionId()).configJson(node.getConfigJson())
                    .timeoutMs(node.getTimeoutMs()).retryPolicy(node.getRetryPolicy())
                    .compensationActionId(node.getCompensationActionId())
                    .positionX(node.getPositionX()).positionY(node.getPositionY()).build();
            nodeMapper.insert(data);
        }
        int index = 0;
        for (ActionWorkflowEdgeVO edge : safe(edges)) {
            index++;
            ActionWorkflowEdgeDO data = ActionWorkflowEdgeDO.builder()
                    .workflowId(workflowId)
                    .edgeKey(StringUtils.hasText(edge.getEdgeKey()) ? edge.getEdgeKey() : "edge_" + index)
                    .fromNodeKey(edge.getFromNodeKey()).toNodeKey(edge.getToNodeKey())
                    .conditionExpr(edge.getConditionExpr()).priority(edge.getPriority() == null ? index : edge.getPriority())
                    .build();
            edgeMapper.insert(data);
        }
    }

    private void validateGraph(Long ontologyId, Long triggerConceptId,
                               List<ActionWorkflowNodeVO> rawNodes, List<ActionWorkflowEdgeVO> rawEdges) {
        List<ActionWorkflowNodeVO> nodes = safe(rawNodes);
        List<ActionWorkflowEdgeVO> edges = safe(rawEdges);
        if (nodes.isEmpty()) throw new RuntimeException("编排至少需要开始和结束节点");
        Map<String, ActionWorkflowNodeVO> byKey = new LinkedHashMap<>();
        int starts = 0, ends = 0, actions = 0;
        String startKey = null;
        for (ActionWorkflowNodeVO node : nodes) {
            if (!StringUtils.hasText(node.getNodeKey())) throw new RuntimeException("节点编码不能为空");
            node.setNodeKey(node.getNodeKey().trim());
            if (!node.getNodeKey().matches("[A-Za-z0-9_-]+")) {
                throw new RuntimeException("节点编码只能包含字母、数字、下划线和短横线: " + node.getNodeKey());
            }
            if (!StringUtils.hasText(node.getName())) throw new RuntimeException("节点名称不能为空: " + node.getNodeKey());
            node.setName(node.getName().trim());
            if (byKey.put(node.getNodeKey(), node) != null) throw new RuntimeException("节点编码重复: " + node.getNodeKey());
            String type = String.valueOf(node.getNodeType()).toUpperCase(Locale.ROOT);
            node.setNodeType(type);
            if (!NODE_TYPES.contains(type)) throw new RuntimeException("不支持的节点类型: " + type);
            if ("START".equals(type)) {
                starts++;
                startKey = node.getNodeKey();
                ActionDO triggerAction = requireNodeAction(ontologyId, node, "开始触发节点");
                Long actionConceptId = resolveActionConceptId(triggerAction);
                if (actionConceptId == null && !"WEBHOOK".equalsIgnoreCase(triggerAction.getActionType())) {
                    throw new RuntimeException("开始触发动作没有明确的对象概念: " + triggerAction.getName());
                }
                if (triggerConceptId != null && !Objects.equals(actionConceptId, triggerConceptId)) {
                    throw new RuntimeException("开始触发动作与 Workflow 触发概念不一致");
                }
            }
            if ("END".equals(type)) ends++;
            if ("ACTION".equals(type)) {
                actions++;
                requireNodeAction(ontologyId, node, "动作节点");
                validateCompensationAction(ontologyId, node);
                validateNodeExecutionPolicy(node);
            }
        }
        if (starts != 1) throw new RuntimeException("编排必须且只能有一个开始节点");
        if (ends != 1) throw new RuntimeException("编排必须且只能有一个结束守护节点");
        if (actions < 1) throw new RuntimeException("编排至少需要一个动作节点");

        Map<String, Integer> indegree = new HashMap<>();
        Map<String, Integer> outdegree = new HashMap<>();
        Map<String, List<String>> next = new HashMap<>();
        Map<String, List<String>> previous = new HashMap<>();
        Map<String, List<ActionWorkflowEdgeVO>> outgoingEdges = new HashMap<>();
        byKey.keySet().forEach(key -> { indegree.put(key, 0); outdegree.put(key, 0); next.put(key, new ArrayList<>()); previous.put(key, new ArrayList<>()); outgoingEdges.put(key, new ArrayList<>()); });
        Set<String> routes = new HashSet<>();
        Set<String> edgeKeys = new HashSet<>();
        for (ActionWorkflowEdgeVO edge : edges) {
            if (!StringUtils.hasText(edge.getFromNodeKey()) || !StringUtils.hasText(edge.getToNodeKey())) {
                throw new RuntimeException("连线的起点和终点不能为空");
            }
            edge.setFromNodeKey(edge.getFromNodeKey().trim());
            edge.setToNodeKey(edge.getToNodeKey().trim());
            if (StringUtils.hasText(edge.getEdgeKey())) edge.setEdgeKey(edge.getEdgeKey().trim());
            if (!byKey.containsKey(edge.getFromNodeKey()) || !byKey.containsKey(edge.getToNodeKey())) {
                throw new RuntimeException("连线引用了不存在的节点");
            }
            if (edge.getFromNodeKey().equals(edge.getToNodeKey())) throw new RuntimeException("节点不能连接自身");
            String unique = edge.getFromNodeKey() + "->" + edge.getToNodeKey();
            if (!routes.add(unique)) throw new RuntimeException("存在重复连线: " + unique);
            if (StringUtils.hasText(edge.getEdgeKey()) && !edgeKeys.add(edge.getEdgeKey())) {
                throw new RuntimeException("连线编码重复: " + edge.getEdgeKey());
            }
            ActionWorkflowNodeVO source = byKey.get(edge.getFromNodeKey());
            if (StringUtils.hasText(edge.getConditionExpr()) && !"CONDITION".equals(source.getNodeType())) {
                throw new RuntimeException("只有条件节点的出边可以配置条件表达式: " + edge.getFromNodeKey());
            }
            next.get(edge.getFromNodeKey()).add(edge.getToNodeKey());
            previous.get(edge.getToNodeKey()).add(edge.getFromNodeKey());
            outgoingEdges.get(edge.getFromNodeKey()).add(edge);
            outdegree.put(edge.getFromNodeKey(), outdegree.get(edge.getFromNodeKey()) + 1);
            indegree.put(edge.getToNodeKey(), indegree.get(edge.getToNodeKey()) + 1);
        }
        for (ActionWorkflowNodeVO node : nodes) {
            String nodeKey = node.getNodeKey();
            String nodeType = node.getNodeType();
            int in = indegree.get(nodeKey);
            int out = outdegree.get(nodeKey);
            if ("START".equals(nodeType)) {
                if (in != 0) throw new RuntimeException("开始节点不能有入边");
                if (out != 1) throw new RuntimeException("开始节点必须且只能有一条出边");
            } else if ("END".equals(nodeType)) {
                if (out != 0) throw new RuntimeException("结束节点不能有出边");
                if (in != 1) throw new RuntimeException("每个结束节点必须且只能有一条入边: " + nodeKey);
            } else if ("PARALLEL_JOIN".equals(nodeType)) {
                if (in < 2) throw new RuntimeException("并行汇聚节点至少需要两条入边: " + nodeKey);
                if (out != 1) throw new RuntimeException("并行汇聚节点必须且只能有一条出边: " + nodeKey);
                validateParallelJoinPolicy(node);
            } else {
                if (in != 1) throw new RuntimeException("节点必须且只能有一条入边: " + nodeKey);
                if (("ACTION".equals(nodeType)) && out != 1) {
                    throw new RuntimeException("动作节点必须且只能有一条出边: " + nodeKey);
                }
            }
            if ("CONDITION".equals(nodeType)) {
                List<ActionWorkflowEdgeVO> branches = outgoingEdges.get(node.getNodeKey());
                if (branches.size() < 2) throw new RuntimeException("条件节点至少需要两条分支: " + node.getNodeKey());
                long defaults = branches.stream().filter(edge -> !StringUtils.hasText(edge.getConditionExpr())).count();
                if (defaults > 1) throw new RuntimeException("条件节点最多只能有一条默认分支: " + node.getNodeKey());
                if (defaults == branches.size()) throw new RuntimeException("条件节点至少需要一条带表达式的分支: " + node.getNodeKey());
                Set<String> upstreamNodeKeys = collectPredecessors(node.getNodeKey(), previous);
                branches.stream().filter(edge -> StringUtils.hasText(edge.getConditionExpr()))
                        .forEach(edge -> validateConditionExpression(edge.getConditionExpr(), triggerConceptId, byKey, upstreamNodeKeys));
            }
            if ("PARALLEL_SPLIT".equals(nodeType) && out < 2) throw new RuntimeException("并行拆分节点至少需要两条出边: " + nodeKey);
        }
        Queue<String> queue = new ArrayDeque<>();
        indegree.forEach((key, value) -> { if (value == 0) queue.add(key); });
        int visited = 0;
        while (!queue.isEmpty()) {
            String key = queue.remove(); visited++;
            for (String target : next.get(key)) {
                int value = indegree.get(target) - 1;
                indegree.put(target, value);
                if (value == 0) queue.add(target);
            }
        }
        if (visited != nodes.size()) throw new RuntimeException("编排存在环，第一版只允许 DAG");
        Set<String> reachable = new HashSet<>();
        Queue<String> walk = new ArrayDeque<>();
        walk.add(startKey);
        while (!walk.isEmpty()) {
            String key = walk.remove();
            if (!reachable.add(key)) continue;
            walk.addAll(next.get(key));
        }
        if (reachable.size() != nodes.size()) throw new RuntimeException("存在从开始节点不可达的节点");
    }

    private void validateNodeExecutionPolicy(ActionWorkflowNodeVO node) {
        if (node.getTimeoutMs() != null && (node.getTimeoutMs() < 0 || node.getTimeoutMs() > 86400000)) {
            throw new RuntimeException("动作节点超时时间必须在 0 到 86400000 毫秒之间: " + node.getNodeKey());
        }
        if (!StringUtils.hasText(node.getRetryPolicy())) return;
        try {
            JSONObject json = JSON.parseObject(node.getRetryPolicy());
            Integer maxAttempts = json.getInteger("maxAttempts");
            if (maxAttempts == null && json.getInteger("maxRetries") != null) {
                maxAttempts = json.getInteger("maxRetries") + 1;
            }
            if (maxAttempts == null || maxAttempts < 1 || maxAttempts > 10) {
                throw new RuntimeException("maxAttempts 必须在 1 到 10 之间");
            }
            if (maxAttempts > 1 && node.getActionId() != null) {
                ActionDO action = actionMapper.selectById(node.getActionId());
                if (action != null && "WEBHOOK".equalsIgnoreCase(action.getActionType())) {
                    try {
                        JSONObject actionConfig = JSON.parseObject(action.getExecutionSteps());
                        if (actionConfig == null || !Boolean.TRUE.equals(actionConfig.getBoolean("idempotent"))) {
                            throw new RuntimeException("WEBHOOK 未声明 idempotent=true，不能配置多次尝试");
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException("WEBHOOK 配置无法校验幂等性", e);
                    }
                }
            }
            Long backoffMs = json.getLong("backoffMs");
            if (backoffMs != null && (backoffMs < 0 || backoffMs > 300000)) {
                throw new RuntimeException("backoffMs 必须在 0 到 300000 毫秒之间");
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("动作节点重试策略无效（示例：{\"maxAttempts\":3,\"backoffMs\":1000}）："
                    + node.getNodeKey() + "，" + e.getMessage(), e);
        }
    }

    private void validateParallelJoinPolicy(ActionWorkflowNodeVO node) {
        if (!StringUtils.hasText(node.getConfigJson())) return;
        try {
            JSONObject config = JSON.parseObject(node.getConfigJson());
            String policy = StringUtils.hasText(config.getString("joinPolicy"))
                    ? config.getString("joinPolicy").trim().toUpperCase(Locale.ROOT) : "ALL";
            if (!"ALL".equals(policy)) {
                throw new RuntimeException("当前运行时只支持 ALL（全部分支成功后继续）");
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("并行汇聚策略无效：" + node.getNodeKey() + "，" + e.getMessage(), e);
        }
    }

    private ActionWorkflowDO requireWorkflow(Long id) {
        ActionWorkflowDO workflow = workflowMapper.selectById(id);
        if (workflow == null) throw new RuntimeException("动作编排不存在: " + id);
        accessGuard.assertWorkflowAccess(workflow);
        return workflow;
    }

    private void validateTriggerConcept(Long ontologyId, Long triggerConceptId) {
        if (triggerConceptId == null) return;
        ConceptDO concept = conceptMapper.selectById(triggerConceptId);
        if (concept == null || !Objects.equals(concept.getOntologyId(), ontologyId)) {
            throw new RuntimeException("触发概念不属于当前本体");
        }
    }

    private Long resolveTriggerConceptId(Long ontologyId, List<ActionWorkflowNodeVO> nodes) {
        List<ActionWorkflowNodeVO> starts = safe(nodes).stream()
                .filter(node -> "START".equalsIgnoreCase(node.getNodeType()))
                .collect(Collectors.toList());
        if (starts.size() != 1) return null;
        ActionDO action = requireNodeAction(ontologyId, starts.get(0), "开始触发节点");
        Long conceptId = resolveActionConceptId(action);
        if (conceptId == null) {
            if ("WEBHOOK".equalsIgnoreCase(action.getActionType())) {
                return null;
            }
            throw new RuntimeException("开始触发动作没有明确的对象概念: " + action.getName());
        }
        return conceptId;
    }

    private ActionDO requireNodeAction(Long ontologyId, ActionWorkflowNodeVO node, String roleName) {
        if (node.getActionId() == null) throw new RuntimeException(roleName + "必须选择已有动作: " + node.getNodeKey());
        ActionDO action = actionMapper.selectById(node.getActionId());
        if (action == null || !Objects.equals(action.getOntologyId(), ontologyId)) {
            throw new RuntimeException(roleName + "引用的动作不属于当前本体: " + node.getNodeKey());
        }
        return action;
    }

    private void validateCompensationAction(Long ontologyId, ActionWorkflowNodeVO node) {
        if (node.getCompensationActionId() == null) return;
        ActionDO compensation = actionMapper.selectById(node.getCompensationActionId());
        if (compensation == null || !Objects.equals(compensation.getOntologyId(), ontologyId)) {
            throw new RuntimeException("补偿动作不属于当前本体: " + node.getNodeKey());
        }
        if (!Boolean.TRUE.equals(compensation.getNeedsApproval())
                && (compensation.getApprovalLevels() == null || compensation.getApprovalLevels() <= 0)) {
            throw new RuntimeException("补偿动作必须配置人工审批: " + compensation.getName());
        }
    }

    private Long resolveActionConceptId(ActionDO action) {
        if (action == null) return null;
        return action.getConceptId() != null ? action.getConceptId() : action.getSourceConceptId();
    }

    private Set<String> collectPredecessors(String nodeKey, Map<String, List<String>> previous) {
        Set<String> result = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>(previous.getOrDefault(nodeKey, Collections.emptyList()));
        while (!queue.isEmpty()) {
            String current = queue.remove();
            if (!result.add(current)) continue;
            queue.addAll(previous.getOrDefault(current, Collections.emptyList()));
        }
        return result;
    }

    private void validateConditionExpression(String text, Long triggerConceptId,
                                             Map<String, ActionWorkflowNodeVO> nodes,
                                             Set<String> upstreamNodeKeys) {
        final JSONObject expression;
        try {
            expression = JSON.parseObject(text);
        } catch (Exception ex) {
            throw new RuntimeException("条件必须使用结构化配置，不能直接填写任意表达式");
        }
        if (!"COMPARE".equals(expression.getString("type"))) {
            throw new RuntimeException("暂不支持的条件类型");
        }
        validateConditionReference(expression.getJSONObject("left"), triggerConceptId, nodes, upstreamNodeKeys);
        String operator = expression.getString("operator");
        if (!CONDITION_OPERATORS.contains(operator)) throw new RuntimeException("不支持的条件操作符: " + operator);
        if ("IS_EMPTY".equals(operator) || "NOT_EMPTY".equals(operator)) return;
        JSONObject right = expression.getJSONObject("right");
        if (right == null) throw new RuntimeException("条件右值不能为空");
        String mode = right.getString("mode");
        if ("REFERENCE".equals(mode)) {
            validateConditionReference(right, triggerConceptId, nodes, upstreamNodeKeys);
        } else if (!"FIXED".equals(mode)) {
            throw new RuntimeException("条件右值模式必须是固定值或动态引用");
        }
    }

    private void validateConditionReference(JSONObject reference, Long triggerConceptId,
                                            Map<String, ActionWorkflowNodeVO> nodes,
                                            Set<String> upstreamNodeKeys) {
        if (reference == null) throw new RuntimeException("条件动态值来源不能为空");
        String source = reference.getString("source");
        String path = reference.getString("path");
        if (!REFERENCE_SOURCES.contains(source)) throw new RuntimeException("不支持的条件动态值来源: " + source);
        if (!StringUtils.hasText(path)) throw new RuntimeException("条件动态值字段不能为空");
        if ("OBJECT".equals(source)) {
            if (triggerConceptId == null) throw new RuntimeException("使用当前对象条件前必须选择触发概念");
            PropertyDO property = propertyMapper.selectOne(new com.datamaster.mybatis.core.query.LambdaQueryWrapperX<PropertyDO>()
                    .eq(PropertyDO::getConceptId, triggerConceptId).eq(PropertyDO::getCode, path));
            if (property == null) throw new RuntimeException("触发概念不存在属性: " + path);
        }
        if ("STEP_OUTPUT".equals(source)) {
            String nodeKey = reference.getString("nodeKey");
            ActionWorkflowNodeVO node = nodes.get(nodeKey);
            if (node == null || !"ACTION".equals(node.getNodeType())) {
                throw new RuntimeException("上游输出引用了无效动作节点: " + nodeKey);
            }
            if (!upstreamNodeKeys.contains(nodeKey)) {
                throw new RuntimeException("条件只能引用当前条件节点之前已执行的动作输出: " + nodeKey);
            }
        }
        if ("CONTEXT".equals(source) && !Arrays.asList("objectKey", "currentUser", "now", "eventId").contains(path)) {
            throw new RuntimeException("不支持的运行上下文字段: " + path);
        }
    }

    private void normalizeWorkflow(ActionWorkflowSaveReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getName())) throw new RuntimeException("编排名称不能为空");
        if (!StringUtils.hasText(reqVO.getCode())) {
            reqVO.setCode("wf_" + UUID.randomUUID().toString().replace("-", ""));
        }
        reqVO.setCode(reqVO.getCode().trim());
        reqVO.setName(reqVO.getName().trim());
        if (!reqVO.getCode().matches("[A-Za-z0-9_-]+")) {
            throw new RuntimeException("编排编码只能包含字母、数字、下划线和短横线");
        }
        String failurePolicy = StringUtils.hasText(reqVO.getFailurePolicy())
                ? reqVO.getFailurePolicy().trim().toUpperCase(Locale.ROOT) : "STOP";
        if (!FAILURE_POLICIES.contains(failurePolicy)) {
            throw new RuntimeException("不支持的失败策略: " + failurePolicy);
        }
        reqVO.setFailurePolicy(failurePolicy);
    }

    private ActionWorkflowDO toWorkflow(ActionWorkflowSaveReqVO req) {
        return ActionWorkflowDO.builder().id(req.getId()).ontologyId(req.getOntologyId())
                .code(req.getCode()).name(req.getName()).triggerConceptId(req.getTriggerConceptId()).triggerRef(req.getTriggerRef())
                .inputSchema(req.getInputSchema()).outputSchema(req.getOutputSchema())
                .failurePolicy(StringUtils.hasText(req.getFailurePolicy()) ? req.getFailurePolicy() : "STOP")
                .description(req.getDescription()).build();
    }

    @SuppressWarnings("unchecked")
    private void validateInputSchema(String inputSchema) {
        validateSchemaDefinition(inputSchema, "输入", true);
    }

    private void validateOutputSchema(String outputSchema) {
        validateSchemaDefinition(outputSchema, "输出", true);
    }

    @SuppressWarnings("unchecked")
    private void validateSchemaDefinition(String schemaText, String label, boolean rootObjectOnly) {
        if (!StringUtils.hasText(schemaText)) return;
        final JSONObject schema;
        try {
            schema = JSON.parseObject(schemaText);
        } catch (Exception e) {
            throw new RuntimeException("Workflow " + label + " Schema 不是合法 JSON");
        }
        if (schema == null) throw new RuntimeException("Workflow " + label + " Schema 必须是 JSON 对象");
        if (rootObjectOnly && !allowsObjectType(schema.get("type"))) {
            throw new RuntimeException("Workflow " + label + " Schema 根节点必须是 object");
        }
        validateSchemaNode(schema, "$", label);
    }

    @SuppressWarnings("unchecked")
    private boolean allowsObjectType(Object type) {
        if (type == null) return true;
        if ("object".equals(type)) return true;
        if (type instanceof Collection) {
            return ((Collection<Object>) type).contains("object");
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void validateSchemaNode(JSONObject schema, String path, String label) {
        Object type = schema.get("type");
        Set<String> supportedTypes = new HashSet<>(Arrays.asList(
                "object", "array", "string", "number", "integer", "boolean", "null"));
        if (type instanceof Collection) {
            for (Object item : (Collection<Object>) type) {
                if (!(item instanceof String) || !supportedTypes.contains(item)) {
                    throw new RuntimeException("Workflow " + label + " Schema type 不受支持: " + item);
                }
            }
        } else if (type != null && (!(type instanceof String) || !supportedTypes.contains(type))) {
            throw new RuntimeException("Workflow " + label + " Schema type 不受支持: " + type);
        }
        Object additionalProperties = schema.get("additionalProperties");
        if (additionalProperties != null && !(additionalProperties instanceof Boolean)) {
            throw new RuntimeException("Workflow " + label + " Schema additionalProperties 必须是布尔值");
        }
        Object enumValue = schema.get("enum");
        if (enumValue != null && !(enumValue instanceof Collection)) {
            throw new RuntimeException("Workflow " + label + " Schema enum 必须是数组");
        }
        Object properties = schema.get("properties");
        if (properties != null && !(properties instanceof Map)) {
            throw new RuntimeException("Workflow " + label + " Schema properties 必须是对象");
        }
        Object required = schema.get("required");
        if (required != null && !(required instanceof Collection)) {
            throw new RuntimeException("Workflow " + label + " Schema required 必须是数组");
        }
        if (required instanceof Collection && properties instanceof Map) {
            for (Object field : (Collection<Object>) required) {
                if (!((Map<String, Object>) properties).containsKey(String.valueOf(field))) {
                    throw new RuntimeException("Workflow " + label + " Schema required 字段未定义: " + path + "." + field);
                }
            }
        }
        if (properties instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) properties).entrySet()) {
                if (!(entry.getValue() instanceof JSONObject)) {
                    throw new RuntimeException("Workflow " + label + " Schema 属性必须是对象: " + path + "." + entry.getKey());
                }
                validateSchemaNode((JSONObject) entry.getValue(), path + "." + entry.getKey(), label);
            }
        }
        Object items = schema.get("items");
        if (items != null) {
            if (!(items instanceof JSONObject)) {
                throw new RuntimeException("Workflow " + label + " Schema items 必须是对象: " + path);
            }
            validateSchemaNode((JSONObject) items, path + "[]", label);
        }
    }

    private ActionWorkflowRespVO toResponse(ActionWorkflowDO data, boolean graph) {
        ActionWorkflowRespVO vo = new ActionWorkflowRespVO();
        vo.setId(data.getId()); vo.setOntologyId(data.getOntologyId()); vo.setCode(data.getCode()); vo.setName(data.getName());
        vo.setTriggerConceptId(data.getTriggerConceptId());
        vo.setVersion(data.getVersion()); vo.setStatus(data.getStatus()); vo.setTriggerRef(data.getTriggerRef());
        vo.setInputSchema(data.getInputSchema()); vo.setOutputSchema(data.getOutputSchema());
        vo.setFailurePolicy(data.getFailurePolicy()); vo.setEnabled(data.getEnabled()); vo.setDescription(data.getDescription());
        vo.setCreateTime(data.getCreateTime());
        if (graph) { vo.setNodes(toNodeVOs(nodeMapper.selectByWorkflowId(data.getId()))); vo.setEdges(toEdgeVOs(edgeMapper.selectByWorkflowId(data.getId()))); }
        return vo;
    }

    private List<ActionWorkflowNodeVO> toNodeVOs(List<ActionWorkflowNodeDO> rows) {
        return rows.stream().map(row -> { ActionWorkflowNodeVO vo = new ActionWorkflowNodeVO();
            vo.setId(row.getId()); vo.setNodeKey(row.getNodeKey()); vo.setName(row.getName()); vo.setNodeType(row.getNodeType());
            vo.setActionId(row.getActionId()); vo.setConfigJson(row.getConfigJson()); vo.setTimeoutMs(row.getTimeoutMs());
            vo.setRetryPolicy(row.getRetryPolicy()); vo.setCompensationActionId(row.getCompensationActionId());
            vo.setPositionX(row.getPositionX()); vo.setPositionY(row.getPositionY()); return vo; }).collect(Collectors.toList());
    }

    private List<ActionWorkflowEdgeVO> toEdgeVOs(List<ActionWorkflowEdgeDO> rows) {
        return rows.stream().map(row -> { ActionWorkflowEdgeVO vo = new ActionWorkflowEdgeVO();
            vo.setId(row.getId()); vo.setEdgeKey(row.getEdgeKey()); vo.setFromNodeKey(row.getFromNodeKey());
            vo.setToNodeKey(row.getToNodeKey()); vo.setConditionExpr(row.getConditionExpr()); vo.setPriority(row.getPriority()); return vo; }).collect(Collectors.toList());
    }

    private static <T> List<T> safe(List<T> list) { return list == null ? Collections.emptyList() : list; }
}
