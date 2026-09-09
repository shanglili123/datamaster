package com.datamaster.module.assets.service.asset.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetRespVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSyncReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.mapper.assetColumn.AssetsAssetColumnMapper;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.asset.IAssetsAssetSyncService;
import com.datamaster.module.assets.service.assetColumn.IAssetsAssetColumnService;
import com.datamaster.metadata.api.column.dto.CatalogColumnRespDTO;
import com.datamaster.metadata.api.service.column.CatalogColumnApiService;
import com.datamaster.metadata.api.service.table.CatalogTableApiService;
import com.datamaster.metadata.api.table.dto.CatalogTableRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产元数据同步 Service 实现
 *
 * 从当前元数据目录（CAT_TABLE / CAT_COLUMN）同步到资产，不自行实时取数。
 * 新建资产状态沿用目录表状态；已有资产不覆盖其发布状态。
 * 列级更新使用 syncUpdateColumnMetadata 仅刷新字段元数据，保留
 * DATA_ELEM_CODE_ID、SENSITIVE_LEVEL_ID 等扩展关联字段。
 */
@Slf4j
@Service
public class AssetsAssetSyncServiceImpl implements IAssetsAssetSyncService {

    @Resource
    private IAssetsAssetService assetsAssetService;
    @Resource
    private IAssetsAssetColumnService assetsAssetColumnService;
    @Resource
    private AssetsAssetColumnMapper assetsAssetColumnMapper;
    @Resource
    private CatalogTableApiService catalogTableApiService;
    @Resource
    private CatalogColumnApiService catalogColumnApiService;

    @Override
    public AjaxResult sync(AssetsAssetSyncReqVO reqVO) {
        List<CatalogTableRespDTO> catalogTables = resolveCatalogTables(reqVO);
        if (CollectionUtils.isEmpty(catalogTables)) {
            return AjaxResult.success("未发现可同步的元数据");
        }
        String catCode = resolveSyncCatCode(reqVO);
        SyncStat stat = doSyncCatalog(catalogTables, catCode);
        return AjaxResult.success(stat.toMessage());
    }

    private String resolveSyncCatCode(AssetsAssetSyncReqVO reqVO) {
        if (reqVO.getAssetId() != null) {
            return null;
        }
        if (StringUtils.isBlank(reqVO.getCatCode())) {
            throw new ServiceException("请选择资产目录");
        }
        return reqVO.getCatCode().trim();
    }

    private List<CatalogTableRespDTO> resolveCatalogTables(AssetsAssetSyncReqVO reqVO) {
        if (reqVO.getAssetId() != null) {
            AssetsAssetRespVO asset = assetsAssetService.getAssetByIdSimple(reqVO.getAssetId());
            if (asset == null || asset.getDatasourceId() == null || StringUtils.isEmpty(asset.getTableName())) {
                throw new ServiceException("资产不存在或未关联元数据表");
            }
            CatalogTableRespDTO table = asset.getTableId() == null
                    ? catalogTableApiService.getByDatasourceIdAndTableName(
                    asset.getDatasourceId(), asset.getTableName())
                    : catalogTableApiService.getById(asset.getTableId());
            if (table == null) {
                throw new ServiceException("资产关联的目录元数据不存在");
            }
            List<CatalogTableRespDTO> result = new ArrayList<>();
            result.add(table);
            return result;
        }
        if (reqVO.getDatasourceId() == null || StringUtils.isBlank(reqVO.getDatabaseName())) {
            throw new ServiceException("请选择需要同步的元数据库");
        }
        return catalogTableApiService.listByDatasourceAndDatabase(
                reqVO.getDatasourceId(), reqVO.getDatabaseName(), reqVO.getSchemaName());
    }

    private SyncStat doSyncCatalog(List<CatalogTableRespDTO> tables, String catCode) {
        SyncStat stat = new SyncStat();
        for (CatalogTableRespDTO table : tables) {
            if (table == null || table.getDatasourceId() == null || StringUtils.isBlank(table.getTableName())) {
                continue;
            }
            try {
                boolean existed = syncCatalogTable(table, catCode);
                if (existed) {
                    stat.updated++;
                } else {
                    stat.created++;
                }
            } catch (Exception e) {
                stat.failed++;
                stat.errors.add(table.getTableName() + ": " + e.getMessage());
                log.error("从目录元数据同步资产失败，表：{}", table.getTableName(), e);
            }
        }
        return stat;
    }

    private boolean syncCatalogTable(CatalogTableRespDTO table, String syncCatCode) {
        AssetsAssetDO asset = findCatalogAsset(table);
        boolean existed = asset != null;
        if (!existed) {
            asset = new AssetsAssetDO();
            asset.setType("1");
            asset.setSource("1");
            asset.setStatus(StringUtils.isNotBlank(table.getStatus()) ? table.getStatus() : "1");
            asset.setCatCode(syncCatCode);
        }
        asset.setTableId(table.getId());
        asset.setName(StringUtils.isNotBlank(table.getTableComment())
                ? table.getTableComment() : table.getTableName());
        asset.setDatasourceId(table.getDatasourceId());
        asset.setTableName(table.getTableName());
        asset.setTableComment(table.getTableComment());
        asset.setDataCount(table.getRowCount());
        asset.setFieldCount(table.getColumnCount());
        if (existed) {
            assetsAssetService.updateById(asset);
        } else {
            assetsAssetService.save(asset);
        }
        syncCatalogColumns(asset, table);
        return existed;
    }

