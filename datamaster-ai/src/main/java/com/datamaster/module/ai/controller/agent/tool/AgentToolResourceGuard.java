package com.datamaster.module.ai.controller.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceSpaceRelDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceSpaceRelService;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionRespVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.datamaster.module.ontology.service.IActionService;
import com.datamaster.module.ontology.service.IOntologyService;
import com.datamaster.module.governance.dal.dataobject.space.TaxonomySpaceDO;
import com.datamaster.module.governance.service.space.ITaxonomySpaceService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Date;

/**
 * Agent Tool 资源归属守卫。
 *
 * Tool 只能使用当前调用上下文中的空间资源；模型传入的资源 ID 只是候选值，
 * 不能替代空间归属校验。具体业务 Service 仍负责最终的业务规则和数据权限。
 */
@Component
public class AgentToolResourceGuard {

    @Resource
    private IOntologyService ontologyService;

    @Resource
    private IActionService actionService;

    @Resource
    private IActionExecutionService actionExecutionService;

    @Resource
    private IAssetsDatasourceSpaceRelService datasourceSpaceRelService;

    @Resource
    private IAssetsDatasourceService datasourceService;

    @Resource
    private ITaxonomySpaceService taxonomySpaceService;

    @Resource
    private IAiAgentOperationService agentOperationService;

    public boolean hasSpace(AgentToolContext context) {
        if (context == null || context.getUserId() == null
                || (context.getSpaceId() == null && blank(context.getSpaceCode()))) return false;
        if (taxonomySpaceService == null) return false;
        List<TaxonomySpaceDO> spaces = taxonomySpaceService.getCurrentUserSpaceList(context.getUserId());
        if (spaces == null) return false;
        for (TaxonomySpaceDO space : spaces) {
            if (space != null && !Boolean.FALSE.equals(space.getValidFlag())
                    && spaceMatches(space.getId(), space.getCode(), context)) return true;
        }
        return false;
    }

    public boolean datasourceVisible(Long datasourceId, AgentToolContext context) {
        if (datasourceId == null || !hasSpace(context)) return false;
        AssetsDatasourceSpaceRelDO query = AssetsDatasourceSpaceRelDO.builder().build();
        query.setDatasourceId(datasourceId);
        List<AssetsDatasourceSpaceRelDO> relations = datasourceSpaceRelService.getDatasourceSpaceRelList(query);
        if (relations == null) return false;
        for (AssetsDatasourceSpaceRelDO relation : relations) {
            if (relation != null && !Boolean.FALSE.equals(relation.getValidFlag())
                    && spaceMatches(relation.getSpaceId(), relation.getSpaceCode(), context)) return true;
        }
        return false;
    }

    /**
     * 防止模型从长会话历史中拿到另一个数据源的旧 ID。已有自主操作明确记录了
     * 当前数据源时，后续探查任务只能继续使用该数据源；新操作尚未选定数据源时放行。
     */
    public boolean matchesCurrentDatasource(Long datasourceId, AgentToolContext context) {
        if (datasourceId == null || context == null || blank(context.getOperationId())
                || agentOperationService == null) return true;
        AiAgentOperationDO operation = agentOperationService.get(context.getOperationId());
        if (operation == null) return true;
        if (operation.getDatasourceId() != null) return operation.getDatasourceId().equals(datasourceId);
        if (blank(operation.getDatasourceName()) && blank(operation.getDatabaseName())) return true;
        AssetsDatasourceDO candidate = datasourceService.getDatasourceDOById(datasourceId);
        if (candidate == null) return false;
        if (!blank(operation.getDatasourceName())
                && operation.getDatasourceName().equalsIgnoreCase(candidate.getDatasourceName())) return true;
        String candidateDatabase = databaseName(candidate.getDatasourceConfig());
        return !blank(operation.getDatabaseName()) && operation.getDatabaseName().equalsIgnoreCase(candidateDatabase);
    }

    public String currentDatasourceDescription(AgentToolContext context) {
        if (context == null || blank(context.getOperationId()) || agentOperationService == null) return null;
        AiAgentOperationDO operation = agentOperationService.get(context.getOperationId());
        if (operation == null || operation.getDatasourceId() == null) return null;
        String name = blank(operation.getDatasourceName()) ? "" : "（" + operation.getDatasourceName() + "）";
        return operation.getDatasourceId() + name;
    }

