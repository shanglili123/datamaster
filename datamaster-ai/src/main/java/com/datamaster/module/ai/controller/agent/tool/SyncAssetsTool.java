package com.datamaster.module.ai.controller.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSyncReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.service.asset.IAssetsAssetSyncService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.governance.api.cat.dto.CategoryReqDTO;
import com.datamaster.module.governance.api.cat.dto.CategoryRespDTO;
import com.datamaster.module.governance.api.service.cat.ITaxonomyCategoryApiService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将已采集的目录元数据同步为数据资产。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class SyncAssetsTool {

    @Resource
    private IAssetsAssetSyncService assetSyncService;
    @Resource
    private IAssetsDatasourceService datasourceService;
    @Resource
    private ITaxonomyCategoryApiService categoryApiService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "sync_assets", value = "把指定数据源中已采集的目录元数据同步为数据资产；数据库、模式和资产目录可省略，由平台根据数据源配置和当前空间自动补齐")
    public AgentToolResult execute(AgentToolRequest.SyncAssets input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "请先确定当前空间", operationId(context));
        }
        Long datasourceId = AgentToolIds.longValue(input.getDatasourceId());
        if (datasourceId == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "同步资产需要 datasourceId", operationId(context));
        }
        AssetsDatasourceDO datasource = datasourceService.getDatasourceDOById(datasourceId);
        if (datasource == null) {
            return AgentToolResult.failure("数据源不存在：" + datasourceId);
        }
        if (!resourceGuard.datasourceVisible(datasourceId, context)) {
            return AgentToolResult.failure("数据源不属于当前空间，拒绝同步资产");
        }
        if (!resourceGuard.matchesCurrentDatasource(datasourceId, context)) {
            return AgentToolResult.failure("请求的数据源与当前会话目标不一致；当前目标为 "
                    + resourceGuard.currentDatasourceDescription(context) + "，已拒绝同步其他数据源的资产");
        }

        JSONObject config = parseConfig(datasource.getDatasourceConfig());
        String databaseName = firstText(input.getDatabaseName(), config.getString("dbname"));
        if (databaseName == null) {
            return AgentToolResult.failure("数据源连接配置缺少 dbname，无法确定需要同步的数据库；请先修正数据源配置");
        }
        String schemaName = firstText(input.getSchemaName(), config.getString("schema"), config.getString("sid"));
        if (schemaName == null && isPostgres(datasource.getDatasourceType())) {
            schemaName = "public";
        }
        String catCode = text(input.getCatCode());
        if (catCode == null) {
            catCode = resolveAssetCategory(context);
        }

        AssetsAssetSyncReqVO request = new AssetsAssetSyncReqVO();
        request.setDatasourceId(datasourceId);
        request.setDatabaseName(databaseName);
        request.setSchemaName(schemaName);
        request.setCatCode(catCode);
        request.setSpaceId(context.getSpaceId());
        request.setSpaceCode(context.getSpaceCode());
        AjaxResult response = assetSyncService.sync(request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datasourceId", String.valueOf(datasourceId));
        result.put("datasourceName", datasource.getDatasourceName());
        result.put("databaseName", databaseName);
        result.put("schemaName", schemaName);
        result.put("catCode", catCode);
        result.put("syncResult", response);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result,
                "数据资产同步完成", operationId(context));
    }

    private String resolveAssetCategory(AgentToolContext context) {
        CategoryReqDTO request = new CategoryReqDTO();
        request.setCatType("ASSET");
        request.setSpaceId(context.getSpaceId());
        request.setSpaceCode(context.getSpaceCode());
        List<CategoryRespDTO> categories = categoryApiService.getCategoryList(request);
        String categoryCode = firstCategoryCode(categories);
        if (categoryCode == null
                && (context.getSpaceId() != null || text(context.getSpaceCode()) != null)) {
            request.setSpaceId(null);
            request.setSpaceCode(null);
            categories = categoryApiService.getCategoryList(request);
            categoryCode = firstCategoryCode(categories);
        }
        if (categoryCode != null) return categoryCode;
        throw new IllegalStateException("当前空间没有可用的数据资产目录，请先配置资产目录");
    }

    private String firstCategoryCode(List<CategoryRespDTO> categories) {
        if (categories != null) {
            for (CategoryRespDTO category : categories) {
                if (category != null && !Boolean.FALSE.equals(category.getValidFlag())
                        && !Boolean.TRUE.equals(category.getDelFlag()) && text(category.getCode()) != null) {
                    return category.getCode().trim();
                }
            }
        }
        return null;
    }

    private JSONObject parseConfig(String config) {
        if (text(config) == null) return new JSONObject();
        try {
            JSONObject result = JSON.parseObject(config);
            return result == null ? new JSONObject() : result;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private String firstText(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            String result = text(value);
            if (result != null) return result;
        }
        return null;
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty()
                ? null : String.valueOf(value).trim();
    }

    private boolean isPostgres(String type) {
        String value = type == null ? "" : type.toLowerCase();
        return value.contains("postgres") || value.contains("kingbase");
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }

}
