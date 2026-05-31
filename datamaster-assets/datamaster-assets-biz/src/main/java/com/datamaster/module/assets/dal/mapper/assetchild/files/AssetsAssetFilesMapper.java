package com.datamaster.module.assets.dal.mapper.assetchild.files;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.files.AssetsAssetFilesDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * -Mapper
 *
 * @author DATAMASTER
 * @date 2025-06-26
 */
public interface AssetsAssetFilesMapper extends BaseMapperX<AssetsAssetFilesDO> {

    default PageResult<AssetsAssetFilesDO> selectPage(AssetsAssetFilesPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetFilesDO>()
                .eqIfPresent(AssetsAssetFilesDO::getAssetId, reqVO.getAssetId())
                .likeIfPresent(AssetsAssetFilesDO::getName, reqVO.getName())
                .eqIfPresent(AssetsAssetFilesDO::getUrl, reqVO.getUrl())
                .eqIfPresent(AssetsAssetFilesDO::getType, reqVO.getType())
                .eqIfPresent(AssetsAssetFilesDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetFilesDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
