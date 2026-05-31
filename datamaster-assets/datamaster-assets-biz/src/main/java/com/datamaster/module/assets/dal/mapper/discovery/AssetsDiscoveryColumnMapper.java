package com.datamaster.module.assets.dal.mapper.discovery;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnPageReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryColumnDO;
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
public interface AssetsDiscoveryColumnMapper extends BaseMapperX<AssetsDiscoveryColumnDO> {

    default PageResult<AssetsDiscoveryColumnDO> selectPage(AssetsDiscoveryColumnPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsDiscoveryColumnDO>()
                .eqIfPresent(AssetsDiscoveryColumnDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AssetsDiscoveryColumnDO::getTableId, reqVO.getTableId())
                .likeIfPresent(AssetsDiscoveryColumnDO::getColumnName, reqVO.getColumnName())
                .eqIfPresent(AssetsDiscoveryColumnDO::getColumnComment, reqVO.getColumnComment())
                .eqIfPresent(AssetsDiscoveryColumnDO::getColumnType, reqVO.getColumnType())
                .eqIfPresent(AssetsDiscoveryColumnDO::getColumnLength, reqVO.getColumnLength())
                .eqIfPresent(AssetsDiscoveryColumnDO::getColumnScale, reqVO.getColumnScale())
                .eqIfPresent(AssetsDiscoveryColumnDO::getNullableFlag, reqVO.getNullableFlag())
                .eqIfPresent(AssetsDiscoveryColumnDO::getPkFlag, reqVO.getPkFlag())
                .eqIfPresent(AssetsDiscoveryColumnDO::getDefaultValue, reqVO.getDefaultValue())
                .eqIfPresent(AssetsDiscoveryColumnDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsDiscoveryColumnDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
