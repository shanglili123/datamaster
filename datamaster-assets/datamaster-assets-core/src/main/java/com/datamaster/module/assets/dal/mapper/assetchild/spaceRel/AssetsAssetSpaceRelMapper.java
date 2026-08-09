package com.datamaster.module.assets.dal.mapper.assetchild.spaceRel;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel.AssetsAssetSpaceRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
public interface AssetsAssetSpaceRelMapper extends BaseMapperX<AssetsAssetSpaceRelDO> {

    default PageResult<AssetsAssetSpaceRelDO> selectPage(AssetsAssetSpaceRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetSpaceRelDO>()
                .eqIfPresent(AssetsAssetSpaceRelDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetSpaceRelDO::getSpaceId, reqVO.getSpaceId())
                .eqIfPresent(AssetsAssetSpaceRelDO::getSpaceCode, reqVO.getSpaceCode())
                .eqIfPresent(AssetsAssetSpaceRelDO::getDescription, reqVO.getDescription())
                .eqIfPresent(AssetsAssetSpaceRelDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetSpaceRelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    default List<AssetsAssetSpaceRelDO> selectList(AssetsAssetSpaceRelPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<AssetsAssetSpaceRelDO>()
                .eqIfPresent(AssetsAssetSpaceRelDO::getId, reqVO.getId())
                .eqIfPresent(AssetsAssetSpaceRelDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetSpaceRelDO::getSpaceId, reqVO.getSpaceId())
                .eqIfPresent(AssetsAssetSpaceRelDO::getSpaceCode, reqVO.getSpaceCode())
                .eqIfPresent(AssetsAssetSpaceRelDO::getDescription, reqVO.getDescription()));
    }

    void removeSpaceRelByAssetId(Long id);
}