    /**
     * 返回当前自主操作已经确定的数据源。模型传入的 ID 只是候选值；当长会话中的
     * 候选 ID 过期或来自其他历史轮次时，调用方可以回到这个受控目标继续执行。
     */
    public Long currentDatasourceId(AgentToolContext context) {
        if (context == null || blank(context.getOperationId()) || agentOperationService == null) return null;
        AiAgentOperationDO operation = agentOperationService.get(context.getOperationId());
        if (operation == null || operation.getDatasourceId() == null) return null;
        return datasourceVisible(operation.getDatasourceId(), context) ? operation.getDatasourceId() : null;
    }

    public AiAgentOperationDO currentOperation(AgentToolContext context) {
        if (context == null || blank(context.getOperationId()) || agentOperationService == null) return null;
        return agentOperationService.get(context.getOperationId());
    }

    /** 将 Tool 产出的资源 ID 绑定到当前流程，后续轮次不再依赖模型重复猜 ID。 */
    public void bindCatalogTask(AgentToolContext context, Long taskId) {
        AiAgentOperationDO operation = currentOperation(context);
        if (operation == null || taskId == null) return;
        operation.setCatalogTaskId(taskId);
        operation.setUpdateTime(new Date());
        agentOperationService.update(operation);
    }

    public void bindDatasource(AgentToolContext context, Long datasourceId) {
        AiAgentOperationDO operation = currentOperation(context);
        if (operation == null || datasourceId == null) return;
        if (operation.getDatasourceId() != null && !datasourceId.equals(operation.getDatasourceId())) return;
        if (operation.getDatasourceId() == null) {
            operation.setDatasourceId(datasourceId);
            AssetsDatasourceDO datasource = datasourceService.getDatasourceDOById(datasourceId);
            if (datasource != null) operation.setDatasourceName(datasource.getDatasourceName());
            String space = operation.getSpaceId() == null
                    ? (operation.getSpaceCode() == null ? "" : operation.getSpaceCode().trim())
                    : String.valueOf(operation.getSpaceId());
            operation.setFlowKey("DATASOURCE:" + space + ":" + datasourceId);
            operation.setUpdateTime(new Date());
            agentOperationService.update(operation);
        }
    }

    public void bindOntology(AgentToolContext context, Long ontologyId) {
        AiAgentOperationDO operation = currentOperation(context);
        if (operation == null || ontologyId == null) return;
        operation.setOntologyId(ontologyId);
        operation.setUpdateTime(new Date());
        agentOperationService.update(operation);
    }

    public boolean ontologyVisible(Long ontologyId, AgentToolContext context) {
        if (ontologyId == null || !hasSpace(context)) return false;
        OntologyRespVO ontology = ontologyService.getOntologyById(ontologyId);
        return ontology != null && spaceMatches(ontology.getSpaceId(), ontology.getSpaceCode(), context);
    }

    public boolean actionVisible(Long actionId, AgentToolContext context) {
        if (actionId == null) return false;
        ActionRespVO action = actionService.getActionById(actionId);
        return action != null && ontologyVisible(action.getOntologyId(), context);
    }

    public boolean executionVisible(Long executionId, AgentToolContext context) {
        if (executionId == null) return false;
        ExecutionRespVO execution = actionExecutionService.getExecutionById(executionId);
        return execution != null && ontologyVisible(execution.getOntologyId(), context);
    }

    private boolean spaceMatches(Long resourceSpaceId, String resourceSpaceCode, AgentToolContext context) {
        if (context == null) return false;
        // spaceId 是空间主键，存在时以它为准。历史数据可能只保存了一个
        // 空间字段，或 spaceCode 曾经被重命名；强制两个字段同时一致会误拒绝
        // 当前用户自己空间中的资源。没有 ID 时才退回比较空间编码。
        if (context.getSpaceId() != null && resourceSpaceId != null) {
            return context.getSpaceId().equals(resourceSpaceId);
        }
        return !blank(context.getSpaceCode()) && !blank(resourceSpaceCode)
                && context.getSpaceCode().equalsIgnoreCase(resourceSpaceCode);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String databaseName(String config) {
        try { return blank(config) ? null : JSON.parseObject(config).getString("dbname"); }
        catch (Exception ignored) { return null; }
    }
}
