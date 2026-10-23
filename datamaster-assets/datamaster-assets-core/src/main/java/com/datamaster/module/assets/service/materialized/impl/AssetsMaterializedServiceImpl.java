package com.datamaster.module.assets.service.materialized.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSaveReqVO;
import com.datamaster.module.assets.controller.admin.materialized.vo.AssetsMaterializedReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.datasource.AssetsTableStructureSupport;
import com.datamaster.module.assets.service.materialized.IAssetsMaterializedService;
import com.datamaster.module.governance.api.model.dto.StandardsModelColumnRespDTO;
import com.datamaster.module.governance.api.model.dto.StandardsModelRespDTO;
import com.datamaster.module.governance.api.service.model.IStandardsModelApiService;
import com.datamaster.module.governance.api.service.warehouse.IWarehouseTableNamingApiService;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingReqDTO;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingRespDTO;
import com.datamaster.module.governance.controller.admin.model.vo.StandardsModelMaterializedSaveReqVO;
import com.datamaster.module.governance.dal.dataobject.model.StandardsModelMaterializedDO;
import com.datamaster.module.governance.service.model.IStandardsModelMaterializedService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 外部DDL与平台登记分阶段处理，禁止依赖平台事务撤销物理表。 */
@Slf4j
@Service
public class AssetsMaterializedServiceImpl implements IAssetsMaterializedService {
    @Resource private IStandardsModelApiService standardsModelApiService;
    @Resource private IStandardsModelMaterializedService standardsModelMaterializedService;
    @Resource private IAssetsAssetService assetService;
    @Resource(name = "assetsAssetMapper") private AssetsAssetMapper assetMapper;
    @Resource(name = "assetsDatasourceMapper") private AssetsDatasourceMapper datasourceMapper;
    @Resource private IWarehouseTableNamingApiService namingApiService;
    @Resource private DataSourceFactory dataSourceFactory;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long createMaterializedTable(AssetsMaterializedReqVO req) {
        if (CollectionUtils.isEmpty(req.getModelId())) { throw new ServiceException("物化模型列表不能为空"); }
        if (req.getDatasourceId() == null) { throw new ServiceException("必须选择已登记数据源"); }
        AssetsDatasourceDO datasource = datasourceMapper.selectById(req.getDatasourceId());
        if (datasource == null) { throw new ServiceException("已登记数据源不存在"); }
        req.setDatasourceType(datasource.getDatasourceType());
        req.setDatasourceName(datasource.getDatasourceName());
        req.setIp(datasource.getIp());
        req.setPort(datasource.getPort());
        req.setDatasourceConfig(datasource.getDatasourceConfig());
        DbQuery query = null;
        try {
            DbQueryProperty property = datasource.simplify();
            query = dataSourceFactory.createDbQuery(property);
            if (!query.valid()) { throw new ServiceException("数据库连接失败"); }
            for (Long modelId : req.getModelId()) { createMaterializedTableSingle(modelId, req, query, property); }
        } finally {
            if (query != null) {
                try { query.close(); } catch (Exception ex) { log.warn("关闭物化数据库连接失败", ex); }
            }
        }
        return 1L;
    }

