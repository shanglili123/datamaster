package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetPageReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 查询当前空间可见的数据资产摘要，不返回原始连接配置或完整内部对象。 */
@Component
@AgentTool
public class ListAssetsTool {

    private static final int MAX_RESULTS = 200;

    @Resource
    private IAssetsAssetService assetService;
    @Resource
    private AgentToolResourceGuard resourceGuard;

    @Tool(name = "list_assets", value = "查询当前空间可见的数据资产摘要，可按数据源、表名或资产名称筛选")
    public AgentToolResult execute(AgentToolRequest.ListAssets input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "请先确定当前空间", operationId(context));
        }
        AgentToolRequest.ListAssets request = input == null ? new AgentToolRequest.ListAssets() : input;
        Long datasourceId = null;
        try {
            datasourceId = AgentToolIds.longValue(request.getDatasourceId());
        } catch (com.datamaster.common.exception.ServiceException ignored) {
            // 模型偶尔会在长会话中重放 ds-xxx 占位符；后续统一回退到
            // 当前流程已经由 get/create datasource 绑定的真实数据源。
        }
        boolean correctedDatasource = false;
        if (datasourceId == null) {
            Long currentDatasourceId = resourceGuard.currentDatasourceId(context);
            if (currentDatasourceId != null) {
                datasourceId = currentDatasourceId;
                correctedDatasource = true;
            }
        }
        if (datasourceId != null && !resourceGuard.datasourceVisible(datasourceId, context)) {
            // 长会话中模型可能复用了上一轮的旧 ID。自主操作记录中的目标数据源
            // 才是平台可信上下文；仅在该目标本身属于当前空间时替换候选值。
            Long currentDatasourceId = resourceGuard.currentDatasourceId(context);
            if (currentDatasourceId == null) {
                return AgentToolResult.failure("数据源不属于当前空间，拒绝查询资产");
            }
            datasourceId = currentDatasourceId;
            correctedDatasource = true;
        }
        AssetsAssetPageReqVO query = new AssetsAssetPageReqVO();
        query.setDatasourceId(datasourceId);
        query.setTableName(text(request.getTableName()));
        query.setName(text(request.getName()));
        query.setSpaceId(context.getSpaceId());
        query.setSpaceCode(context.getSpaceCode());
        List<AssetsAssetDO> assets = assetService.getAssetList(query);
        boolean correctedTableFilter = false;
        // 普通用户常直接说物理表名，模型有时会把它放到 name（资产展示名）
        // 而不是 tableName。展示名查询为空时用同一个值重试物理表名，避免
        // 已存在的资产被误报成“当前空间暂无资产”。
        if ((assets == null || assets.isEmpty())
                && text(request.getTableName()) == null && text(request.getName()) != null) {
            query.setName(null);
            query.setTableName(text(request.getName()));
            assets = assetService.getAssetList(query);
            correctedTableFilter = assets != null && !assets.isEmpty();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        if (assets != null) {
            for (AssetsAssetDO asset : assets) {
                if (asset == null) continue;
                result.add(summary(asset));
                if (result.size() >= MAX_RESULTS) break;
            }
        }
        String message = correctedDatasource || correctedTableFilter
                ? "已使用当前会话实际数据源和表名查询当前空间数据资产"
                : "已查询当前空间数据资产";
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, message, operationId(context));
    }

    private Map<String, Object> summary(AssetsAssetDO asset) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", asset.getId());
        item.put("name", asset.getName());
        item.put("tableName", asset.getTableName());
        item.put("tableComment", asset.getTableComment());
        item.put("datasourceId", asset.getDatasourceId());
        item.put("datasourceName", asset.getDatasourceName());
        item.put("status", asset.getStatus());
        item.put("type", asset.getType());
        item.put("fieldCount", asset.getFieldCount());
        return item;
    }

    private String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
