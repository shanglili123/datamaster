package com.datamaster.module.assets.service.datasource.impl;

import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbName;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceCreaTeTableListReqDTO;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceCreaTeTableReqDTO;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据源管理 mgmt 接口实现（包装资产侧数据源服务，负责 common DTO 与资产 DTO 的转换）
 *
 * @author DATAMASTER
 * @date 2026-08-01
 */
@Slf4j
@Service
public class DatasourceApiServiceImpl implements IDatasourceApiService {

    @Resource
    private IAssetsDatasourceApiService assetsDatasourceApiService;

    @Override
    public DatasourceRespDTO getDatasourceById(Long id) {
        return BeanUtils.toBean(assetsDatasourceApiService.getDatasourceById(id), DatasourceRespDTO.class);
    }

    @Override
    public boolean creaDatasourceTeTableApi(DatasourceCreaTeTableReqDTO datasourceCreaTeTableReqDTO) {
        return assetsDatasourceApiService.creaDatasourceTeTableApi(toAssetsReqDTO(datasourceCreaTeTableReqDTO));
    }

    @Override
    public boolean creaDatasourceTeTableApi(DbQuery dbQuery, DbQueryProperty dbQueryProperty, DatasourceCreaTeTableReqDTO creaTeTableReqDTO) {
        return assetsDatasourceApiService.creaDatasourceTeTableApi(dbQuery, dbQueryProperty, toAssetsReqDTO(creaTeTableReqDTO));
    }

    @Override
    public boolean creaDatasourceTeTableListApi(DatasourceCreaTeTableListReqDTO datasourceCreaTeTableListReqDTO) {
        com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableListReqDTO assetsReqDTO = new com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableListReqDTO();
        assetsReqDTO.setDatasourceType(datasourceCreaTeTableListReqDTO.getDatasourceType());
        assetsReqDTO.setDatasourceConfig(datasourceCreaTeTableListReqDTO.getDatasourceConfig());
        assetsReqDTO.setIp(datasourceCreaTeTableListReqDTO.getIp());
        assetsReqDTO.setPort(datasourceCreaTeTableListReqDTO.getPort());
        List<com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableReqDTO> dtoList = new ArrayList<>();
        if (datasourceCreaTeTableListReqDTO.getDtoList() != null) {
            for (DatasourceCreaTeTableReqDTO dto : datasourceCreaTeTableListReqDTO.getDtoList()) {
                dtoList.add(toAssetsReqDTO(dto));
            }
        }
        assetsReqDTO.setDtoList(dtoList);
        return assetsDatasourceApiService.creaDatasourceTeTableListApi(assetsReqDTO);
    }

    @Override
    public List<DbColumn> getDbTableColumns(Long datasourceId, String tableName) {
        return assetsDatasourceApiService.getDbTableColumns(datasourceId, tableName);
    }

    @Override
    public DbTable getDbTable(Long datasourceId, String tableName) {
        return assetsDatasourceApiService.getDbTable(datasourceId, tableName);
    }

    @Override
    public List<DbName> getDatabaseListByDatasourceId(Long id) {
        return assetsDatasourceApiService.getDatabaseListByDatasourceId(id);
    }

    @Override
    public List<DatasourceRespDTO> getDatabaseListByIds(List<Long> ids) {
        return BeanUtils.toBean(assetsDatasourceApiService.getDatabaseListByIds(ids), DatasourceRespDTO.class);
    }

    @Override
    public String getSpaceCodeByDatasourceId(Long datasourceId) {
        return assetsDatasourceApiService.getSpaceCodeByDatasourceId(datasourceId);
    }

    @Override
    public void detectTableSchemaUpdates(Long id) {
        assetsDatasourceApiService.detectTableSchemaUpdates(id);
    }

    private com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableReqDTO toAssetsReqDTO(DatasourceCreaTeTableReqDTO dto) {
        return BeanUtils.toBean(dto, com.datamaster.module.assets.api.datasource.dto.DatasourceCreaTeTableReqDTO.class);
    }
}
