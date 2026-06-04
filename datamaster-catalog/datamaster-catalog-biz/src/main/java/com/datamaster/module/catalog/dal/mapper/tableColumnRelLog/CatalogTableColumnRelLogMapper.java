package com.datamaster.module.catalog.dal.mapper.tableColumnRelLog;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogPageReqVO;
import com.datamaster.module.catalog.dal.dataobject.tableColumnRelLog.CatalogTableColumnRelLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 元数据数据库与信息及字段信息关系-日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
public interface CatalogTableColumnRelLogMapper extends BaseMapperX<CatalogTableColumnRelLogDO> {

    default PageResult<CatalogTableColumnRelLogDO> selectPage(CatalogTableColumnRelLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CatalogTableColumnRelLogDO>()
                .eqIfPresent(CatalogTableColumnRelLogDO::getDataType, reqVO.getDataType())
                .eqIfPresent(CatalogTableColumnRelLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CatalogTableColumnRelLogDO::getDbId, reqVO.getDbId())
                .eqIfPresent(CatalogTableColumnRelLogDO::getDbVersion, reqVO.getDbVersion())
                .eqIfPresent(CatalogTableColumnRelLogDO::getTableId, reqVO.getTableId())
                .eqIfPresent(CatalogTableColumnRelLogDO::getTableVersion, reqVO.getTableVersion())
                .eqIfPresent(CatalogTableColumnRelLogDO::getColumnId, reqVO.getColumnId())
                .eqIfPresent(CatalogTableColumnRelLogDO::getColumnVersion, reqVO.getColumnVersion())
                .eqIfPresent(CatalogTableColumnRelLogDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(CatalogTableColumnRelLogDO::getDescription, reqVO.getDescription())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CatalogTableColumnRelLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .eq(reqVO.getProjectId() != null, CatalogTableColumnRelLogDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
