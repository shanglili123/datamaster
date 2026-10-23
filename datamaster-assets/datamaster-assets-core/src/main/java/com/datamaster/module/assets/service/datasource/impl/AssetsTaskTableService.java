package com.datamaster.module.assets.service.datasource.impl;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableReqDTO;
import com.datamaster.module.assets.api.datasource.dto.DatasourceCreateTableRespDTO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.datasource.AssetsTableStructureSupport;
import com.datamaster.module.governance.api.service.warehouse.IWarehouseTableNamingApiService;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingReqDTO;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/** 集成建表专用编排，不改变通用creaDatasourceTeTable的调用契约。 */
@Slf4j
@Service
public class AssetsTaskTableService {
    @Resource(name = "assetsDatasourceMapper") private AssetsDatasourceMapper datasourceMapper;
    @Resource private DataSourceFactory dataSourceFactory;
    @Resource private IWarehouseTableNamingApiService namingApiService;
    @Resource private IAssetsAssetService assetService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DatasourceCreateTableRespDTO createTaskTempTable(DatasourceCreaTeTableReqDTO req) {
        if (req.getDatasourceId() == null) { throw new ServiceException("集成建表必须选择已登记数据源"); }
        AssetsDatasourceDO datasource = datasourceMapper.selectById(req.getDatasourceId());
        if (datasource == null) { throw new ServiceException("已登记数据源不存在"); }
        // 绝不使用客户端传来的ip/port/config/type连接快照。
        req.setDatasourceType(datasource.getDatasourceType());
        req.setIp(datasource.getIp());
        req.setPort(datasource.getPort());
        req.setDatasourceConfig(datasource.getDatasourceConfig());
        WarehouseTableNamingReqDTO namingReq = new WarehouseTableNamingReqDTO();
        namingReq.setBaseTableName(req.getBaseTableName());
        namingReq.setTableType(req.getTableType());
        namingReq.setDataLayerId(req.getDataLayerId());
        namingReq.setBusinessCategoryId(req.getBusinessCategoryId());
        namingReq.setDataDomainId(req.getDataDomainId());
        namingReq.setThemeDomainId(req.getThemeDomainId());
        namingReq.setTableCase(req.getTableCase());
        namingReq.setDatasourceType(datasource.getDatasourceType());
        WarehouseTableNamingRespDTO naming = namingApiService.resolveAndValidate(namingReq);
        String tableName = AssetsTableStructureSupport.physicalIdentifier(naming.getTableName(), datasource.getDatasourceType());
        AssetsTableStructureSupport.validateIdentifier(tableName, "表名");
        AssetsTableStructureSupport.validateColumns(req.getColumnsList());
        req.setTableName(tableName);
        DatasourceCreateTableRespDTO result = new DatasourceCreateTableRespDTO();
        result.setDatasourceId(req.getDatasourceId());
        result.setTableName(tableName);
        DbQuery query = null;
        boolean ddlStarted = false;
        try {
            DbQueryProperty property = datasource.simplify();
            query = dataSourceFactory.createDbQuery(property);
            if (!query.valid()) { throw new ServiceException("数据库连接失败"); }
            String actualName = AssetsTableStructureSupport.findPhysicalTable(query, property, tableName);
            boolean existed = actualName != null;
            if (existed && !tableName.equals(actualName)) { throw new ServiceException("已有表实际大小写与目标名称冲突"); }
            if (!existed) {
                List<String> statements = query.generateCreateTableSQL(property, tableName, req.getTableComment(), req.getColumnsList());
                if (statements == null || statements.isEmpty()) { throw new ServiceException("该数据源不支持关系表建表"); }
                ddlStarted = true;
                for (int i = 0; i < statements.size(); i++) {
                    query.execute(statements.get(i));
                    if (i == 0) { result.setTableCreated(true); }
                }
                actualName = AssetsTableStructureSupport.findPhysicalTable(query, property, tableName);
                if (actualName == null || !tableName.equals(actualName)) { throw new ServiceException("DDL已执行，但无法确认实际表名称，请人工检查"); }
            }
            List<DbColumn> actualColumns;
            if (existed) {
                actualColumns = AssetsTableStructureSupport.proveStructure(query, property, actualName, req.getColumnsList());
            } else {
                actualColumns = query.getTableColumns(property, actualName);
                if (actualColumns == null || actualColumns.size() != req.getColumnsList().size()) { throw new ServiceException("表已创建但字段元数据不完整，无法安全登记"); }
            }
            AssetsAssetSaveReqVO asset = new AssetsAssetSaveReqVO();
            asset.setType("1");
            asset.setName(tableName);
            asset.setTableName(actualName);
            asset.setTableComment(req.getTableComment());
            asset.setDatasourceId(req.getDatasourceId());
            asset.setSource("2");
            asset.setStatus("1");
            asset.setCreateType("2");
            asset.setCatCode(StringUtils.isNotBlank(req.getCatCode()) ? req.getCatCode() : "integration");
            asset.setTableType(req.getTableType());
            asset.setDataLayerId(naming.getDataLayerId());
            asset.setBusinessCategoryId(naming.getBusinessCategoryId());
            asset.setBusinessCategoryCode(naming.getBusinessCategoryCode());
            asset.setDataDomainId(naming.getDataDomainId());
            asset.setThemeDomainId(naming.getThemeDomainId());
            asset.setThemeDomainCode(naming.getThemeDomainCode());
            asset.setTableCase(naming.getTableCase());
            AssetsTableStructureSupport.setAssetColumns(asset, actualColumns);
            result.setAssetId(assetService.registerCreatedTableAsset(asset, existed));
            result.setAssetRegistered(true);
            result.setMessage(existed ? "表已存在，结构与治理核对通过，资产已安全登记/复用" : "建表并登记资产成功");
        } catch (Exception ex) {
            log.warn("集成建表/登记未完成 datasourceId={}, tableName={}", req.getDatasourceId(), tableName, ex);
            result.setMessage(result.isTableCreated()
                    ? "部分成功：物理表已创建，资产未登记；不会自动删除表，请修复后重试。原因：" + ex.getMessage()
                    : (ddlStarted ? "DDL执行未完整确认，物理表可能已创建；不会自动删除，请核对后重试。原因：" : "未创建/登记：") + ex.getMessage());
        } finally {
            if (query != null) {
                try { query.close(); } catch (Exception ex) { log.warn("关闭集成建表连接失败", ex); }
            }
        }
        return result;
    }
}
