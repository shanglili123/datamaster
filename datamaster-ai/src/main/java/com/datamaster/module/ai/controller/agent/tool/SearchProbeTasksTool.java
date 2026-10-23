package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskPageReqVO;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.metadata.service.task.ICatalogTaskService;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 按任务 ID、名称、数据源和状态查询当前空间的探查任务。 */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY)
public class SearchProbeTasksTool {
    @Resource private ICatalogTaskService catalogTaskService;

    @Tool(name = "search_probe_tasks", value = "查询当前空间的元数据探查任务；支持 taskId、name、datasourceId、status 条件，只查询不发布或执行")
    public AgentToolResult execute(AgentToolRequest.SearchProbeTasks input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        CatalogTaskPageReqVO request = new CatalogTaskPageReqVO();
        request.setId(input == null ? null : AgentToolIds.longValue(input.getTaskId()));
        request.setName(input == null ? null : text(input.getName()));
        request.setDatasourceId(input == null ? null : AgentToolIds.longValue(input.getDatasourceId()));
        request.setStatus(input == null ? null : text(input.getStatus()));
        request.setSpaceId(context == null ? null : context.getSpaceId());
        request.setSpaceCode(context == null ? null : context.getSpaceCode());
        request.setPageNum(1); request.setPageSize(input == null || input.getLimit() == null ? 20 : Math.max(1, Math.min(input.getLimit(), 100)));
        PageResult<CatalogTaskDO> page = catalogTaskService.getCatalogTaskPage(request);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, page, "探查任务查询完成", context == null ? null : context.getOperationId());
    }
    private String text(Object value) { return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim(); }
}
