package com.datamaster.module.standards.dal.mapper.model;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedPageReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelMaterializedDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 物化模型记录Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsModelMaterializedMapper extends BaseMapperX<StandardsModelMaterializedDO> {

    default PageResult<StandardsModelMaterializedDO> selectPage(StandardsModelMaterializedPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsModelMaterializedDO>()
                .likeIfPresent(StandardsModelMaterializedDO::getModelName, reqVO.getModelName())
                .eqIfPresent(StandardsModelMaterializedDO::getModelAlias, reqVO.getModelAlias())
                .eqIfPresent(StandardsModelMaterializedDO::getModelId, reqVO.getModelId())
                .eqIfPresent(StandardsModelMaterializedDO::getStatus, reqVO.getStatus())
                .eqIfPresent(StandardsModelMaterializedDO::getMessage, reqVO.getMessage())
                .eqIfPresent(StandardsModelMaterializedDO::getSqlCommand, reqVO.getSqlCommand())
                .eqIfPresent(StandardsModelMaterializedDO::getDatasourceId, reqVO.getDatasourceId())
                .eqIfPresent(StandardsModelMaterializedDO::getDatasourceType, reqVO.getDatasourceType())
                .likeIfPresent(StandardsModelMaterializedDO::getDatasourceName, reqVO.getDatasourceName())
                .eqIfPresent(StandardsModelMaterializedDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(StandardsModelMaterializedDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getProjectId() != null, StandardsModelMaterializedDO::getProjectId, reqVO.getProjectId())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsModelMaterializedDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
