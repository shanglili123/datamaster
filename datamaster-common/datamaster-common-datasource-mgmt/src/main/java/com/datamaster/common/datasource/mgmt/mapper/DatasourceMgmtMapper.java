package com.datamaster.common.datasource.mgmt.mapper;

import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface DatasourceMgmtMapper extends BaseMapperX<DatasourceDO> {

    default PageResult<DatasourceDO> selectPage(com.datamaster.common.datasource.mgmt.api.dto.DatasourcePageReq req) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        return selectPage(req, new LambdaQueryWrapperX<DatasourceDO>()
                .likeIfPresent(DatasourceDO::getDatasourceName, req.getDatasourceName())
                .inIfPresent(DatasourceDO::getDatasourceType, StringUtils.isNotEmpty(req.getDatasourceType()) ? req.getDatasourceType().split(",") : null)
                .eqIfPresent(DatasourceDO::getDatasourceConfig, req.getDatasourceConfig())
                .eqIfPresent(DatasourceDO::getIp, req.getIp())
                .eqIfPresent(DatasourceDO::getPort, req.getPort())
                .eqIfPresent(DatasourceDO::getListCount, req.getListCount())
                .eqIfPresent(DatasourceDO::getSyncCount, req.getSyncCount())
                .eqIfPresent(DatasourceDO::getDataSize, req.getDataSize())
                .eqIfPresent(DatasourceDO::getDescription, req.getDescription())
                .eqIfPresent(DatasourceDO::getCreateTime, req.getCreateTime())
                .inIfPresent(DatasourceDO::getId, req.getIdList())
                .orderBy(req.getOrderByColumn(), req.getIsAsc(), allowedColumns));
    }
}
