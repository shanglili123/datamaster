package com.datamaster.module.catalog.dal.mapper.columnLog;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogPageReqVO;
import com.datamaster.module.catalog.dal.dataobject.columnLog.CatalogColumnLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 元数据字段信息 - 日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
public interface CatalogColumnLogMapper extends BaseMapperX<CatalogColumnLogDO> {

    default PageResult<CatalogColumnLogDO> selectPage(CatalogColumnLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CatalogColumnLogDO>()
                .eqIfPresent(CatalogColumnLogDO::getDataType, reqVO.getDataType())
                .eqIfPresent(CatalogColumnLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CatalogColumnLogDO::getColumnId, reqVO.getColumnId())
                .eqIfPresent(CatalogColumnLogDO::getVersion, reqVO.getVersion())
                .eqIfPresent(CatalogColumnLogDO::getDbId, reqVO.getDbId())
                .eqIfPresent(CatalogColumnLogDO::getTableId, reqVO.getTableId())
                .eqIfPresent(CatalogColumnLogDO::getDatasourceId, reqVO.getDatasourceId())
                .eqIfPresent(CatalogColumnLogDO::getSafetyLevelId, reqVO.getSafetyLevelId())
                .eqIfPresent(CatalogColumnLogDO::getDataElemId, reqVO.getDataElemId())
                .likeIfPresent(CatalogColumnLogDO::getColumnName, reqVO.getColumnName())
                .eqIfPresent(CatalogColumnLogDO::getColumnComment, reqVO.getColumnComment())
                .eqIfPresent(CatalogColumnLogDO::getColumnType, reqVO.getColumnType())
                .eqIfPresent(CatalogColumnLogDO::getColumnLength, reqVO.getColumnLength())
                .eqIfPresent(CatalogColumnLogDO::getColumnPrecision, reqVO.getColumnPrecision())
                .eqIfPresent(CatalogColumnLogDO::getColumnScale, reqVO.getColumnScale())
                .eqIfPresent(CatalogColumnLogDO::getDefaultValue, reqVO.getDefaultValue())
                .eqIfPresent(CatalogColumnLogDO::getPkFlag, reqVO.getPkFlag())
                .eqIfPresent(CatalogColumnLogDO::getFkFlag, reqVO.getFkFlag())
                .eqIfPresent(CatalogColumnLogDO::getNullableFlag, reqVO.getNullableFlag())
                .eqIfPresent(CatalogColumnLogDO::getBusDefinition, reqVO.getBusDefinition())
                .eqIfPresent(CatalogColumnLogDO::getMeasuringUnit, reqVO.getMeasuringUnit())
                .eqIfPresent(CatalogColumnLogDO::getDataQuality, reqVO.getDataQuality())
                .eqIfPresent(CatalogColumnLogDO::getUpdateType, reqVO.getUpdateType())
                .eqIfPresent(CatalogColumnLogDO::getUpdateMsg, reqVO.getUpdateMsg())
                .eqIfPresent(CatalogColumnLogDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(CatalogColumnLogDO::getDescription, reqVO.getDescription())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CatalogColumnLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
