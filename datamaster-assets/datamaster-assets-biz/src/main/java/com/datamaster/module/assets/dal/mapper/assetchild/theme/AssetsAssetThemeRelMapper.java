package com.datamaster.module.assets.dal.mapper.assetchild.theme;

import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.theme.AssetsAssetThemeRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * -Mapper
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface AssetsAssetThemeRelMapper extends BaseMapperX<AssetsAssetThemeRelDO> {

    default PageResult<AssetsAssetThemeRelDO> selectPage(AssetsAssetThemeRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetThemeRelDO>()
                .eqIfPresent(AssetsAssetThemeRelDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetThemeRelDO::getThemeId, reqVO.getThemeId())
                .eqIfPresent(AssetsAssetThemeRelDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetThemeRelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    void deleteDaAssetThemeRelByAssetId(Long id);

    List<Long> getDaAssetIdList(@Param("themeIdList") List<Long> themeIdList);
}
