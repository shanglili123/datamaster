package com.datamaster.common.datasource.mgmt.api;

import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbName;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceCreaTeTableListReqDTO;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceCreaTeTableReqDTO;

import java.util.List;

public interface IDatasourceApiService {

    DatasourceRespDTO getDatasourceById(Long id);

    boolean creaDatasourceTeTableApi(DatasourceCreaTeTableReqDTO datasourceCreaTeTableReqDTO);

    boolean creaDatasourceTeTableApi(DbQuery dbQuery, DbQueryProperty dbQueryProperty, DatasourceCreaTeTableReqDTO creaTeTableReqDTO);

    boolean creaDatasourceTeTableListApi(DatasourceCreaTeTableListReqDTO datasourceCreaTeTableListReqDTO);

    List<DbColumn> getDbTableColumns(Long datasourceId, String tableName);

    DbTable getDbTable(Long datasourceId, String tableName);

    List<DbName> getDatabaseListByDatasourceId(Long id);

    List<DatasourceRespDTO> getDatabaseListByIds(List<Long> ids);

    /**
     * 根据数据源ID获取空间编码
     *
     * @param datasourceId 数据源ID
     * @return 空间编码
     */
    String getSpaceCodeByDatasourceId(Long datasourceId);

    /**
     * 检查数据源是否关联指定空间；空间 ID 和编码同时提供时须同时匹配。
     * 参数不完整或关联已失效时返回 false，不创建或修改关联。
     */
    boolean hasDatasourceSpace(Long datasourceId, Long spaceId, String spaceCode);

}
