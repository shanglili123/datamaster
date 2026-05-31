package com.datamaster.module.assets.dal.mapper.discovery;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTablePageReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTableDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface AssetsDiscoveryTableMapper extends BaseMapperX<AssetsDiscoveryTableDO> {

    default PageResult<AssetsDiscoveryTableDO> selectPage(AssetsDiscoveryTablePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsDiscoveryTableDO>()
                .eqIfPresent(AssetsDiscoveryTableDO::getTaskId, reqVO.getTaskId())
                .likeIfPresent(AssetsDiscoveryTableDO::getTableName, reqVO.getTableName())
                .eqIfPresent(AssetsDiscoveryTableDO::getTableComment, reqVO.getTableComment())
                .eqIfPresent(AssetsDiscoveryTableDO::getDataCount, reqVO.getDataCount())
                .eqIfPresent(AssetsDiscoveryTableDO::getFieldCount, reqVO.getFieldCount())
                .eqIfPresent(AssetsDiscoveryTableDO::getChangeFlag, reqVO.getChangeFlag())
                .eqIfPresent(AssetsDiscoveryTableDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AssetsDiscoveryTableDO::getIgnoreFlag, reqVO.getIgnoreFlag())
                .eqIfPresent(AssetsDiscoveryTableDO::getCreateTime, reqVO.getCreateTime())
                // 新增的 keyword 模糊匹配
                .likeIfPresent(AssetsDiscoveryTableDO::getTableName, reqVO.getKeyword())
                .or()
                .likeIfPresent(AssetsDiscoveryTableDO::getTableComment, reqVO.getKeyword())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsDiscoveryTableDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
