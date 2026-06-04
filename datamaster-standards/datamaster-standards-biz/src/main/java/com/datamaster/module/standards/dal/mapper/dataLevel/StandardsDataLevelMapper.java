

package com.datamaster.module.standards.dal.mapper.dataLevel;

import com.datamaster.module.standards.dal.dataobject.dataLevel.StandardsDataLevelDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数据分级Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
public interface StandardsDataLevelMapper extends BaseMapperX<StandardsDataLevelDO> {

    default PageResult<StandardsDataLevelDO> selectPage(StandardsDataLevelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsDataLevelDO>()
                .likeIfPresent(StandardsDataLevelDO::getName, reqVO.getName())
                .likeIfPresent(StandardsDataLevelDO::getShortName, reqVO.getShortName())
                .eqIfPresent(StandardsDataLevelDO::getSensitiveLevel, reqVO.getSensitiveLevel())
                .eqIfPresent(StandardsDataLevelDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(StandardsDataLevelDO::getDescription, reqVO.getDescription())
                .eqIfPresent(StandardsDataLevelDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(StandardsDataLevelDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getProjectId() != null, StandardsDataLevelDO::getProjectId, reqVO.getProjectId())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsDataLevelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
