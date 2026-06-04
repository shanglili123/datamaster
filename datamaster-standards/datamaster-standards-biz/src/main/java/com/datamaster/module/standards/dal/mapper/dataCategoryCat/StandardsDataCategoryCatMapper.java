

package com.datamaster.module.standards.dal.mapper.dataCategoryCat;

import com.datamaster.module.standards.dal.dataobject.dataCategoryCat.StandardsDataCategoryCatDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数据分类-类目Mapper接口
 *
 * @author FXB
 * @date 2026-04-07
 */
public interface StandardsDataCategoryCatMapper extends BaseMapperX<StandardsDataCategoryCatDO> {

    default PageResult<StandardsDataCategoryCatDO> selectPage(StandardsDataCategoryCatPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsDataCategoryCatDO>()
                .likeIfPresent(StandardsDataCategoryCatDO::getName, reqVO.getName())
                .eqIfPresent(StandardsDataCategoryCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(StandardsDataCategoryCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(StandardsDataCategoryCatDO::getCode, reqVO.getCode())
                .eqIfPresent(StandardsDataCategoryCatDO::getDescription, reqVO.getDescription())
                .eqIfPresent(StandardsDataCategoryCatDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getProjectId() != null, StandardsDataCategoryCatDO::getProjectId, reqVO.getProjectId())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsDataCategoryCatDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
