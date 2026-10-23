package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskSaveReqVO;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskScopeSaveReqVO;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.metadata.service.task.ICatalogTaskService;
import com.datamaster.metadata.service.task.ICatalogTaskScopeService;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskScopeDO;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 新增指定数据库范围的元数据探查任务，不自动发布或执行。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class CreateProbeTaskTool {

    @Resource private ICatalogTaskService catalogTaskService;
    @Resource private ICatalogTaskScopeService catalogTaskScopeService;
    @Resource private IAssetsDatasourceService datasourceService;
    @Resource private AgentToolResourceGuard resourceGuard;
    @Tool(name = "create_probe_task", value = "为数据源创建或复用指定数据库和模式的元数据探查任务；当前流程已有任务时必须复用，不自动发布或执行")
    public AgentToolResult execute(AgentToolRequest.CreateProbeTask input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long datasourceId = input == null ? null : AgentToolIds.longValue(input.getDatasourceId());
        String databaseName = input == null ? null : text(input.getDatabaseName());
        if (datasourceId == null || databaseName == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "需要 datasourceId 和 databaseName", operationId(context));
        }
        AssetsDatasourceDO datasource = datasourceService.getDatasourceDOById(datasourceId);
        if (datasource == null) return AgentToolResult.failure("数据源不存在：" + datasourceId);
        if (!resourceGuard.datasourceVisible(datasourceId, context)) return AgentToolResult.failure("数据源不属于当前空间，拒绝创建探查任务");
        if (!resourceGuard.matchesCurrentDatasource(datasourceId, context)) {
            return AgentToolResult.failure("请求的数据源与当前会话目标不一致；当前目标为 "
                    + resourceGuard.currentDatasourceDescription(context) + "，已拒绝创建其他数据源的探查任务");
        }
        resourceGuard.bindDatasource(context, datasourceId);
        String schemaName = resolveSchema(input.getSchemaName(), datasource.getDatasourceType());
        // 优先复用当前流程已经绑定的探查任务。流程 ID 是跨自然语言轮次的
        // 稳定标志，避免模型每次都按数据库名重新创建任务。
        com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
        if (operation != null && operation.getCatalogTaskId() != null
                && datasourceId.equals(operation.getDatasourceId())) {
            CatalogTaskDO bound = catalogTaskService.getCatalogTaskById(operation.getCatalogTaskId());
            if (bound != null && visibleInSpace(bound, context)
                    && datasourceId.equals(bound.getDatasourceId())) {
                Map<String, Object> reused = new LinkedHashMap<>();
                reused.put("id", bound.getId()); reused.put("catalogTaskId", bound.getId());
                reused.put("name", bound.getName()); reused.put("datasourceId", datasourceId);
                reused.put("databaseName", databaseName); reused.put("schemaName", schemaName);
                reused.put("reused", true); reused.put("status", bound.getStatus());
                return AgentToolResult.of(AgentToolResult.SUCCEEDED, reused, "已复用当前流程绑定的探查任务", operationId(context));
            }
        }
        // 只查询目标数据源的库级探查任务。原先调用 getCatalogTaskList() 会加载并
        // 转换所有历史任务；只要其中一条引用的数据源已被删除，转换层就可能因
        // 空 source 抛出 "Source must not be null"，从而阻断当前完全无关的任务创建。
        List<CatalogTaskDO> existingTasks = catalogTaskService.lambdaQuery()
                .eq(CatalogTaskDO::getDatasourceId, datasourceId)
                .eq(CatalogTaskDO::getCollectionScope, "1")
                .list();
        if (existingTasks != null) for (CatalogTaskDO existing : existingTasks) {
            if (existing == null || !datasourceId.equals(existing.getDatasourceId())
                    || !same(existing.getSpaceId(), context == null ? null : context.getSpaceId())
                    || !sameText(existing.getSpaceCode(), context == null ? null : context.getSpaceCode())) continue;
            // existing.getId() 是探查任务 ID，不能传给按数据源 ID 实时读取库列表的方法。
            // 这里要检查的是该历史任务实际保存的采集范围，因此按 taskId 查询 CAT_TASK_SCOPE。
            List<CatalogTaskScopeDO> scopes = catalogTaskScopeService
                    .getCatalogTaskScopeListBytaskId(existing.getId());
            if (scopes != null) for (CatalogTaskScopeDO scope : scopes) {
                if (scope != null && databaseName.equalsIgnoreCase(scope.getDbName())
                        && sameText(resolveSchema(scope.getSchemaName(), datasource.getDatasourceType()), schemaName)) {
                    Map<String, Object> reused = new LinkedHashMap<>(); reused.put("id", existing.getId());
                    reused.put("catalogTaskId", existing.getId()); reused.put("name", existing.getName());
                    reused.put("datasourceId", datasourceId); reused.put("datasourceName", datasource.getDatasourceName());
                    reused.put("databaseName", databaseName); reused.put("schemaName", schemaName);
                    reused.put("reused", true); reused.put("status", existing.getStatus());
                    resourceGuard.bindCatalogTask(context, existing.getId());
                    return AgentToolResult.of(AgentToolResult.SUCCEEDED, reused, "已复用相同数据库范围的探查任务", operationId(context));
                }
            }
        }
        CatalogTaskSaveReqVO request = new CatalogTaskSaveReqVO();
        request.setDatasourceId(datasourceId);
        request.setSourceSystemName(datasource.getDatasourceName());
        request.setName(text(input.getName()) == null ? "数据智能体探查-" + databaseName : text(input.getName()));
        request.setDbType(datasource.getDatasourceType()); request.setCollectionMode("1");
        request.setCollectionScope("1"); request.setCollectType("1"); request.setStatus("0");
        request.setSpaceId(context == null ? null : context.getSpaceId());
        request.setSpaceCode(context == null ? null : context.getSpaceCode());
        CatalogTaskScopeSaveReqVO scope = new CatalogTaskScopeSaveReqVO();
        scope.setDbName(databaseName); scope.setSchemaName(schemaName);
        scope.setSpaceId(request.getSpaceId()); scope.setSpaceCode(request.getSpaceCode());
        request.setScopeSaveReqVOS(Collections.singletonList(scope));
        Long taskId = catalogTaskService.createCatalogTask(request);
        CatalogTaskDO created = catalogTaskService.getCatalogTaskById(taskId);
        Map<String, Object> data = new LinkedHashMap<>(); data.put("id", taskId);
        data.put("catalogTaskId", taskId);
        data.put("name", created == null ? request.getName() : created.getName()); data.put("status", "0");
        data.put("datasourceId", datasourceId); data.put("datasourceName", datasource.getDatasourceName());
        data.put("databaseName", databaseName);
        data.put("schemaName", scope.getSchemaName());
        resourceGuard.bindCatalogTask(context, taskId);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "探查任务创建成功，尚未发布执行", operationId(context));
    }
    private String operationId(AgentToolContext c) { return c == null ? null : c.getOperationId(); }
    private String text(Object v) { return v == null || String.valueOf(v).trim().isEmpty() ? null : String.valueOf(v).trim(); }
    private String resolveSchema(Object value, String datasourceType) {
        String schema = text(value);
        if (schema != null) return schema;
        String type = datasourceType == null ? "" : datasourceType;
        return "PostgreSQL".equalsIgnoreCase(type) || "POSTGRESQL".equalsIgnoreCase(type)
                || "Kingbase".equalsIgnoreCase(type) || "Kingbase8".equalsIgnoreCase(type) ? "public" : null;
    }
    private boolean same(Long a, Long b) { return a == null ? b == null : a.equals(b); }
    private boolean sameText(String a, String b) { return a == null ? b == null || b.trim().isEmpty() : a.equalsIgnoreCase(b == null ? "" : b); }
    private boolean visibleInSpace(CatalogTaskDO task, AgentToolContext context) {
        if (task == null || context == null
                || (context.getSpaceId() == null && text(context.getSpaceCode()) == null)) return false;
        return (context.getSpaceId() == null || context.getSpaceId().equals(task.getSpaceId()))
                && (text(context.getSpaceCode()) == null
                || context.getSpaceCode().equalsIgnoreCase(task.getSpaceCode()));
    }
}