    private AssetsAssetDO findCatalogAsset(CatalogTableRespDTO table) {
        if (table.getId() != null) {
            AssetsAssetDO exact = assetsAssetService.getOne(Wrappers.<AssetsAssetDO>lambdaQuery()
                    .eq(AssetsAssetDO::getTableId, table.getId())
                    .last("limit 1"));
            if (exact != null) {
                return exact;
            }
        }
        List<AssetsAssetDO> candidates = assetsAssetService.getAssetByDataSourceId(
                table.getDatasourceId(), table.getTableName());
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }
        for (AssetsAssetDO candidate : candidates) {
            if (candidate.getTableId() == null) {
                return candidate;
            }
            CatalogTableRespDTO boundTable = catalogTableApiService.getById(candidate.getTableId());
            if (boundTable != null
                    && Objects.equals(boundTable.getDatasourceId(), table.getDatasourceId())
                    && StringUtils.equalsIgnoreCase(boundTable.getDbName(), table.getDbName())
                    && StringUtils.equalsIgnoreCase(StringUtils.defaultString(boundTable.getSchemaName()),
                    StringUtils.defaultString(table.getSchemaName()))) {
                return candidate;
            }
        }
        return null;
    }

    private void syncCatalogColumns(AssetsAssetDO asset, CatalogTableRespDTO table) {
        List<CatalogColumnRespDTO> catalogColumns = catalogColumnApiService.listByTableId(table.getId());
        AssetsAssetColumnPageReqVO assetColumnReq = new AssetsAssetColumnPageReqVO();
        assetColumnReq.setAssetId(asset.getId());
        List<AssetsAssetColumnDO> existingColumns = assetsAssetColumnService.getAssetColumnList(assetColumnReq);
        Map<String, AssetsAssetColumnDO> existingByName = existingColumns.stream()
                .filter(column -> column.getColumnName() != null)
                .collect(Collectors.toMap(AssetsAssetColumnDO::getColumnName, column -> column, (a, b) -> a));

        Set<String> catalogNames = new HashSet<>();
        for (CatalogColumnRespDTO catalogColumn : catalogColumns) {
            if (StringUtils.isBlank(catalogColumn.getColumnName())) {
                continue;
            }
            catalogNames.add(catalogColumn.getColumnName());
            AssetsAssetColumnDO existing = existingByName.get(catalogColumn.getColumnName());
            if (existing != null) {
                AssetsAssetColumnDO update = new AssetsAssetColumnDO();
                update.setId(existing.getId());
                update.setAssetId(asset.getId());
                update.setColumnName(catalogColumn.getColumnName());
                update.setColumnComment(catalogColumn.getColumnComment());
                update.setColumnType(catalogColumn.getColumnType());
                update.setColumnLength(catalogColumn.getColumnLength() == null
                        ? null : catalogColumn.getColumnLength().longValue());
                update.setColumnScale(catalogColumn.getColumnScale() == null
                        ? null : catalogColumn.getColumnScale().longValue());
                update.setNullableFlag(catalogColumn.getNullableFlag());
                update.setPkFlag(catalogColumn.getPkFlag());
                update.setDefaultValue(catalogColumn.getDefaultValue());
                assetsAssetColumnMapper.syncUpdateColumnMetadata(update);
            } else {
                AssetsAssetColumnSaveReqVO save = new AssetsAssetColumnSaveReqVO();
                save.setAssetId(asset.getId());
                save.setColumnName(catalogColumn.getColumnName());
                save.setColumnComment(catalogColumn.getColumnComment());
                save.setColumnType(catalogColumn.getColumnType());
                save.setColumnLength(catalogColumn.getColumnLength() == null
                        ? null : catalogColumn.getColumnLength().longValue());
                save.setColumnScale(catalogColumn.getColumnScale() == null
                        ? null : catalogColumn.getColumnScale().longValue());
                save.setNullableFlag(catalogColumn.getNullableFlag());
                save.setPkFlag(catalogColumn.getPkFlag());
                save.setDefaultValue(catalogColumn.getDefaultValue());
                assetsAssetColumnService.createAssetColumn(save);
            }
        }
        List<Long> removedIds = existingColumns.stream()
                .filter(column -> column.getColumnName() != null && !catalogNames.contains(column.getColumnName()))
                .map(AssetsAssetColumnDO::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(removedIds)) {
            assetsAssetColumnService.removeAssetColumn(removedIds);
        }
        if (table.getColumnCount() == null || !Objects.equals(table.getColumnCount(), (long) catalogColumns.size())) {
            AssetsAssetDO countUpdate = new AssetsAssetDO();
            countUpdate.setId(asset.getId());
            countUpdate.setFieldCount((long) catalogColumns.size());
            assetsAssetService.updateById(countUpdate);
        }
    }

    private static class SyncStat {
        private int created = 0;
        private int updated = 0;
        private int failed = 0;
        private final List<String> errors = new ArrayList<>();

        private String toMessage() {
            StringBuilder message = new StringBuilder();
            message.append("同步完成：新增资产 ").append(created).append(" 个，更新资产 ").append(updated).append(" 个");
            if (failed > 0) {
                message.append("，失败 ").append(failed).append(" 个");
            }
            if (!errors.isEmpty()) {
                message.append("（").append(String.join("；", errors)).append("）");
            }
            return message.toString();
        }
    }
}
