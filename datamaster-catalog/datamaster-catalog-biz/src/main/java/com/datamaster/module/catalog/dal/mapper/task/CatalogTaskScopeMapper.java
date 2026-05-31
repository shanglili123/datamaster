package com.datamaster.module.catalog.dal.mapper.task;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopePageReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskScopeDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 采集范围Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface CatalogTaskScopeMapper extends BaseMapperX<CatalogTaskScopeDO> {

    default PageResult<CatalogTaskScopeDO> selectPage(CatalogTaskScopePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CatalogTaskScopeDO>()
                .eqIfPresent(CatalogTaskScopeDO::getTaskId, reqVO.getTaskId())
                .likeIfPresent(CatalogTaskScopeDO::getDbName, reqVO.getDbName())
                .likeIfPresent(CatalogTaskScopeDO::getSchemaName, reqVO.getSchemaName())
                .eqIfPresent(CatalogTaskScopeDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(CatalogTaskScopeDO::getDescription, reqVO.getDescription())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CatalogTaskScopeDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
