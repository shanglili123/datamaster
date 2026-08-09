package com.datamaster.module.assets.dal.mapper.datasource;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelPageReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceSpaceRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
public interface AssetsDatasourceSpaceRelMapper extends BaseMapperX<AssetsDatasourceSpaceRelDO> {

    default PageResult<AssetsDatasourceSpaceRelDO> selectPage(AssetsDatasourceSpaceRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsDatasourceSpaceRelDO>()
                .eqIfPresent(AssetsDatasourceSpaceRelDO::getSpaceId, reqVO.getSpaceId())
                .eqIfPresent(AssetsDatasourceSpaceRelDO::getSpaceCode, reqVO.getSpaceCode())
                .eqIfPresent(AssetsDatasourceSpaceRelDO::getDatasourceId, reqVO.getDatasourceId())
                .eqIfPresent(AssetsDatasourceSpaceRelDO::getDescription, reqVO.getDescription())
                .eqIfPresent(AssetsDatasourceSpaceRelDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsDatasourceSpaceRelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
