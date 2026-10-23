package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.metadata.api.service.table.CatalogTableApiService;
import com.datamaster.metadata.api.table.dto.CatalogTableRespDTO;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel.AssetsAssetSpaceRelDO;
import com.datamaster.module.assets.dal.dataobject.assetColumnSpaceRel.AssetsAssetColumnSpaceRelDO;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.assetColumn.IAssetsAssetColumnService;
import com.datamaster.module.assets.service.assetchild.spaceRel.IAssetsAssetSpaceRelService;
import com.datamaster.module.assets.service.assetColumnSpaceRel.IAssetsAssetColumnSpaceRelService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 将当前探查范围内的资产和字段绑定到 Agent 当前空间。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class AuthorizeAssetsTool {

    @Resource private CatalogTableApiService catalogTableApiService;
    @Resource private IAssetsAssetService assetService;
    @Resource private IAssetsAssetColumnService assetColumnService;
    @Resource private IAssetsAssetSpaceRelService assetSpaceRelService;
    @Resource private IAssetsAssetColumnSpaceRelService assetColumnSpaceRelService;
    @Resource private AgentToolResourceGuard resourceGuard;
    @Tool(name = "authorize_assets", value = "把指定数据源和数据库范围内的数据资产及字段授权给当前空间，重复授权会自动跳过")
    public AgentToolResult execute(AgentToolRequest.AuthorizeAssets input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long datasourceId = AgentToolIds.longValue(input.getDatasourceId());
        String databaseName = text(input.getDatabaseName());
        if (datasourceId == null || databaseName == null || !resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "授权资产需要 datasourceId、databaseName 和当前空间", context.getOperationId());
        }
        if (!resourceGuard.datasourceVisible(datasourceId, context)) {
            return AgentToolResult.failure("数据源不属于当前空间，拒绝授权资产");
        }
        String schemaName = text(input.getSchemaName());
        List<CatalogTableRespDTO> tables = catalogTableApiService.listByDatasourceAndDatabase(
                datasourceId, databaseName, schemaName, context.getSpaceId(), context.getSpaceCode());
        Set<String> tableNames = new HashSet<>();
        if (tables != null) for (CatalogTableRespDTO table : tables) {
            if (table != null && table.getTableName() != null) tableNames.add(table.getTableName().toLowerCase());
        }
        List<AssetsAssetDO> assets = assetService.list(Wrappers.<AssetsAssetDO>lambdaQuery().eq(AssetsAssetDO::getDatasourceId, datasourceId));
        Set<String> existingAssetRelations = new HashSet<>();
        List<AssetsAssetSpaceRelDO> relations = assetSpaceRelService.getAssetSpaceRelList();
        if (relations != null) for (AssetsAssetSpaceRelDO relation : relations) {
            if (relation != null && sameSpace(relation.getSpaceId(), context.getSpaceId())
                    && sameText(relation.getSpaceCode(), context.getSpaceCode())) {
                existingAssetRelations.add(String.valueOf(relation.getAssetId()));
            }
        }
        Set<String> existingColumnRelations = new HashSet<>();
        List<AssetsAssetColumnSpaceRelDO> columnRelations = assetColumnSpaceRelService.getAssetColumnSpaceRelList();
        if (columnRelations != null) for (AssetsAssetColumnSpaceRelDO relation : columnRelations) {
            if (relation != null && sameSpace(relation.getSpaceId(), context.getSpaceId())
                    && sameText(relation.getSpaceCode(), context.getSpaceCode())) {
                existingColumnRelations.add(String.valueOf(relation.getColumnId()));
            }
        }
        int assetCount = 0;
        int columnCount = 0;
        if (assets != null) for (AssetsAssetDO asset : assets) {
            if (asset == null || asset.getTableName() == null || !tableNames.contains(asset.getTableName().toLowerCase())) continue;
            if (!existingAssetRelations.contains(String.valueOf(asset.getId()))) {
                AssetsAssetSpaceRelSaveReqVO relation = new AssetsAssetSpaceRelSaveReqVO();
                relation.setAssetId(asset.getId());
                relation.setSpaceId(context.getSpaceId());
                relation.setSpaceCode(context.getSpaceCode());
                assetSpaceRelService.createAssetSpaceRel(relation);
                assetCount++;
            }
            AssetsAssetColumnPageReqVO columnRequest = new AssetsAssetColumnPageReqVO();
            columnRequest.setAssetId(asset.getId());
            List<AssetsAssetColumnDO> columns = assetColumnService.getAssetColumnList(columnRequest);
            if (columns == null) continue;
            for (AssetsAssetColumnDO column : columns) {
                if (column == null || existingColumnRelations.contains(String.valueOf(column.getId()))) continue;
                AssetsAssetColumnSpaceRelSaveReqVO relation = new AssetsAssetColumnSpaceRelSaveReqVO();
                relation.setAssetId(asset.getId());
                relation.setColumnId(column.getId());
                relation.setSpaceId(context.getSpaceId());
                relation.setSpaceCode(context.getSpaceCode());
                assetColumnSpaceRelService.createAssetColumnSpaceRel(relation);
                columnCount++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assetCount", assetCount);
        result.put("columnCount", columnCount);
        result.put("tableCount", tableNames.size());
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "资产和字段授权完成", context.getOperationId());
    }
    private String text(Object value) { return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim(); }
    private boolean sameSpace(Long first, Long second) { return first == null ? second == null : first.equals(second); }
    private boolean sameText(String first, String second) { return first == null || first.trim().isEmpty() ? second == null || second.trim().isEmpty() : first.equalsIgnoreCase(second); }
}
