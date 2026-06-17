
package com.datamaster.api.ds.api.service.datasource;

import com.datamaster.api.ds.api.base.DsResultDTO;
import com.datamaster.api.ds.api.datasource.DsDatasourceCreateReqDTO;

public interface IDsDatasourceService {

    DsResultDTO createDatasource(DsDatasourceCreateReqDTO reqDTO);

    DsResultDTO testConnect(DsDatasourceCreateReqDTO reqDTO);

    /**
     * 查询 DS 数据源列表，按名称精确查找是否已存在
     * @param name 数据源名称
     * @return true=已存在
     */
    boolean existsByName(String name);

}
