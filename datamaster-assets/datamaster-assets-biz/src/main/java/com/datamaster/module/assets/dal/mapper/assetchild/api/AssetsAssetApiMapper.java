package com.datamaster.module.assets.dal.mapper.assetchild.api;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * -APIMapper
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface AssetsAssetApiMapper extends BaseMapperX<AssetsAssetApiDO> {

    default PageResult<AssetsAssetApiDO> selectPage(AssetsAssetApiPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetApiDO>()
                .eqIfPresent(AssetsAssetApiDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetApiDO::getUrl, reqVO.getUrl())
                .eqIfPresent(AssetsAssetApiDO::getHttpMethod, reqVO.getHttpMethod())
                .eqIfPresent(AssetsAssetApiDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetApiDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
