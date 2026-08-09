package com.datamaster.module.assets.service.materialized.impl;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceCreaTeTableReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsAssetApiOutService;
import com.datamaster.module.assets.controller.admin.materialized.vo.AssetsMaterializedReqVO;
import com.datamaster.module.assets.service.materialized.IAssetsMaterializedService;
import com.datamaster.module.governance.api.model.dto.StandardsModelColumnRespDTO;
import com.datamaster.module.governance.api.model.dto.StandardsModelRespDTO;
import com.datamaster.module.governance.api.service.model.IStandardsModelApiService;
import com.datamaster.module.governance.controller.admin.model.vo.StandardsModelMaterializedSaveReqVO;
import com.datamaster.module.governance.service.model.IStandardsModelMaterializedService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产物化Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-08-01
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsMaterializedServiceImpl implements IAssetsMaterializedService {

    @Resource
    private IStandardsModelApiService standardsModelApiService;
    @Resource
    private IStandardsModelMaterializedService standardsModelMaterializedService;
    @Resource
    private IAssetsAssetApiOutService iAssetsAssetApiService;
    @Resource
    private IDatasourceApiService datasourceApiService;
    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Override
    public Long createMaterializedTable(AssetsMaterializedReqVO req) {
        List<Long> modelIdList = req.getModelId();
        if (CollectionUtils.isEmpty(modelIdList)) {
            throw new RuntimeException("获取信息失败,原因:物化信息为空");
        }

        DbQueryProperty dbQueryProperty = new DbQueryProperty(req.getDatasourceType(), req.getIp(), req.getPort(), req.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
        //测试链接
        if (!dbQuery.valid()) {
            throw new RuntimeException("数据库连接失败！");
        }

        for (Long modelId : modelIdList) {
            createMaterializedTableSingle(modelId, req, dbQuery, dbQueryProperty);
        }
        dbQuery.close();

        return 1L;
    }

    private void createMaterializedTableSingle(Long modelId, AssetsMaterializedReqVO req, DbQuery dbQuery, DbQueryProperty dbQueryProperty) {
        String status = "2";
        String message = null;
        String sqlCommand = null;
        String modelName = null;
        String modelAlias = null;
        Long assetId = null;
        try {
            // 1. 查询模型/字段，若校验不通过则抛异常
            StandardsModelRespDTO model = checkAndGetModel(modelId);
            modelName = model.getModelName();
            modelAlias = model.getModelComment();

            List<StandardsModelColumnRespDTO> columnList = checkAndGetModelColumns(modelId);
            Long fieldCount = Long.valueOf(columnList.size());

            String tableName = model.getTableName();
            int tableStatus = dbQuery.generateCheckTableExistsSQL(dbQueryProperty, tableName);
            if (tableStatus > 0) {
                // 表已存在，无需重复创建
                status = "4";
                message = "表 [" + tableName + "] 已存在，无需重复创建";
            } else {
                List<DbColumn> dbColumns = setColumnsListFromModelColumns(columnList);

                DatasourceCreaTeTableReqDTO creaTeTableReqDTO = new DatasourceCreaTeTableReqDTO();
                creaTeTableReqDTO.setDatasourceType(req.getDatasourceType());
                creaTeTableReqDTO.setIp(req.getIp());
                creaTeTableReqDTO.setPort(req.getPort());
                creaTeTableReqDTO.setDatasourceConfig(req.getDatasourceConfig());
                creaTeTableReqDTO.setTableName(tableName);
                creaTeTableReqDTO.setTableComment(model.getModelComment());
                creaTeTableReqDTO.setColumnsList(dbColumns);

                List<String> tableSQLList = dbQuery.generateCreateTableSQL(dbQueryProperty, tableName, model.getModelComment(), dbColumns);
                sqlCommand = tableSQLList.toString();

                boolean created = datasourceApiService.creaDatasourceTeTableApi(dbQuery, dbQueryProperty, creaTeTableReqDTO);
                if (!created) {
                    status = "4";
                    message = "表 [" + tableName + "] 已存在，无需重复创建";
                } else {
                    // 登记资产
                    AssetsAssetReqDTO assetReqDTO = new AssetsAssetReqDTO();
                    assetReqDTO.setSource("2");
                    assetReqDTO.setModelId(modelId);
                    assetReqDTO.setDatasourceId(req.getDatasourceId());
                    assetReqDTO.setFieldCount(fieldCount);
                    AssetsAssetRespDTO assetRespDTO = iAssetsAssetApiService.insertAsset(assetReqDTO);
                    assetId = assetRespDTO.getId();//资产id

                    status = "3";
                    message = "建表成功";
                }
            }
        } catch (Exception ex) {
            log.error("物化建表失败, modelId={}", modelId, ex);
            status = "4";
            message = "建表失败：" + ex.getMessage();
        } finally {
            insertMaterializedRecord(modelId, req, modelName, modelAlias, status, message, sqlCommand, assetId);
        }
    }

    /**
     * 将模型列转换为 DbColumn，并赋值给 columnsList
     *
     * @param columnList 模型列列表
     */
    private List<DbColumn> setColumnsListFromModelColumns(List<StandardsModelColumnRespDTO> columnList) {
        return columnList.stream()
                .map(column -> DbColumn.builder()
                        .colName(column.getEngName())
                        .dataType(column.getColumnType())
                        .dataLength(column.getColumnLength() != null ? column.getColumnLength().toString() : null)
                        .dataScale(column.getColumnScale() != null ? column.getColumnScale().toString() : null)
                        .colKey("1".equals(column.getPkFlag()))
                        .nullable("0".equals(column.getNullableFlag()))
                        .colPosition(column.getSortOrder() == null ? 1 : column.getSortOrder().intValue())
                        .dataDefault(column.getDefaultValue())
                        .colComment(column.getCnName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 查询并校验模型，不存在则抛异常
     */
    private StandardsModelRespDTO checkAndGetModel(Long modelId) {
        StandardsModelRespDTO model = standardsModelApiService.getDpModelByIdApi(modelId);
        if (model == null || model.getId() == null) {
            throw new RuntimeException("逻辑模型不存在, modelId=" + modelId);
        }
        return model;
    }

    /**
     * 查询并校验字段列表，不存在则抛异常
     */
    private List<StandardsModelColumnRespDTO> checkAndGetModelColumns(Long modelId) {
        List<StandardsModelColumnRespDTO> columnList = standardsModelApiService.getModelIdColumnList(modelId);
        if (CollectionUtils.isEmpty(columnList)) {
            throw new RuntimeException("逻辑模型无字段，无法建表, modelId=" + modelId);
        }
        return columnList;
    }

    /**
     * 组装物化记录并落库
     */
    private void insertMaterializedRecord(Long modelId, AssetsMaterializedReqVO req, String modelName, String modelAlias, String status, String message, String sqlCommand, Long assetId) {
        StandardsModelMaterializedSaveReqVO saveReqVO = new StandardsModelMaterializedSaveReqVO();
        saveReqVO.setModelId(modelId);
        saveReqVO.setModelName(modelName != null ? modelName : String.valueOf(modelId));
        saveReqVO.setModelAlias(modelAlias != null ? modelAlias : String.valueOf(modelId));
        saveReqVO.setStatus(status);
        saveReqVO.setMessage(message);
        saveReqVO.setSqlCommand(sqlCommand);
        saveReqVO.setDatasourceId(req.getDatasourceId());
        saveReqVO.setDatasourceType(req.getDatasourceType());
        saveReqVO.setDatasourceName(req.getDatasourceName());
        saveReqVO.setAssetId(assetId);
        saveReqVO.setCreatorId(req.getCreatorId());
        saveReqVO.setCreateBy(req.getCreateBy());
        saveReqVO.setCreateTime(req.getCreateTime());
        standardsModelMaterializedService.createDpModelMaterialized(saveReqVO);
    }

}
