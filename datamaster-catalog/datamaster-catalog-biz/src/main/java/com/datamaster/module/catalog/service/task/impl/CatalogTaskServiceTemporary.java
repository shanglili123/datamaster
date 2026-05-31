package com.datamaster.module.catalog.service.task.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 采集任务Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTaskServiceTemporary {
//    @Resource
//    private CatalogTaskMapper CatalogTaskMapper;
//
//    @Resource
//    private ICatalogTaskSchedulerService CatalogTaskSchedulerService;
//    @Resource
//    private ICatalogTaskScopeService CatalogTaskScopeService;
//    @Resource
//    @Lazy
//    private ICatalogDatasourceService CatalogDatasourceService;
//    @Resource
//    private ICatalogTaskInstanceService CatalogTaskInstanceService;
//    @Resource
//    private ICatalogTaskInstanceLogService CatalogTaskInstanceLogService;
//
//    @Autowired
//    @Lazy
//    private DataSourceFactory dataSourceFactory;
//
//    @Resource
//    @Lazy
//    private IRedisService redisService;
//
//    //外部api
//    @Resource
//    @Lazy
//    private ICatalogDbService CatalogDbApiService;
//    @Resource
//    private ICatalogTableService CatalogTableApiService;
//    @Resource
//    private ICatalogColumnService CatalogColumnApiService;
//
//    @Resource
//    private CatalogDbTxService CatalogDbTxService;
//    @Resource
//    private CatalogTableTxService CatalogTableTxService;
//
//
//    /**
//     * @param taskId
//     * @return
//     */
//    public boolean runDaDiscoveryTask(Long taskId) {
//
//        String redisKey = buildRunLockKey(taskId);
//        if (!acquireRunLock(redisKey)) {
//            throw new RuntimeException("历史任务未执行完毕，请稍后重试");
//        }
//        CatalogTaskRespVO task = loadTask(taskId);
//        try {
//            TableProcessResult tableProcessResult = executeTaskSafely(task);
//            return true;
//        } catch (Exception e) {
//            return false;
//        } finally {
//            finalizeTask(redisKey);
//        }
//    }
//
//
//    public CatalogTaskRespVO getCatalogTaskByIdNew(Long id) {
//        CatalogTaskRespVO bean = BeanUtils.toBean(CatalogTaskMapper.selectById(id), CatalogTaskRespVO.class);
//
//        CatalogTaskSchedulerDO scheduler = CatalogTaskSchedulerService.getCatalogTaskSchedulerBytaskId(id);
//        if (scheduler != null) {
//            bean.setCronExpression(scheduler.getCronExpression());
//            bean.setSchedulerStatus(scheduler.getStatus());
//        }
//
//        List<CatalogTaskScopeDO> CatalogTaskScopeDOS = CatalogTaskScopeService.getCatalogTaskScopeListBytaskId(id);
//        bean.setScopeSaveReqVOS(CatalogTaskScopeDOS);
//
//        CatalogDatasourceDO CatalogDatasourceById = CatalogDatasourceService.getMcDatasourceById(bean.getDatasourceId());
//        bean.setDatasourceDO(CatalogDatasourceById);
//
//        CatalogTaskInstanceDO CatalogTaskInstanceByTaskId = CatalogTaskInstanceService.getCatalogTaskInstanceByTaskId(id);
//        if (CatalogTaskInstanceByTaskId != null) {
//            bean.setLastExecuteTime(
//                    DateUtil.format(CatalogTaskInstanceByTaskId.getCreateTime(), "yyyy-MM-dd HH:mm:ss")
//            );
//        }
//
//        return bean;
//    }
//
//    private String buildRunLockKey(Long taskId) {
//        // 统一前缀，避免与其它模块 key 冲突
//        return "mc:taskTemporary:run:" + taskId;
//    }
//
//    private CatalogTaskRespVO loadTask(Long taskId) {
//        CatalogTaskRespVO task = this.getCatalogTaskByIdNew(taskId);
//        if (task == null) {
//            throw new DataQueryException("采集任务不存在，taskId=" + taskId);
//        }
//        return task;
//    }
//
//    private boolean acquireRunLock(String redisKey) {
//        String status = redisService.get(redisKey);
//        if (StringUtils.isNotBlank(status) && "1".equals(status)) {
//            return false;
//        }
//        redisService.set(redisKey, "1", 60 * 60 * 12);
//        return true;
//    }
//
//
//    private void finalizeTask(String redisKey) {
//        redisService.set(redisKey, "2", 300);
//    }
//
//    private CatalogDatasourceDO prepareDatasource(CatalogTaskRespVO task) {
//        Long datasourceId = task.getDatasourceId();
//        if (datasourceId == null) {
//            throw new DataQueryException("数据源ID为空");
//        }
//
//        CatalogDatasourceDO datasource;
//        try {
//            datasource = CatalogDatasourceService.getMcDatasourceById(datasourceId);
//        } catch (Exception e) {
//            throw e;
//        }
//
//        if (datasource == null) {
//            throw new DataQueryException("数据源详情信息查询失败");
//        }
//        return datasource;
//    }
//
//    /**
//     * 主流程
//     *
//     * @param task
//     */
//    private TableProcessResult executeTaskSafely(CatalogTaskRespVO task) {
//        CatalogDatasourceDO datasource = prepareDatasource(task);
//        Long taskId = task.getId();
//
//        // 1. 根据采集范围，获取“库级”范围
//        List<CatalogTaskScopeDO> databaseScopes;
//        if ("2".equalsIgnoreCase(task.getCollectionScope())) {
//            // 全量：实时从数据源加载数据库
//            databaseScopes = loadDatabaseScopesFromDatasource(task, datasource);
//        } else {
//            // 增量：直接使用任务配置的采集范围
//            databaseScopes = loadDatabaseScopesFromTask(task);
//        }
//
//        if (CollectionUtils.isEmpty(databaseScopes)) {
//            return null;
//        }
//
//        // 2. 库级比对（是否新增 / 变更 / 删除）
//        List<CatalogDbSaveReqVO> dbReqDTOList = compareAndRecordDatabaseScope(task, databaseScopes, datasource);
//
//        List<CatalogDbSaveReqVO> CatalogDbByTaskId = CatalogDbApiService.getCatalogDbByTaskId(taskId, "1");
//
//
//        Long addCount = 0L;
//        Long delCount = 0L;
//        Long updateCount = 0L;
//
//        Long totalCount = 0L;
//        Long successCount = 0L;
//
//        int dbIndex = 1;
//        // 3. 循环每个库
//        for (CatalogDbSaveReqVO dbScope : dbReqDTOList) {
//
//            CatalogDbSaveReqVO matchedDb = findMatchedDb(dbScope, datasource, CatalogDbByTaskId);
//            if (matchedDb == null) {
//                Long CatalogDbId = CatalogDbTxService.createDbAndCommit(dbScope);
//                dbScope.setId(CatalogDbId);
//            } else {
//                dbScope.setId(matchedDb.getId());
//            }
//
//            DbQueryContext dbQuery = createDbQueryForScope(datasource, dbScope, task);
//            try {
//                TableProcessResult tableProcessResult = executeSingleDatabase(dbQuery, task, dbScope, datasource);
//
//                if (tableProcessResult != null) {
//                    addCount = addCount + tableProcessResult.getAddCount();
//                    updateCount = updateCount + tableProcessResult.getUpdateCount();
//                    delCount = delCount + tableProcessResult.getDelCount();
//                    totalCount = totalCount + tableProcessResult.getTotalCount();
//                    successCount = successCount + tableProcessResult.getSuccessCount();
//                }
//            } finally {
//                closeDbQuerySafely(dbQuery, task, dbScope);
//            }
//        }
//        //目前不做删除
////        List<CatalogDbRespDTO> dbsOnlyInResp = findDbsOnlyInResp(databaseScopes, datasource, CatalogDbByTaskId);
////        if (CollectionUtils.isNotEmpty(dbsOnlyInResp)) {
////
////            List<Long> collect = CatalogDbByTaskId.stream().map(a -> a.getId())
////                    .collect(Collectors.toList());
////
////            List<CatalogTableRespDTO> CatalogTableByDbId = CatalogTableApiService.getCatalogTableByDbId(collect, "1");
////            if (CollectionUtils.isNotEmpty(CatalogTableByDbId)) {
////                delCount = delCount + CatalogTableByDbId.size();
////            }
////
////            List<Long> tableIds = CatalogTableByDbId.stream().map(a -> a.getId())
////                    .collect(Collectors.toList());
////            CatalogTableApiService.removeApiCatalogTable(tableIds, "1");
////
////            CatalogDbApiService.removeApiCatalogDbById(collect);
////        }
//        return new TableProcessResult(addCount, delCount, updateCount, totalCount, successCount);
//
//    }
//
//    private List<CatalogDbRespDTO> findDbsOnlyInResp(List<CatalogTaskScopeDO> databaseScopes,
//                                                CatalogDatasourceDO datasource,
//                                                List<CatalogDbRespDTO> CatalogDbByTaskId) {
//
//        List<CatalogDbRespDTO> result = new ArrayList<>();
//        if (CollectionUtils.isEmpty(CatalogDbByTaskId)) {
//            return result;
//        }
//
//        for (CatalogDbRespDTO resp : CatalogDbByTaskId) {
//            boolean exists = false;
//
//            if (CollectionUtils.isNotEmpty(databaseScopes)) {
//                for (CatalogTaskScopeDO scope : databaseScopes) {
//                    if (Objects.equals(resp.getIp(), datasource.getIp())
//                            && Objects.equals(resp.getPort(), datasource.getPort() == null ? null : datasource.getPort().intValue())
//                            && Objects.equals(resp.getDatasourceConfig(), datasource.getDatasourceConfig())
//                            && Objects.equals(resp.getDbType(), datasource.getDatasourceType())
//                            && Objects.equals(resp.getDbName(), scope.getDbName())
//                            && Objects.equals(resp.getSchemaName(), scope.getSchemaName())) {
//                        exists = true;
//                        break;
//                    }
//                }
//            }
//
//            if (!exists) {
//                result.add(resp);
//            }
//        }
//        return result;
//    }
//
//    private CatalogDbSaveReqVO findMatchedDb(CatalogDbSaveReqVO dbScope,
//                                      CatalogDatasourceDO datasource,
//                                      List<CatalogDbSaveReqVO> CatalogDbByTaskId) {
//
//        if (dbScope == null || CollectionUtils.isEmpty(CatalogDbByTaskId)) {
//            return null;
//        }
//
//        for (CatalogDbSaveReqVO resp : CatalogDbByTaskId) {
//            if (Objects.equals(resp.getIp(), datasource.getIp())
//                    && Objects.equals(resp.getPort(), datasource.getPort() == null ? null : datasource.getPort().intValue())
//                    && Objects.equals(resp.getDatasourceConfig(), datasource.getDatasourceConfig())
//                    && Objects.equals(resp.getDbType(), datasource.getDatasourceType())
//                    && Objects.equals(resp.getDbName(), dbScope.getDbName())
//                    && Objects.equals(resp.getSchemaName(), dbScope.getSchemaName())) {
//                return resp;
//            }
//        }
//        return null;
//    }
//
//    private DbQueryContext createDbQueryForScope(CatalogDatasourceDO datasource,
//                                                 CatalogDbReqDTO dbScope,
//                                                 CatalogTaskRespVO task) {
//        DbQueryProperty property = new DbQueryProperty(
//                datasource.getDatasourceType(),
//                datasource.getIp(),
//                datasource.getPort(),
//                datasource.getDatasourceConfig()
//        );
//
//        // PG / Kingbase 切库 + schema
//        if (DbType.KINGBASE8.getDb().equals(property.getDbType())
//                || DbType.POSTGRE_SQL.getDb().equals(property.getDbType())) {
//            property.setDbName(dbScope.getDbName());
//            property.setSid(dbScope.getSchemaName());
//        }
//
//        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
//        if (!dbQuery.valid()) {
//            throw new DataQueryException("数据库连接失败");
//        }
//
//        property.setDbName(dbScope.getDbName());
//        property.setSid(dbScope.getSchemaName());
//        return new DbQueryContext(dbQuery, property);
//    }
//
//    private TableProcessResult executeSingleDatabase(DbQueryContext dbQuery,
//                                                     CatalogTaskRespVO task,
//                                                     CatalogDbReqDTO dbScope,
//                                                     CatalogDatasourceDO datasource) {
//
//        Long taskId = task.getId();
//        // 1. 表列表（不再建连接）
//        List<DbTable> tables = loadTablesByDatabase(dbQuery, task, dbScope);
//        if (CollectionUtils.isEmpty(tables)) {
//            return null;
//        }
//        int size = tables.size();
//        List<CatalogTableRespDTO> tableRespDTOList = getCatalogTableById(task, dbScope);
//
//        // 2. 表级比对
//        List<CatalogTableReqDTO> CatalogTables = compareAndRecordTables(task, dbScope, tables);
//
//        List<DbColumn> columns =
//                loadColumnsByTable(dbQuery, task, dbScope);
//
//        Map<String, List<DbColumn>> tableColumnMap =
//                columns.stream()
//                        .collect(Collectors.groupingBy(DbColumn::getTableName));
//
//        List<CatalogColumnReqDTO> CatalogColumnReqDTOList = new ArrayList<>();
//        // 3. 表循环
//        for (CatalogTableReqDTO table : CatalogTables) {
//            List<DbColumn> dbColumns = tableColumnMap.get(table.getTableName());
//            if (CollectionUtils.isEmpty(dbColumns)) {
//                continue;
//            }
//
//            try {
//
//                TableProcessResult result =
//                        CatalogTableTxService.runInNewTx(() ->
//                                doProcessSingleTable(task, dbScope, table, tableRespDTOList, dbColumns)
//                        );
//
////                TableProcessResult result = doProcessSingleTable(task, instance, dbScope, table, tableRespDTOList, dbColumns);
//                //
//                if (result != null) {
//                    CatalogColumnReqDTOList.addAll(result.getCatalogColumnReqDTOList());
//                }
//
//            } catch (Exception e) {
//
//            }
//        }
//        if (CollectionUtils.isNotEmpty(CatalogColumnReqDTOList)) {
//            CatalogColumnApiService.createCatalogColumnList(CatalogColumnReqDTOList);
//        }
//
//        //目前不做删除
////        List<CatalogTableRespDTO> tablesOnlyInResp = findTablesOnlyInResp(CatalogTables, tableRespDTOList);
////        if (CollectionUtils.isNotEmpty(tablesOnlyInResp)) {
////
////            List<Long> collect = tablesOnlyInResp.stream().map(a -> a.getId())
////                    .collect(Collectors.toList());
////            CatalogTableApiService.removeApiCatalogTable(collect, "1");
////        }
//
//
//        return new TableProcessResult(0L, 0L, 0L, 0L, 0L);
//    }
//
//    private TableProcessResult doProcessSingleTable(CatalogTaskRespVO task,
//                                                    CatalogDbReqDTO dbScope,
//                                                    CatalogTableReqDTO table,
//                                                    List<CatalogTableRespDTO> tableRespDTOList,
//                                                    List<DbColumn> columns) {
//        if (CollectionUtils.isEmpty(columns)) {
//            return null;
//        }
//
//        List<CatalogColumnReqDTO> columnReqDTOS =
//                compareAndRecordColumns(task, dbScope, table, columns);
//
//
//        CatalogTableRespDTO matched =
//                findMatchedTable(table, tableRespDTOList);
//
//        if (matched != null) {
//
//
//            table.setId(matched.getId());
//
//            List<CatalogColumnRespDTO> CatalogColumnRespDTOList =
//                    getCatalogColumnByTaskId(table, dbScope);
//
//            boolean updated = isTableUpdated(
//                    table, matched, columnReqDTOS, CatalogColumnRespDTOList);
//
//            if (updated) {
//
//                CatalogTableApiService.updateCatalogTable(table);
//
//                removeCatalogColumn(table, dbScope);
//
//            } else {
//                return null;
//            }
//
//        } else {
//            Long CatalogTableId =
//                    CatalogTableApiService.createCatalogTable(table);
//
//            table.setId(CatalogTableId);
//        }
//
//        for (CatalogColumnReqDTO columnReqDTO : columnReqDTOS) {
//            columnReqDTO.setTableId(table.getId());
//        }
//
//
//        return new TableProcessResult(0L, 0L, 0L, columnReqDTOS);
//    }
//
//
//    private void removeCatalogColumn(CatalogTableReqDTO table, CatalogDbReqDTO dbScope) {
//
//        CatalogColumnReqDTO createReqVO = new CatalogColumnReqDTO();
//        createReqVO.setTaskId(table.getTaskId());
//        createReqVO.setTableId(table.getId());
//        createReqVO.setDataType("1");
//        CatalogColumnApiService.removeCatalogColumn(createReqVO);
//    }
//
//    private boolean isTableUpdated(CatalogTableReqDTO reqTable,
//                                   CatalogTableRespDTO respTable,
//                                   List<CatalogColumnReqDTO> reqColumns,
//                                   List<CatalogColumnRespDTO> respColumns) {
//
//        // 1️⃣ 表注释不一致 → 更新
//        String reqComment = StringUtils.defaultString(reqTable.getTableComment());
//        String respComment = StringUtils.defaultString(respTable.getTableComment());
//        if (!reqComment.equals(respComment)) {
//            return true;
//        }
//
//        // 2️⃣ 字段数量不一致 → 更新
//        int reqSize = reqColumns == null ? 0 : reqColumns.size();
//        int respSize = respColumns == null ? 0 : respColumns.size();
//        if (reqSize != respSize) {
//            return true;
//        }
//
//        // 3️⃣ 构建 respColumns 的 Map（columnName 唯一）
//        Map<String, CatalogColumnRespDTO> respColumnMap = new HashMap<>();
//        if (respColumns != null) {
//            for (CatalogColumnRespDTO respCol : respColumns) {
//                respColumnMap.put(respCol.getColumnName(), respCol);
//            }
//        }
//
//        // 4️⃣ 循环 reqColumns，逐字段判断
//        if (reqColumns != null) {
//            for (CatalogColumnReqDTO reqCol : reqColumns) {
//
//                CatalogColumnRespDTO respCol =
//                        respColumnMap.get(reqCol.getColumnName());
//
//                // 字段不存在 → 更新
//                if (respCol == null) {
//                    return true;
//                }
//
//                // 字段属性不一致 → 更新
//                if (isColumnUpdated(reqCol, respCol)) {
//                    return true;
//                }
//            }
//        }
//
//        // 全部一致
//        return false;
//    }
//
//    private boolean isColumnUpdated(CatalogColumnReqDTO req, CatalogColumnRespDTO resp) {
//
//        // String 类型：null == ""
//        if (!StringUtils.defaultString(req.getColumnComment())
//                .equals(StringUtils.defaultString(resp.getColumnComment()))) {
//            return true;
//        }
//
//        if (!StringUtils.defaultString(req.getColumnType())
//                .equals(StringUtils.defaultString(resp.getColumnType()))) {
//            return true;
//        }
//
//        // 数值类型：直接 Objects.equals
//        if (!Objects.equals(req.getColumnLength(), resp.getColumnLength())) {
//            return true;
//        }
//
//        if (!Objects.equals(req.getColumnPrecision(), resp.getColumnPrecision())) {
//            return true;
//        }
//
//        if (!Objects.equals(req.getColumnScale(), resp.getColumnScale())) {
//            return true;
//        }
//
//        // String 类型：null == ""
//        if (!StringUtils.defaultString(req.getDefaultValue())
//                .equals(StringUtils.defaultString(resp.getDefaultValue()))) {
//            return true;
//        }
//
//        if (!StringUtils.defaultString(req.getPkFlag())
//                .equals(StringUtils.defaultString(resp.getPkFlag()))) {
//            return true;
//        }
//
//        if (!StringUtils.defaultString(req.getFkFlag())
//                .equals(StringUtils.defaultString(resp.getFkFlag()))) {
//            return true;
//        }
//
//        if (!StringUtils.defaultString(req.getNullableFlag())
//                .equals(StringUtils.defaultString(resp.getNullableFlag()))) {
//            return true;
//        }
//
//        return false;
//    }
//
//    private List<CatalogColumnRespDTO> getCatalogColumnByTaskId(CatalogTableReqDTO table, CatalogDbReqDTO dbScope) {
//        CatalogColumnReqDTO createReqVO = new CatalogColumnReqDTO();
//        createReqVO.setTaskId(table.getTaskId());
//        createReqVO.setTableId(table.getId());
//        createReqVO.setDataType("1");
//        return CatalogColumnApiService.getCatalogColumnByTaskId(createReqVO);
//    }
//
//    private List<CatalogTableRespDTO> findTablesOnlyInResp(List<CatalogTableReqDTO> CatalogTables,
//                                                      List<CatalogTableRespDTO> tableRespDTOList) {
//
//        List<CatalogTableRespDTO> result = new ArrayList<>();
//        if (CollectionUtils.isEmpty(tableRespDTOList)) {
//            return result;
//        }
//
//        for (CatalogTableRespDTO resp : tableRespDTOList) {
//            boolean exists = false;
//            if (CollectionUtils.isNotEmpty(CatalogTables)) {
//                for (CatalogTableReqDTO req : CatalogTables) {
//                    if (Objects.equals(req.getDbName(), resp.getDbName())
//                            && Objects.equals(req.getSchemaName(), resp.getSchemaName())
//                            && Objects.equals(req.getTableName(), resp.getTableName())) {
//                        exists = true;
//                        break;
//                    }
//                }
//            }
//            if (!exists) {
//                result.add(resp);
//            }
//        }
//        return result;
//    }
//
//
//    private CatalogTableRespDTO findMatchedTable(CatalogTableReqDTO req,
//                                            List<CatalogTableRespDTO> tableRespDTOList) {
//        if (req == null || CollectionUtils.isEmpty(tableRespDTOList)) {
//            return null;
//        }
//
//        for (CatalogTableRespDTO resp : tableRespDTOList) {
//            if (Objects.equals(req.getDbId(), resp.getDbId())
//                    && Objects.equals(req.getTableName(), resp.getTableName())) {
//                return resp;
//            }
//        }
//        return null;
//    }
//
//
//    private List<CatalogTableRespDTO> getCatalogTableById(CatalogTaskRespVO task, CatalogDbReqDTO dbScope) {
//        CatalogTableReqDTO CatalogTableReqDTO = new CatalogTableReqDTO();
//        CatalogTableReqDTO.setTaskId(task.getId());
//        CatalogTableReqDTO.setDbId(dbScope.getId());
//        CatalogTableReqDTO.setDataType("1");
//        return CatalogTableApiService.getCatalogTableById(CatalogTableReqDTO);
//    }
//
//    private List<DbTable> loadTablesByDatabase(DbQueryContext dbQuery,
//                                               CatalogTaskRespVO task,
//                                               CatalogDbReqDTO dbScope) {
//
//        try {
//            List<DbTable> tables = dbQuery.getDbQuery().getTables(dbQuery.getProperty());
//            return tables == null ? new ArrayList<>() : tables;
//        } catch (Exception e) {
//            return new ArrayList<>();
//        }
//    }
//
//    private List<DbColumn> loadColumnsByTable(DbQueryContext dbQuery,
//                                              CatalogTaskRespVO task,
//                                              CatalogDbReqDTO dbScope) {
//
//        try {
//            List<DbColumn> tableColumns = dbQuery.getDbQuery().getDbColumns(dbQuery.getProperty());
//            return tableColumns == null ? new ArrayList<>() : tableColumns;
//        } catch (Exception e) {
//            return new ArrayList<>();
//        }
//    }
//
//    private void closeDbQuerySafely(DbQueryContext ctx,
//                                    CatalogTaskRespVO task,
//                                    CatalogDbReqDTO dbScope) {
//        try {
//            ctx.getDbQuery().close();
//        } catch (Exception e) {
//        }
//    }
//
//
//    private List<CatalogTaskScopeDO> loadDatabaseScopesFromDatasource(CatalogTaskRespVO task,
//                                                                 CatalogDatasourceDO datasource) {
//        // 1. 构建 DbQueryProperty
//        DbQueryProperty baseProperty = new DbQueryProperty(
//                datasource.getDatasourceType(),
//                datasource.getIp(),
//                datasource.getPort(),
//                datasource.getDatasourceConfig()
//        );
//
//        // 2. 获取数据库列表（含层级）
//        List<DbName> dbNames;
//        DbQuery rootQuery = dataSourceFactory.createDbQuery(baseProperty);
//        try {
//            if (!rootQuery.valid()) {
//                throw new DataQueryException("数据库连接失败");
//            }
//            dbNames = rootQuery.getDbNames(null);
//        } finally {
//            rootQuery.close();
//        }
//
//        List<CatalogTaskScopeDO> scopeList = new ArrayList<>();
//        if (CollectionUtils.isEmpty(dbNames)) {
//            return scopeList;
//        }
//
//        // 3. 单层结构：直接映射 dbName
//        if (dbNames.get(0).getLevel() == 1 && dbNames.get(0).getTotalLevels() == 1) {
//            for (DbName dbName : dbNames) {
//                CatalogTaskScopeDO scope = new CatalogTaskScopeDO();
//                scope.setDbName(dbName.getDbName());
//                scopeList.add(scope);
//            }
//            return scopeList;
//        }
//
//        // 4. 多层结构：加载下级并映射 db + schema
//        for (DbName dbName : dbNames) {
//
//            DbQueryProperty childProperty = baseProperty;
//            if (DbType.KINGBASE8.getDb().equals(baseProperty.getDbType())
//                    || DbType.POSTGRE_SQL.getDb().equals(baseProperty.getDbType())) {
//
//                childProperty = baseProperty.copy();
//                childProperty.setDbName(dbName.getDbName());
//            }
//
//            DbQuery childQuery = dataSourceFactory.createDbQuery(childProperty);
//            try {
//                if (!childQuery.valid()) {
//                    continue;
//                }
//                List<DbName> children = childQuery.getDbNames(dbName);
//                dbName.setChildren(children);
//            } finally {
//                childQuery.close();
//            }
//
//            List<DbName> children = dbName.getChildren();
//            if (CollectionUtils.isNotEmpty(children)) {
//                for (DbName child : children) {
//                    CatalogTaskScopeDO scope = new CatalogTaskScopeDO();
//                    scope.setDbName(dbName.getDbName());
//                    scope.setSchemaName(child.getDbName());
//                    scopeList.add(scope);
//                }
//            } else {
//                CatalogTaskScopeDO scope = new CatalogTaskScopeDO();
//                scope.setDbName(dbName.getDbName());
//                scopeList.add(scope);
//            }
//        }
//
//        return scopeList;
//    }
//
//
//    private List<CatalogTaskScopeDO> loadDatabaseScopesFromTask(CatalogTaskRespVO task) {
//        return task.getScopeSaveReqVOS();
//    }
//
//    private List<CatalogDbSaveReqVO> compareAndRecordDatabaseScope(CatalogTaskRespVO task, List<CatalogTaskScopeDO> databaseScopes,
//                                                           CatalogDatasourceDO datasource) {
//        List<CatalogDbSaveReqVO> dbReqDTOList = new ArrayList<>();
//
//        //TODO 逻辑待完善
//        for (CatalogTaskScopeDO databaseScope : databaseScopes) {
//
//            CatalogDbSaveReqVO createReqVO = new CatalogDbSaveReqVO();
//            //采集标识
//            createReqVO.setTaskId(task.getId());
//
//            // ====== 业务域 ======
//            createReqVO.setDomainId(task.getDomainId());
//            createReqVO.setDomainCode(task.getDomainCode());
//
//            // ====== 数据源基础信息 ======
//            createReqVO.setDatasourceId(datasource.getId());
//            createReqVO.setDbType(datasource.getDatasourceType());
//            createReqVO.setIp(datasource.getIp());
//            createReqVO.setPort(datasource.getPort() != null
//                    ? datasource.getPort().intValue()
//                    : null);
//            createReqVO.setDatasourceConfig(datasource.getDatasourceConfig());
//
//            // ====== 库 / 模式 ======
//            createReqVO.setDbName(databaseScope.getDbName());
//            createReqVO.setSchemaName(databaseScope.getSchemaName());
//
//            // ====== 描述 ======
//            createReqVO.setDescription(databaseScope.getDescription());
//
//            createReqVO.setCreateBy("System Collection Task");
//            createReqVO.setCreatorId(1L);
//
//            // ====== 状态与标志位（后端可统一兜底，这里显式给） ======
//            createReqVO.setStatus("0");      // 未发布
//            createReqVO.setAuditStatus("2");
//            createReqVO.setVersion(1);
//            createReqVO.setAuditTime(new Date());
//
//            dbReqDTOList.add(createReqVO);
//        }
//
//        return dbReqDTOList;
//    }
//
//    private List<CatalogTableReqDTO> compareAndRecordTables(CatalogTaskRespVO task,
//                                                       CatalogDbReqDTO dbScope,
//                                                       List<DbTable> tables) {
//        List<CatalogTableReqDTO> CatalogTableReqDTOList = new ArrayList<>();
//        for (DbTable table : tables) {
//
//            CatalogTableReqDTO CatalogTableReqDTO = new CatalogTableReqDTO();
//
//            // ====== 关联关系 ======
//            CatalogTableReqDTO.setDataType("1");
//            CatalogTableReqDTO.setTaskId(task.getId());
//            CatalogTableReqDTO.setDbId(dbScope.getId());
//            CatalogTableReqDTO.setDatasourceId(task.getDatasourceId());
//
//            // ====== 表基础信息 ======
//            CatalogTableReqDTO.setTableName(table.getTableName());
//            CatalogTableReqDTO.setTableComment(StringUtils.isEmpty(table.getTableComment()) ? "" : table.getTableComment());
//
//            // ====== 库 / 模式 ======
//            CatalogTableReqDTO.setDbName(dbScope.getDbName());
//            CatalogTableReqDTO.setSchemaName(dbScope.getSchemaName());
//
//            CatalogTableReqDTO.setCreateBy("System Collection Task");
//            CatalogTableReqDTO.setCreatorId(1L);
//
//            // ====== 状态与标志位 ======
//            CatalogTableReqDTO.setStatus("0");     // 未发布
//            CatalogTableReqDTO.setVersion(1);
//            CatalogTableReqDTO.setMasterFlag("1");
//            CatalogTableReqDTO.setTempFlag("0");
//            CatalogTableReqDTO.setAuditStatus("2");
//            CatalogTableReqDTO.setAuditTime(new Date());
//            CatalogTableReqDTO.setValidFlag(true);
//            CatalogTableReqDTO.setDelFlag(false);
//
//            // ====== 描述 ======
//            CatalogTableReqDTO.setDescription(table.getTableComment());
//
//            // ====== 调用元数据服务 ======
////            Long CatalogTableId = CatalogTableApiService.createCatalogTable(CatalogTableReqDTO);
////
////            CatalogTableReqDTO.setId(CatalogTableId);
//            CatalogTableReqDTOList.add(CatalogTableReqDTO);
//        }
//
//        return CatalogTableReqDTOList;
//    }
//
//    private List<CatalogColumnReqDTO> compareAndRecordColumns(CatalogTaskRespVO task,
//                                                         CatalogDbReqDTO dbScope,
//                                                         CatalogTableReqDTO table,
//                                                         List<DbColumn> columns) {
//
//        List<CatalogColumnReqDTO> columnReqDTOS = new ArrayList<>();
//        for (DbColumn column : columns) {
//
//            CatalogColumnReqDTO createReqVO = new CatalogColumnReqDTO();
//
//            // ====== 关联信息 ======
//            createReqVO.setTaskId(task.getId());
//            createReqVO.setDbId(dbScope.getId());
//            createReqVO.setTableId(table.getId());
//            createReqVO.setDatasourceId(task.getDatasourceId());
//
//            // ====== 字段基础信息 ======
//            createReqVO.setColumnName(column.getColName());
//            createReqVO.setColumnComment(StringUtils.isEmpty(column.getColName()) ? "" : column.getColName());
//            createReqVO.setColumnType(column.getDataType());
//
//            // ====== 长度 / 精度 ======
//            createReqVO.setColumnLength(parseInt(column.getDataLength()));
//            createReqVO.setColumnPrecision(parseInt(column.getDataPrecision()));
//            createReqVO.setColumnScale(parseInt(column.getDataScale()));
//
//            // ====== 默认值 ======
//            createReqVO.setDefaultValue(column.getDataDefault());
//
//            // ====== 主键 / 可空 ======
//            createReqVO.setPkFlag(Boolean.TRUE.equals(column.getColKey()) ? "1" : "0");
//            createReqVO.setNullableFlag(Boolean.FALSE.equals(column.getNullable()) ? "1" : "0");
//            createReqVO.setFkFlag("0");
//
//            createReqVO.setCreateBy("System Collection Task");
//            createReqVO.setCreatorId(1L);
//
//            // ====== 状态与标志位 ======
//            createReqVO.setStatus("0");     // 未发布
//            createReqVO.setValidFlag(true);
//            createReqVO.setDelFlag(false);
//            createReqVO.setVersion(1);
//            createReqVO.setDataType("1");
//            createReqVO.setAuditStatus("2");
//            createReqVO.setAuditTime(new Date());
//
//            // ====== 描述 ======
//            createReqVO.setDescription(column.getColComment());
//
//
//            columnReqDTOS.add(createReqVO);
//            // ====== 调用字段元数据服务 ======
//
//            // 如需回写 columnId，可在 DbColumn 中扩展字段
//        }
//        return columnReqDTOS;
//    }
//
//    /**
//     * 安全的 String -> Integer 转换
//     */
//    private Integer parseInt(String val) {
//        if (val == null || val.trim().isEmpty()) {
//            return null;
//        }
//        try {
//            return Integer.valueOf(val.trim());
//        } catch (NumberFormatException e) {
//            return null;
//        }
//    }
}


