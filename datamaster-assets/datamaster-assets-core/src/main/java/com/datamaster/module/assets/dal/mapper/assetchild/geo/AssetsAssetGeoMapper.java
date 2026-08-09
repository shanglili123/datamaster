package com.datamaster.module.assets.dal.mapper.assetchild.geo;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.geo.AssetsAssetGeoDO;
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
public interface AssetsAssetGeoMapper extends BaseMapperX<AssetsAssetGeoDO> {

    default PageResult<AssetsAssetGeoDO> selectPage(AssetsAssetGeoPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetGeoDO>()
                .eqIfPresent(AssetsAssetGeoDO::getAssetId, reqVO.getAssetId())
                .likeIfPresent(AssetsAssetGeoDO::getFileName, reqVO.getFileName())
                .eqIfPresent(AssetsAssetGeoDO::getFileUrl, reqVO.getFileUrl())
                .eqIfPresent(AssetsAssetGeoDO::getFileType, reqVO.getFileType())
                .eqIfPresent(AssetsAssetGeoDO::getElementType, reqVO.getElementType())
                .eqIfPresent(AssetsAssetGeoDO::getCoordinateSystem, reqVO.getCoordinateSystem())
                .eqIfPresent(AssetsAssetGeoDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetGeoDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
