package com.datamaster.common.datasource.mgmt.service;

import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.core.page.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IDatasourceMgmtService {

    PageResult<DatasourceDO> getDatasourcePage(com.datamaster.common.core.page.PageParam pageParam, com.datamaster.mybatis.core.query.LambdaQueryWrapperX<DatasourceDO> queryWrapper);

    DatasourceDO getDatasourceDOById(Long id);

    List<DatasourceDO> getDatasourceList();

    Map<Long, DatasourceDO> getDatasourceMap();

    Long createDatasource(DatasourceDO datasource);

    int updateDatasource(DatasourceDO datasource);

    int removeDatasource(Collection<Long> idList);

    Boolean editDatasourceStatus(Long datasourceId, Boolean status);
}
