package com.datamaster.module.standards.dal.mapper.sensitiveLevel;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.sensitiveLevel.vo.StandardsSensitiveLevelPageReqVO;
import com.datamaster.module.standards.dal.dataobject.sensitiveLevel.StandardsSensitiveLevelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 敏感等级Mapper接口
 *
 * @author Chaos
 * @date 2025-01-21
 */
public interface StandardsSensitiveLevelMapper extends BaseMapperX<StandardsSensitiveLevelDO> {

    default PageResult<StandardsSensitiveLevelDO> selectPage(StandardsSensitiveLevelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsSensitiveLevelDO>()
                .likeIfPresent(StandardsSensitiveLevelDO::getSensitiveLevel, reqVO.getSensitiveLevel())
                .eqIfPresent(StandardsSensitiveLevelDO::getSensitiveRule, reqVO.getSensitiveRule())
                .eqIfPresent(StandardsSensitiveLevelDO::getStartCharLoc, reqVO.getStartCharLoc())
                .eqIfPresent(StandardsSensitiveLevelDO::getEndCharLoc, reqVO.getEndCharLoc())
                .eqIfPresent(StandardsSensitiveLevelDO::getMaskCharacter, reqVO.getMaskCharacter())
                .eqIfPresent(StandardsSensitiveLevelDO::getOnlineFlag, reqVO.getOnlineFlag())
                .eqIfPresent(StandardsSensitiveLevelDO::getDescription, reqVO.getDescription())
                .eqIfPresent(StandardsSensitiveLevelDO::getCreateTime, reqVO.getCreateTime())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

}
