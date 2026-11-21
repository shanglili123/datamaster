package com.datamaster.module.ai.agent.runtime;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.metadata.dal.mapper.metadata.CatalogTableMapper;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import com.datamaster.module.ai.controller.agent.tool.AgentToolResourceGuard;
import com.datamaster.module.ai.service.skill.AiAskOntologyContextService;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel.AssetsAssetSpaceRelDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.assetchild.spaceRel.AssetsAssetSpaceRelMapper;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact table lookup within the current source; never opens a data connection or returns row counts. */
@Component
class UnifiedAskCatalog {
    @Resource private AiAskOntologyContextService ontologyContexts;
    @Resource private AgentToolResourceGuard resources;
    @Resource private CatalogTableMapper metadata;
    @Resource private AssetsAssetMapper assets;
    @Resource private AssetsAssetSpaceRelMapper assetSpaces;
    @Resource private IAssetsTableGovernanceApiService governance;
    @Resource private ISpaceFlowApi flows;

    boolean available() {
        return SecurityUtils.hasPermi("ast:asset:query") || SecurityUtils.hasPermi("cat:table:query");
    }

    Map<String, Object> describe(String table, AskUnifiedReqVO request, AgentToolContext context) {
        if (!available() || !Objects.equals(SecurityUtils.getUserId(), context.getUserId()) || !resources.hasSpace(context))
            throw new AgentToolRejection(AgentToolRejection.Reason.RESOURCE_DENIED);
        if (StringUtils.isBlank(table) || table.length() > 128 || !table.equals(table.trim()))
            throw new IllegalArgumentException("需要准确的表名称");
        Long datasourceId = ontologyContexts.resolveDatasource(request.getOntologyId(), request.getDatasourceId(),
                context.getSpaceId(), context.getSpaceCode(), context.getUserId(), true);
        if (datasourceId == null || !resources.datasourceVisible(datasourceId, context))
            throw new ServiceException("当前数据源不可访问");

        var policy = flows.policy(context.getSpaceId(), context.getSpaceCode());
        boolean mayReadAsset = SecurityUtils.hasPermi("ast:asset:query");
        List<Map<String, String>> visible = new ArrayList<>();
        if (mayReadAsset || policy.isAssetRequired()) {
            var candidates = assets.selectList(Wrappers.<AssetsAssetDO>lambdaQuery()
                    .eq(AssetsAssetDO::getDatasourceId, datasourceId).eq(AssetsAssetDO::getTableName, table)
                    .orderByDesc(AssetsAssetDO::getId).last("LIMIT 16"));
            boolean authorizedAsset = false;
            for (var candidate : candidates) {
                if (Boolean.TRUE.equals(candidate.getDelFlag()) || request.getAssetId() != null
                        && !request.getAssetId().equals(candidate.getId())) continue;
                Long count = assetSpaces.selectCount(Wrappers.<AssetsAssetSpaceRelDO>lambdaQuery()
                        .eq(AssetsAssetSpaceRelDO::getAssetId, candidate.getId())
                        .eq(AssetsAssetSpaceRelDO::getSpaceId, context.getSpaceId())
                        .eq(AssetsAssetSpaceRelDO::getSpaceCode, context.getSpaceCode())
                        .eq(AssetsAssetSpaceRelDO::getValidFlag, true));
                if (count == null || count == 0) continue;
                authorizedAsset = true;
                if (mayReadAsset) visible.add(summary("ASSET", candidate.getName(), candidate.getTableComment()));
            }
            if (!authorizedAsset && (policy.isAssetRequired() || request.getAssetId() != null))
                throw new ServiceException("当前范围无可授权的资产定义");
        }
        // An explicit asset must remain the boundary even in spaces without mandatory asset governance.
        if (request.getAssetId() != null && visible.isEmpty()) throw new ServiceException("当前资产不可访问");
        checkTable(datasourceId, table, context);
        if (policy.isQualityRequired()) flows.assertQuality(context.getSpaceId(), context.getSpaceCode(), datasourceId, table);
        if (visible.isEmpty() && request.getAssetId() == null && SecurityUtils.hasPermi("cat:table:query")) {
            var rows = metadata.selectList(Wrappers.<CatalogTableDO>lambdaQuery()
                    .eq(CatalogTableDO::getDatasourceId, datasourceId).eq(CatalogTableDO::getTableName, table)
                    .eq(CatalogTableDO::getSpaceId, context.getSpaceId()).eq(CatalogTableDO::getSpaceCode, context.getSpaceCode())
                    .eq(CatalogTableDO::getValidFlag, true).eq(CatalogTableDO::getStatus, "1")
                    .orderByDesc(CatalogTableDO::getVersion, CatalogTableDO::getId).last("LIMIT 8"));
            for (var row : rows) visible.add(summary("METADATA", row.getTableName(), row.getTableComment()));
        }
        return Map.of("verification", "DEFINITION_ONLY", "definitions", visible);
    }

    private void checkTable(Long datasourceId, String table, AgentToolContext context) {
        AssetsTableGovernanceReqDTO scope = new AssetsTableGovernanceReqDTO();
        scope.setSpaceId(context.getSpaceId()); scope.setSpaceCode(context.getSpaceCode());
        scope.setDatasourceId(datasourceId); scope.setTableName(table); scope.setEntrance("AI_ASK_DATA");
        governance.checkTableAccess(scope);
    }

    private static Map<String, String> summary(String source, String name, String description) {
        return Map.of("source", source, "name", shortText(name), "description", shortText(description));
    }

    private static String shortText(String text) {
        String value = Objects.toString(AgentPayloadSanitizer.sanitizeValue(null, text), "");
        return value.length() <= 400 ? value : value.substring(0, 400) + "…";
    }
}
