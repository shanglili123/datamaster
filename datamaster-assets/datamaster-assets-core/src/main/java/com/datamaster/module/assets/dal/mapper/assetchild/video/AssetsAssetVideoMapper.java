package com.datamaster.module.assets.dal.mapper.assetchild.video;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.video.AssetsAssetVideoDO;
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
public interface AssetsAssetVideoMapper extends BaseMapperX<AssetsAssetVideoDO> {

    default PageResult<AssetsAssetVideoDO> selectPage(AssetsAssetVideoPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetVideoDO>()
                .eqIfPresent(AssetsAssetVideoDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetVideoDO::getIp, reqVO.getIp())
                .eqIfPresent(AssetsAssetVideoDO::getPort, reqVO.getPort())
                .eqIfPresent(AssetsAssetVideoDO::getProtocol, reqVO.getProtocol())
                .eqIfPresent(AssetsAssetVideoDO::getPlatform, reqVO.getPlatform())
                .eqIfPresent(AssetsAssetVideoDO::getConfig, reqVO.getConfig())
                .eqIfPresent(AssetsAssetVideoDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetVideoDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
