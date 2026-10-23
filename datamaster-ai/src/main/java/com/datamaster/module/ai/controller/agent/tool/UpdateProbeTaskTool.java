package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskSaveReqVO;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskRespVO;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskScopeSaveReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjSaveReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluateSaveReqVO;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.metadata.service.task.ICatalogTaskService;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 更新探查任务配置；发布和执行由独立 Tool 控制。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class UpdateProbeTaskTool {
    @Resource private ICatalogTaskService catalogTaskService;
    @Resource private AgentToolResourceGuard resourceGuard;
    @Tool(name = "update_probe_task", value = "更新元数据探查任务名称、数据库范围、模式名或空间绑定，不自动发布执行")
    public AgentToolResult execute(AgentToolRequest.UpdateProbeTask input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "请先确定当前空间", operationId(context));
        Long taskId = AgentToolIds.longValue(input.getTaskId());
        if (taskId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 taskId", operationId(context));
        CatalogTaskDO task = catalogTaskService.getCatalogTaskById(taskId);
        if (task == null) return AgentToolResult.failure("探查任务不存在：" + taskId);
        if (context == null || (context.getSpaceId() == null && text(context.getSpaceCode()) == null)
                || (context.getSpaceId() != null && !Objects.equals(context.getSpaceId(), task.getSpaceId()))
                || (text(context.getSpaceCode()) != null && !Objects.equals(context.getSpaceCode(), task.getSpaceCode()))) {
            return AgentToolResult.failure("探查任务不属于当前空间，拒绝更新");
        }
        if (input.getSchemaName() != null && input.getDatabaseName() == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "修改模式名时请同时指定 databaseName", operationId(context));
        }
        CatalogTaskRespVO detail = catalogTaskService.getCatalogTaskByIdNew(taskId);
        if (detail == null) return AgentToolResult.failure("无法读取探查任务完整配置");
        CatalogTaskSaveReqVO req = BeanUtils.toBean(detail, CatalogTaskSaveReqVO.class);
        // updateCatalogTask 是完整表单保存语义，必须携带原范围和质量规则，不能用部分 VO 覆盖。
        req.setScopeSaveReqVOS(BeanUtils.toBean(detail.getScopeSaveReqVOS(), CatalogTaskScopeSaveReqVO.class));
        req.setQualityObjs(BeanUtils.toBean(detail.getQualityObjs(), QualityTaskObjSaveReqVO.class));
        req.setQualityEvaluates(BeanUtils.toBean(detail.getQualityEvaluates(), QualityTaskEvaluateSaveReqVO.class));
        req.setName(text(input.getName(), task.getName()));
        if (input.getDatabaseName() != null) {
            if (text(input.getDatabaseName()) == null) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "databaseName 不能为空", operationId(context));
            }
            req.setCollectionScope("1");
            CatalogTaskScopeSaveReqVO scope = new CatalogTaskScopeSaveReqVO();
            scope.setDbName(text(input.getDatabaseName(), null));
            scope.setSchemaName(resolveSchema(input.getSchemaName(), task.getDbType())); scope.setTaskId(taskId);
            scope.setSpaceId(task.getSpaceId()); scope.setSpaceCode(task.getSpaceCode());
            req.setScopeSaveReqVOS(Collections.singletonList(scope));
        }
        if ("1".equals(req.getCollectionScope()) && (req.getScopeSaveReqVOS() == null || req.getScopeSaveReqVOS().isEmpty())) {
            return AgentToolResult.failure("自定义库任务没有采集范围，请补充 databaseName 后再修改");
        }
        catalogTaskService.updateCatalogTask(req);
        Map<String, Object> data = new LinkedHashMap<>(); data.put("id", taskId); data.put("updated", true); data.put("status", task.getStatus());
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "探查任务更新成功，尚未发布执行", operationId(context));
    }
    private String operationId(AgentToolContext c) { return c == null ? null : c.getOperationId(); }
    private String text(Object v) { return v == null || String.valueOf(v).trim().isEmpty() ? null : String.valueOf(v).trim(); }
    private String text(Object v, String fallback) { String s = text(v); return s == null ? fallback : s; }
    private String resolveSchema(Object value, String datasourceType) {
        String schema = text(value);
        if (schema != null) return schema;
        String type = datasourceType == null ? "" : datasourceType;
        return "PostgreSQL".equalsIgnoreCase(type) || "POSTGRESQL".equalsIgnoreCase(type)
                || "Kingbase".equalsIgnoreCase(type) || "Kingbase8".equalsIgnoreCase(type) ? "public" : null;
    }
}
