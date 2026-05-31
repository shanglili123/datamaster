package com.datamaster.module.assets.dal.mapper.assetchild.api;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiParamDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * -API-Mapper
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface AssetsAssetApiParamMapper extends BaseMapperX<AssetsAssetApiParamDO> {

    default PageResult<AssetsAssetApiParamDO> selectPage(AssetsAssetApiParamPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetApiParamDO>()
                .eqIfPresent(AssetsAssetApiParamDO::getApiId, reqVO.getApiId())
                .eqIfPresent(AssetsAssetApiParamDO::getParentId, reqVO.getParentId())
                .likeIfPresent(AssetsAssetApiParamDO::getName, reqVO.getName())
                .eqIfPresent(AssetsAssetApiParamDO::getType, reqVO.getType())
                .eqIfPresent(AssetsAssetApiParamDO::getRequestFlag, reqVO.getRequestFlag())
                .eqIfPresent(AssetsAssetApiParamDO::getColumnType, reqVO.getColumnType())
                .eqIfPresent(AssetsAssetApiParamDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetApiParamDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    void removeThemeRelByAssetApiId(Long id);
}
