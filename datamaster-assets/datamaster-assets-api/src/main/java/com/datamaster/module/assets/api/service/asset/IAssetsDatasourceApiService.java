

package com.datamaster.module.assets.api.service.asset;

import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbName;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableListReqDTO;
import com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableReqDTO;

import java.util.List;

/**
 * 数据源Service接口
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface IAssetsDatasourceApiService {


    public AssetsDatasourceRespDTO getDatasourceById(Long id);
    public boolean creaDatasourceTeTableApi(DatasourceCreaTeTableReqDTO datasourceCreaTeTableReqDTO);
    public boolean creaDatasourceTeTableApi(DbQuery dbQuery, DbQueryProperty dbQueryProperty, DatasourceCreaTeTableReqDTO creaTeTableReqDTO);
    public boolean creaDatasourceTeTableListApi(DatasourceCreaTeTableListReqDTO datasourceCreaTeTableListReqDTO);

    /**
     * 获得字段列表
     *
     * @param datasourceId 数据源id
     * @param tableName    表名
     * @return 数据源列表
     */
    List<DbColumn> getDbTableColumns(Long datasourceId, String tableName);


    /**
     * 获得表信息
     *
     * @param datasourceId 数据源id
     * @param tableName    表名
     * @return 数据源列表
     */
    DbTable getDbTable(Long datasourceId, String tableName);

    /**
     * TODO:获得数据库列表
     *
     * @param id 数据源id
     * @return 数据源列表
     */
    public List<DbName> getDatabaseListByDatasourceId(Long id);

    /**
     * 根据ids获取数据源列表
     * @param ids
     * @return
     */
    List<AssetsDatasourceRespDTO> getDatabaseListByIds(List<Long> ids);
}
