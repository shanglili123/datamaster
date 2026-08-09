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
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryColumnPageReqVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTablePageReqVO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryColumnDO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTableDO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTaskDO;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryColumnService;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryTableService;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产元数据同步 Service 实现
 *
 * 同步 = 从探查结果（AST_DISCOVERY_TABLE / AST_DISCOVERY_COLUMN）同步到资产，
 * 不自行实时取数。同步所有未被忽略（ignoreFlag!='1'）的表。
 * 新建资产状态沿用探查表状态（待提交=1/已提交=2）；已有资产不覆盖其发布状态。
 * 列级更新使用 syncUpdateColumnMetadata 仅刷新字段元数据，保留
 * DATA_ELEM_CODE_ID、SENSITIVE_LEVEL_ID 等扩展关联字段。
 */
@Slf4j
@Service
public class AssetsAssetSyncServiceImpl implements IAssetsAssetSyncService {

    /** 探查表状态：已提交 */
    private static final String DISCOVERY_TABLE_STATUS_COMMITTED = "2";
    /** 探查表忽略标志：忽略 */
    private static final String IGNORE_YES = "1";

    @Resource
    private IAssetsAssetService assetsAssetService;
    @Resource
    private IAssetsAssetColumnService assetsAssetColumnService;
    @Resource
    private AssetsAssetColumnMapper assetsAssetColumnMapper;
    @Resource
    private IAssetsDiscoveryTableService discoveryTableService;
    @Resource
    private IAssetsDiscoveryColumnService discoveryColumnService;
    @Resource
    private IAssetsDiscoveryTaskService discoveryTaskService;

    @Override
    public AjaxResult sync(AssetsAssetSyncReqVO reqVO) {
        List<AssetsDiscoveryTableDO> tables = resolveDiscoveryTables(reqVO);
        if (CollectionUtils.isEmpty(tables)) {
            return AjaxResult.success("未发现可同步的元数据");
        }
        SyncStat stat = doSync(tables);
        return AjaxResult.success(stat.toMessage());
    }

    @Scheduled(cron = "0 * * * * ?")
    public void syncByScheduled() {
        try {
            List<AssetsDiscoveryTaskDO> tasks = discoveryTaskService.getDaDiscoveryTaskList();
            if (CollectionUtils.isEmpty(tasks)) {
                return;
            }
            int totalCreated = 0;
            int totalUpdated = 0;
            for (AssetsDiscoveryTaskDO task : tasks) {
                if (StringUtils.isEmpty(task.getCatCode())) {
                    continue;
                }
                AssetsDiscoveryTablePageReqVO tableReq = new AssetsDiscoveryTablePageReqVO();
                tableReq.setTaskId(task.getId());
                List<AssetsDiscoveryTableDO> tables = discoveryTableService.getDaDiscoveryTableList(tableReq);
                if (CollectionUtils.isEmpty(tables)) {
                    continue;
                }
                SyncStat stat = doSync(tables);
                totalCreated += stat.created;
                totalUpdated += stat.updated;
            }
            if (totalCreated > 0 || totalUpdated > 0) {
                log.info("定时同步资产元数据完成，新增：{}，更新：{}", totalCreated, totalUpdated);
            }
        } catch (Exception e) {
            log.error("定时同步资产元数据失败", e);
        }
    }

    private List<AssetsDiscoveryTableDO> resolveDiscoveryTables(AssetsAssetSyncReqVO reqVO) {
        AssetsDiscoveryTablePageReqVO tableReq = new AssetsDiscoveryTablePageReqVO();
        if (reqVO.getTaskId() != null) {
            tableReq.setTaskId(reqVO.getTaskId());
            return discoveryTableService.getDaDiscoveryTableList(tableReq);
        }
        if (reqVO.getDatasourceId() != null) {
            tableReq.setDatasourceId(reqVO.getDatasourceId());
            return discoveryTableService.getDaDiscoveryTableList(tableReq);
        }
        if (reqVO.getAssetId() != null) {
            AssetsAssetRespVO asset = assetsAssetService.getAssetByIdSimple(reqVO.getAssetId());
            if (asset == null || asset.getDatasourceId() == null || StringUtils.isEmpty(asset.getTableName())) {
                throw new ServiceException("资产不存在或未关联元数据表");
            }
            return discoveryTableService.list(Wrappers.<AssetsDiscoveryTableDO>lambdaQuery()
                    .eq(AssetsDiscoveryTableDO::getDatasourceId, asset.getDatasourceId())
                    .eq(AssetsDiscoveryTableDO::getTableName, asset.getTableName()));
        }
        return discoveryTableService.getDaDiscoveryTableList(tableReq);
    }

    private SyncStat doSync(List<AssetsDiscoveryTableDO> tables) {
        SyncStat stat = new SyncStat();
        Map<Long, AssetsDiscoveryTaskDO> taskMap = loadTaskMap(tables);
        for (AssetsDiscoveryTableDO table : tables) {
            if (IGNORE_YES.equals(table.getIgnoreFlag())) {
                continue;
            }
            AssetsDiscoveryTaskDO task = taskMap.get(table.getTaskId());
            if (task == null || task.getDatasourceId() == null) {
                continue;
            }
            try {
                boolean existed = syncTable(table, task);
                if (existed) {
                    stat.updated++;
                } else {
                    stat.created++;
                }
            } catch (Exception e) {
                stat.failed++;
                stat.errors.add(table.getTableName() + ": " + e.getMessage());
                log.error("同步资产元数据失败，表：{}", table.getTableName(), e);
            }
        }
        return stat;
    }

