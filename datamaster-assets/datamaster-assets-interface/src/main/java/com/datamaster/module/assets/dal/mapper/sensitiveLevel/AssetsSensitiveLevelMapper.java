package com.datamaster.module.assets.dal.mapper.sensitiveLevel;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelPageReqVO;
import com.datamaster.module.assets.dal.dataobject.sensitiveLevel.AssetsSensitiveLevelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface AssetsSensitiveLevelMapper extends BaseMapperX<AssetsSensitiveLevelDO> {

    default PageResult<AssetsSensitiveLevelDO> selectPage(AssetsSensitiveLevelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsSensitiveLevelDO>()
                .eqIfPresent(AssetsSensitiveLevelDO::getSensitiveLevel, reqVO.getSensitiveLevel())
                .eqIfPresent(AssetsSensitiveLevelDO::getSensitiveRule, reqVO.getSensitiveRule())
                .eqIfPresent(AssetsSensitiveLevelDO::getStartCharLoc, reqVO.getStartCharLoc())
                .eqIfPresent(AssetsSensitiveLevelDO::getEndCharLoc, reqVO.getEndCharLoc())
                .eqIfPresent(AssetsSensitiveLevelDO::getMaskCharacter, reqVO.getMaskCharacter())
                .eqIfPresent(AssetsSensitiveLevelDO::getOnlineFlag, reqVO.getOnlineFlag())
                .eqIfPresent(AssetsSensitiveLevelDO::getDescription, reqVO.getDescription())
                .eqIfPresent(AssetsSensitiveLevelDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsSensitiveLevelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
