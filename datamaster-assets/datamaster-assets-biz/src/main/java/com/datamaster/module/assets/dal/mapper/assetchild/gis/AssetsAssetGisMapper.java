package com.datamaster.module.assets.dal.mapper.assetchild.gis;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.gis.AssetsAssetGisDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * -Mapper
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface AssetsAssetGisMapper extends BaseMapperX<AssetsAssetGisDO> {

    default PageResult<AssetsAssetGisDO> selectPage(AssetsAssetGisPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetGisDO>()
                .eqIfPresent(AssetsAssetGisDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetGisDO::getUrl, reqVO.getUrl())
                .eqIfPresent(AssetsAssetGisDO::getType, reqVO.getType())
                .eqIfPresent(AssetsAssetGisDO::getHttpMethod, reqVO.getHttpMethod())
                .eqIfPresent(AssetsAssetGisDO::getCoordinateSystem, reqVO.getCoordinateSystem())
                .eqIfPresent(AssetsAssetGisDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetGisDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
