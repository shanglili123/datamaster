package com.datamaster.module.catalog.dal.mapper.tableLog;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogPageReqVO;
import com.datamaster.module.catalog.dal.dataobject.tableLog.CatalogTableLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 元数据信息 - 日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
public interface CatalogTableLogMapper extends BaseMapperX<CatalogTableLogDO> {

    default PageResult<CatalogTableLogDO> selectPage(CatalogTableLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CatalogTableLogDO>()
                .eqIfPresent(CatalogTableLogDO::getDataType, reqVO.getDataType())
                .eqIfPresent(CatalogTableLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CatalogTableLogDO::getTableId, reqVO.getTableId())
                .eqIfPresent(CatalogTableLogDO::getVersion, reqVO.getVersion())
                .eqIfPresent(CatalogTableLogDO::getDbId, reqVO.getDbId())
                .eqIfPresent(CatalogTableLogDO::getDatasourceId, reqVO.getDatasourceId())
                .likeIfPresent(CatalogTableLogDO::getTableName, reqVO.getTableName())
                .eqIfPresent(CatalogTableLogDO::getTableComment, reqVO.getTableComment())
                .eqIfPresent(CatalogTableLogDO::getSafetyLevelId, reqVO.getSafetyLevelId())
                .likeIfPresent(CatalogTableLogDO::getDbName, reqVO.getDbName())
                .likeIfPresent(CatalogTableLogDO::getSchemaName, reqVO.getSchemaName())
                .eqIfPresent(CatalogTableLogDO::getStorageType, reqVO.getStorageType())
                .eqIfPresent(CatalogTableLogDO::getStorageSize, reqVO.getStorageSize())
                .eqIfPresent(CatalogTableLogDO::getBusinessLeader, reqVO.getBusinessLeader())
                .eqIfPresent(CatalogTableLogDO::getBusinessLeaderPhone, reqVO.getBusinessLeaderPhone())
                .eqIfPresent(CatalogTableLogDO::getTechLeader, reqVO.getTechLeader())
                .eqIfPresent(CatalogTableLogDO::getTechLeaderPhone, reqVO.getTechLeaderPhone())
                .eqIfPresent(CatalogTableLogDO::getMasterFlag, reqVO.getMasterFlag())
                .eqIfPresent(CatalogTableLogDO::getTempFlag, reqVO.getTempFlag())
                .eqIfPresent(CatalogTableLogDO::getDataQuality, reqVO.getDataQuality())
                .eqIfPresent(CatalogTableLogDO::getUpdateType, reqVO.getUpdateType())
                .eqIfPresent(CatalogTableLogDO::getUpdateMsg, reqVO.getUpdateMsg())
                .eqIfPresent(CatalogTableLogDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(CatalogTableLogDO::getDescription, reqVO.getDescription())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CatalogTableLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .eq(reqVO.getProjectId() != null, CatalogTableLogDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