    private void createMaterializedTableSingle(Long modelId, AssetsMaterializedReqVO req, DbQuery query, DbQueryProperty property) {
        String status = "4";
        String message = null;
        String sqlCommand = null;
        String modelName = null;
        String modelAlias = null;
        String tableName = null;
        Long assetId = null;
        boolean ddlStarted = false;
        boolean tableCreated = false;
        try {
            StandardsModelRespDTO model = standardsModelApiService.getDpModelByIdApi(modelId);
            if (model == null || model.getId() == null) { throw new ServiceException("逻辑模型不存在"); }
            modelName = model.getModelName();
            modelAlias = model.getModelComment();
            // 只读取模型保存的确定名称；不从前缀、英文缩写再次生成名称。
            tableName = model.getPhysicalTableName();
            AssetsTableStructureSupport.validateIdentifier(tableName, "模型物理表名");
            tableName = AssetsTableStructureSupport.physicalIdentifier(tableName, property.getDbType());
            List<StandardsModelColumnRespDTO> columns = standardsModelApiService.getModelIdColumnList(modelId);
            if (CollectionUtils.isEmpty(columns)) { throw new ServiceException("逻辑模型无字段，无法建表"); }
            List<DbColumn> dbColumns = setColumnsListFromModelColumns(columns);
            AssetsTableStructureSupport.validateColumns(dbColumns);
            String actualName = AssetsTableStructureSupport.findPhysicalTable(query, property, tableName);
            boolean existed = actualName != null;
            if (existed && !tableName.equals(actualName)) { throw new ServiceException("已有表实际大小写与模型目标名称冲突"); }
            List<StandardsModelMaterializedDO> bindings = standardsModelMaterializedService.list(Wrappers.<StandardsModelMaterializedDO>lambdaQuery()
                    .eq(StandardsModelMaterializedDO::getDatasourceId, req.getDatasourceId())
                    .eq(StandardsModelMaterializedDO::getModelId, modelId));
            final String snapshotName = tableName;
            Long boundAssetId = bindings.stream().filter(record -> "3".equals(record.getStatus()) || "5".equals(record.getStatus()))
                    .map(StandardsModelMaterializedDO::getAssetId).filter(Objects::nonNull)
                    .filter(id -> {
                        AssetsAssetDO asset = assetMapper.selectById(id);
                        return asset != null && Objects.equals(req.getDatasourceId(), asset.getDatasourceId()) && snapshotName.equals(asset.getTableName());
                    }).findFirst().orElse(null);
            boolean knownCreatedByModel = bindings.stream().anyMatch(record -> snapshotName.equals(record.getTableName())
                    && "4".equals(record.getStatus()) && StringUtils.isNotBlank(record.getSqlCommand())
                    && record.getMessage() != null && record.getMessage().startsWith("部分成功：物理表已创建"));
            boolean governed = "GOVERNED_V1".equals(model.getNamingVersion());
            if (!governed && (!existed || boundAssetId == null)) {
                throw new ServiceException("LEGACY模型的新物化必须先明确补齐治理信息并保存确定的物理表名");
            }
            if (governed) { validateGovernance(model, property.getDbType()); }
            List<AssetsAssetDO> registered = assetMapper.selectList(Wrappers.<AssetsAssetDO>lambdaQuery()
                    .eq(AssetsAssetDO::getDatasourceId, req.getDatasourceId()));
            for (AssetsAssetDO asset : registered) {
                if (!tableName.equalsIgnoreCase(asset.getTableName())) { continue; }
                long otherModels = standardsModelMaterializedService.count(Wrappers.<StandardsModelMaterializedDO>lambdaQuery()
                        .eq(StandardsModelMaterializedDO::getAssetId, asset.getId()).ne(StandardsModelMaterializedDO::getModelId, modelId));
                if (otherModels > 0) { throw new ServiceException("同名资产已绑定其他模型，禁止自动覆盖"); }
            }
            if (!existed) {
                List<String> statements = query.generateCreateTableSQL(property, tableName, model.getModelComment(), dbColumns);
                if (CollectionUtils.isEmpty(statements)) { throw new ServiceException("目标数据源不支持关系表建表"); }
                sqlCommand = String.join(";\n", statements);
                ddlStarted = true;
                for (int i = 0; i < statements.size(); i++) {
                    query.execute(statements.get(i));
                    // 目标方言第一条是CREATE TABLE；后续注释失败也要保留部分成功事实。
                    if (i == 0) { tableCreated = true; }
                }
                actualName = AssetsTableStructureSupport.findPhysicalTable(query, property, tableName);
                if (!tableName.equals(actualName)) { throw new ServiceException("DDL已执行，但实际表名无法确认，请人工检查"); }
            }
            List<DbColumn> actualColumns = existed ? AssetsTableStructureSupport.proveStructure(query, property, actualName, dbColumns)
                    : query.getTableColumns(property, actualName);
            if (actualColumns == null || actualColumns.size() != dbColumns.size()) { throw new ServiceException("实际字段元数据不完整，无法安全登记"); }
            if (!governed) {
                // 历史模型仅复用已绑定资产；不以历史缺失治理覆盖任何现有治理。
                AssetsAssetDO boundAsset = assetMapper.selectById(boundAssetId);
                assertKnownGovernance(model.getTableType(), boundAsset.getTableType());
                assertKnownGovernance(model.getDataLayerId(), boundAsset.getDataLayerId());
                assertKnownGovernance(model.getBusinessCategoryId(), boundAsset.getBusinessCategoryId());
                assertKnownGovernance(model.getDataDomainId(), boundAsset.getDataDomainId());
                assertKnownGovernance(model.getThemeDomainId(), boundAsset.getThemeDomainId());
                assetId = boundAssetId;
            } else {
                AssetsAssetSaveReqVO asset = new AssetsAssetSaveReqVO();
                asset.setType("1");
                asset.setName(model.getModelComment());
                asset.setCatCode(model.getCatCode());
                asset.setSource("2");
                asset.setStatus("1");
                asset.setCreateType("2");
                asset.setDatasourceId(req.getDatasourceId());
                asset.setTableName(actualName);
                asset.setTableComment(model.getModelComment());
                asset.setTableType(model.getTableType());
                asset.setDataLayerId(model.getDataLayerId());
                asset.setBusinessCategoryId(model.getBusinessCategoryId());
                asset.setBusinessCategoryCode(model.getBusinessCategoryCode());
                asset.setDataDomainId(model.getDataDomainId());
                asset.setThemeDomainId(model.getThemeDomainId());
                asset.setThemeDomainCode(model.getThemeDomainCode());
                asset.setTableCase(model.getTableCase());
                asset.setCreatorId(req.getCreatorId());
                asset.setCreateBy(req.getCreateBy());
                asset.setCreateTime(req.getCreateTime());
                AssetsTableStructureSupport.setAssetColumns(asset, actualColumns);
                asset.getAssetColumnList().forEach(item -> columns.stream()
                        .filter(column -> AssetsTableStructureSupport.physicalIdentifier(column.getEngName(), property.getDbType()).equals(item.getColumnName()))
                        .findFirst().filter(column -> column.getDataElemId() != null)
                        .ifPresent(column -> item.setDataElemCodeId(String.valueOf(column.getDataElemId()))));
                assetId = assetService.registerCreatedTableAsset(asset, existed && !knownCreatedByModel);
            }
            status = existed ? "5" : "3";
            message = existed ? "表已存在，结构及治理核对通过，资产已安全复用/补登记" : "建表及资产登记成功";
        } catch (Exception ex) {
            status = "4";
            log.error("模型物化失败 modelId={}, tableName={}", modelId, tableName, ex);
            message = tableCreated ? "部分成功：物理表已创建，资产未登记；不会自动删除表，请修复后重试。原因：" + ex.getMessage()
                    : (ddlStarted ? "DDL结果未完整确认，表可能已创建；不会自动删除，请人工核对。原因：" : "物化失败：") + ex.getMessage();
        } finally {
            insertMaterializedRecord(modelId, req, modelName, modelAlias, tableName, status, message, sqlCommand, assetId);
        }
    }