    private Map<Long, AssetsDiscoveryTaskDO> loadTaskMap(List<AssetsDiscoveryTableDO> tables) {
        Set<Long> taskIds = tables.stream()
                .map(AssetsDiscoveryTableDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AssetsDiscoveryTaskDO> taskMap = new HashMap<>();
        if (taskIds.isEmpty()) {
            return taskMap;
        }
        List<AssetsDiscoveryTaskDO> tasks = discoveryTaskService.list(
                Wrappers.<AssetsDiscoveryTaskDO>lambdaQuery().in(AssetsDiscoveryTaskDO::getId, taskIds));
        for (AssetsDiscoveryTaskDO task : tasks) {
            taskMap.put(task.getId(), task);
        }
        return taskMap;
    }

    /**
     * 同步单张表：生成缺失资产或更新已有资产，并同步列元数据。
     *
     * @return true=更新已有资产；false=新建资产
     */
    private boolean syncTable(AssetsDiscoveryTableDO table, AssetsDiscoveryTaskDO task) {
        Long datasourceId = task.getDatasourceId();
        List<AssetsAssetDO> existingList = assetsAssetService.getAssetByDataSourceId(datasourceId, table.getTableName());
        boolean existed = CollectionUtils.isNotEmpty(existingList);
        AssetsAssetDO asset;
        if (existed) {
            asset = existingList.get(0);
        } else {
            asset = new AssetsAssetDO();
            asset.setType("1");
            asset.setSource("1");
            asset.setStatus(StringUtils.isNotEmpty(table.getStatus())
                    ? table.getStatus()
                    : DISCOVERY_TABLE_STATUS_COMMITTED);
        }
        asset.setName(StringUtils.isNotEmpty(table.getTableComment()) ? table.getTableComment() : table.getTableName());
        asset.setCatCode(task.getCatCode());
        asset.setDatasourceId(datasourceId);
        asset.setTableName(table.getTableName());
        asset.setTableComment(table.getTableComment());
        asset.setDataCount(table.getDataCount());
        asset.setFieldCount(table.getFieldCount());
        if (existed) {
            assetsAssetService.updateById(asset);
        } else {
            assetsAssetService.save(asset);
        }
        syncColumns(asset, table);
        return existed;
    }

    private void syncColumns(AssetsAssetDO asset, AssetsDiscoveryTableDO table) {
        AssetsDiscoveryColumnPageReqVO columnReq = new AssetsDiscoveryColumnPageReqVO();
        columnReq.setTableId(table.getId());
        List<AssetsDiscoveryColumnDO> discoveryColumns = discoveryColumnService.getDaDiscoveryColumnList(columnReq);

        AssetsAssetColumnPageReqVO assetColumnReq = new AssetsAssetColumnPageReqVO();
        assetColumnReq.setAssetId(asset.getId());
        List<AssetsAssetColumnDO> existingColumns = assetsAssetColumnService.getAssetColumnList(assetColumnReq);
        Map<String, AssetsAssetColumnDO> existingByName = existingColumns.stream()
                .filter(columnDO -> columnDO.getColumnName() != null)
                .collect(Collectors.toMap(AssetsAssetColumnDO::getColumnName, columnDO -> columnDO, (a, b) -> a));

        Set<String> discoveryNames = new HashSet<>();
        for (AssetsDiscoveryColumnDO discoveryColumn : discoveryColumns) {
            if (StringUtils.isEmpty(discoveryColumn.getColumnName())) {
                continue;
            }
            discoveryNames.add(discoveryColumn.getColumnName());
            AssetsAssetColumnDO existing = existingByName.get(discoveryColumn.getColumnName());
            if (existing != null) {
                AssetsAssetColumnDO update = new AssetsAssetColumnDO();
                update.setId(existing.getId());
                update.setAssetId(asset.getId());
                update.setColumnName(discoveryColumn.getColumnName());
                update.setColumnComment(discoveryColumn.getColumnComment());
                update.setColumnType(discoveryColumn.getColumnType());
                update.setColumnLength(discoveryColumn.getColumnLength());
                update.setColumnScale(discoveryColumn.getColumnScale());
                update.setNullableFlag(discoveryColumn.getNullableFlag());
                update.setPkFlag(discoveryColumn.getPkFlag());
                update.setDefaultValue(discoveryColumn.getDefaultValue());
                assetsAssetColumnMapper.syncUpdateColumnMetadata(update);
            } else {
                AssetsAssetColumnSaveReqVO save = new AssetsAssetColumnSaveReqVO();
                save.setAssetId(asset.getId());
                save.setColumnName(discoveryColumn.getColumnName());
                save.setColumnComment(discoveryColumn.getColumnComment());
                save.setColumnType(discoveryColumn.getColumnType());
                save.setColumnLength(discoveryColumn.getColumnLength());
                save.setColumnScale(discoveryColumn.getColumnScale());
                save.setNullableFlag(discoveryColumn.getNullableFlag());
                save.setPkFlag(discoveryColumn.getPkFlag());
                save.setDefaultValue(discoveryColumn.getDefaultValue());
                assetsAssetColumnService.createAssetColumn(save);
            }
        }
        List<Long> removedIds = existingColumns.stream()
                .filter(columnDO -> columnDO.getColumnName() != null && !discoveryNames.contains(columnDO.getColumnName()))
                .map(AssetsAssetColumnDO::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(removedIds)) {
            assetsAssetColumnService.removeAssetColumn(removedIds);
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
