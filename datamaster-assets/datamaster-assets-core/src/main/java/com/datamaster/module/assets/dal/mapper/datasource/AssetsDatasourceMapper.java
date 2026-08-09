package com.datamaster.module.assets.dal.mapper.datasource;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourcePageReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapper
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface AssetsDatasourceMapper extends BaseMapperX<AssetsDatasourceDO> {

    default PageResult<AssetsDatasourceDO> selectPage(AssetsDatasourcePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsDatasourceDO>()
                .likeIfPresent(AssetsDatasourceDO::getDatasourceName, reqVO.getDatasourceName())
                .inIfPresent(AssetsDatasourceDO::getDatasourceType, StringUtils.isNotEmpty(reqVO.getDatasourceType()) ? reqVO.getDatasourceType().split(",") : null)
                .eqIfPresent(AssetsDatasourceDO::getDatasourceConfig, reqVO.getDatasourceConfig())
                .eqIfPresent(AssetsDatasourceDO::getIp, reqVO.getIp())
                .eqIfPresent(AssetsDatasourceDO::getPort, reqVO.getPort())
                .eqIfPresent(AssetsDatasourceDO::getListCount, reqVO.getListCount())
                .eqIfPresent(AssetsDatasourceDO::getSyncCount, reqVO.getSyncCount())
                .eqIfPresent(AssetsDatasourceDO::getDataSize, reqVO.getDataSize())
                .eqIfPresent(AssetsDatasourceDO::getDescription, reqVO.getDescription())
                .eqIfPresent(AssetsDatasourceDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AssetsDatasourceDO::getId, reqVO.getIdList())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsDatasourceDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    public List<AssetsDatasourceDO> getDataSourceByAsset();
}
