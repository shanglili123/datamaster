package com.datamaster.module.assets.service.datasource;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.module.taxonomy.api.project.dto.TaxonomyProjectReqDTO;
import com.datamaster.module.taxonomy.api.project.dto.TaxonomyProjectRespDTO;
import com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableReqDTO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourcePageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.standards.api.model.dto.StandardsModelColumnReqDTO;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface IAssetsDatasourceService extends IService<AssetsDatasourceDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsDatasourceDO> getDatasourcePage(AssetsDatasourcePageReqVO pageReqVO);

    /**
     *
     *
     * @param AssetsDatasource
     * @return
     */
    PageResult<AssetsDatasourceDO> getDatasourceDppPage(AssetsDatasourcePageReqVO AssetsDatasource);

    List<AssetsDatasourceDO> getDatasourceList(AssetsDatasourcePageReqVO pageReqVO);

    /**
     *
     *
     * @param AssetsAsset
     * @return
     */
    List<AssetsDatasourceDO> getDataSourceByAsset(AssetsDatasourceRespVO AssetsAsset);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDatasource(AssetsDatasourceSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDatasource(AssetsDatasourceSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDatasource(Collection<Long> idList);

    /**
     *
     * @param idList id
     * @param type 0:1:
     * @return
     */
    int removeDatasourceDppOrDa(List<Long> idList, Long type);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsDatasourceDO getDatasourceDOById(Long id);
    AssetsDatasourceRespVO getDatasourceByIdSimple(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceDO> getDatasourceList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsDatasourceDO> getDatasourceMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDatasource(List<AssetsDatasourceRespVO> importExcelList, boolean isUpdateSupport, String operName);

    AjaxResult clientsTest(Long id);

    /**
     *
     *
     * @param id id
     * @return
     */
    List<DbTable> getDbTables(Long id);

    /**
     *
     *
     *
     * @param id        id
     * @param tableName
     * @return
     */
    List<DbColumn> getDbTableColumns(Long id, String tableName);

    /**
     *
     *
     * @param jsonObject id
     * @return
     */
    List<StandardsModelColumnReqDTO> getColumnsList(JSONObject jsonObject);

    List<AssetsAssetColumnDO> columnsAsAssetColumnList(JSONObject jsonObject);

    List<AssetsAssetColumnDO> columnsAsAssetColumnList(Long id, String tableName);

    /**
     *
     *
     * @param datasourceCreaTeTableReqDTO
     * @return
     */
    boolean creaDatasourceTeTable(DatasourceCreaTeTableReqDTO datasourceCreaTeTableReqDTO);

    boolean creaDatasourceTeTable(DbQuery dbQuery, DbQueryProperty dbQueryProperty, DatasourceCreaTeTableReqDTO datasourceCreaTeTableReqDTO);

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<TaxonomyProjectRespDTO> getNoDppAddList(TaxonomyProjectReqDTO pageReqVO);

    /**
     * Kafka
     * @param AssetsDatasource
     * @return
     */
    List<AssetsDatasourceDO> getDatasourceDppNoKafka(AssetsDatasourcePageReqVO AssetsDatasource);

    com.datamaster.common.database.core.PageResult<Map<String, Object>> executeSqlQuery(AssetsDatasourcePageReqVO AssetsDatasource);

    void exportSqlQueryResult(HttpServletResponse response, AssetsDatasourcePageReqVO AssetsDatasource);

    List<DbColumn> sqlParse(String sourceId, String sqlText);

    /**
     *
     * @param datasourceId
     * @param status
     * @return
     */
    Boolean editDatasourceStatus(Long datasourceId, Long status);

    /**
     *
     *
     * @param id
     */
    void detectTableSchemaUpdates(Long id);
}