    private void assertKnownGovernance(Object expected, Object registered) {
        if (expected != null && registered != null && !Objects.equals(expected, registered)) {
            throw new ServiceException("历史绑定资产治理冲突，请人工核对");
        }
    }

    private void validateGovernance(StandardsModelRespDTO model, String datasourceType) {
        if (StringUtils.isBlank(model.getPhysicalTableName())) { throw new ServiceException("模型尚未保存确定的物理表名"); }
        WarehouseTableNamingReqDTO req = new WarehouseTableNamingReqDTO();
        req.setBaseTableName(model.getModelName());
        req.setTableType(model.getTableType());
        req.setDataLayerId(model.getDataLayerId());
        req.setBusinessCategoryId(model.getBusinessCategoryId());
        req.setDataDomainId(model.getDataDomainId());
        req.setThemeDomainId(model.getThemeDomainId());
        req.setTableCase(model.getTableCase());
        req.setDatasourceType(datasourceType);
        WarehouseTableNamingRespDTO governance = namingApiService.resolveAndValidate(req);
        // 仅校验治理完整性，不使用当前英文缩写重算历史已确定的表名。
        if (!Objects.equals(governance.getBusinessCategoryCode(), model.getBusinessCategoryCode())
                || !Objects.equals(governance.getThemeDomainCode(), model.getThemeDomainCode())) {
            throw new ServiceException("模型治理快照不完整或已变更，请明确补治理后重试");
        }
    }

    private List<DbColumn> setColumnsListFromModelColumns(List<StandardsModelColumnRespDTO> columns) {
        return columns.stream().map(column -> DbColumn.builder()
                .colName(column.getEngName()).dataType(column.getColumnType())
                .dataLength(column.getColumnLength() == null ? null : column.getColumnLength().toString())
                .dataScale(column.getColumnScale() == null ? null : column.getColumnScale().toString())
                .colKey("1".equals(column.getPkFlag())).nullable("0".equals(column.getNullableFlag()))
                .colPosition(column.getSortOrder() == null ? 1 : column.getSortOrder().intValue())
                .dataDefault(column.getDefaultValue()).colComment(column.getCnName()).build()).collect(Collectors.toList());
    }

    private void insertMaterializedRecord(Long modelId, AssetsMaterializedReqVO req, String modelName, String modelAlias,
                                          String tableName, String status, String message, String sqlCommand, Long assetId) {
        StandardsModelMaterializedSaveReqVO record = new StandardsModelMaterializedSaveReqVO();
        record.setModelId(modelId);
        record.setModelName(modelName == null ? String.valueOf(modelId) : modelName);
        record.setModelAlias(modelAlias == null ? String.valueOf(modelId) : modelAlias);
        record.setTableName(tableName);
        record.setStatus(status);
        record.setMessage(message);
        record.setSqlCommand(sqlCommand);
        record.setDatasourceId(req.getDatasourceId());
        record.setDatasourceType(req.getDatasourceType());
        record.setDatasourceName(req.getDatasourceName());
        record.setAssetId(assetId);
        record.setCreatorId(req.getCreatorId());
        record.setCreateBy(req.getCreateBy());
        record.setCreateTime(req.getCreateTime());
        standardsModelMaterializedService.createDpModelMaterialized(record);
    }
}
