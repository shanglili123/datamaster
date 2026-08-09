package com.datamaster.common.datasource.mgmt.service;

import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;

import java.util.Collection;
import java.util.List;

public interface IDatasourceSpaceRelMgmtService {

    Long createDatasourceSpaceRel(DatasourceSpaceRelDO rel);

    int updateDatasourceSpaceRel(DatasourceSpaceRelDO rel);

    int removeDatasourceSpaceRel(Collection<Long> idList);

    DatasourceSpaceRelDO getDatasourceSpaceRelById(Long id);

    List<DatasourceSpaceRelDO> getDatasourceSpaceRelList();

    List<DatasourceSpaceRelDO> getDatasourceSpaceRelList(DatasourceSpaceRelDO condition);
}
