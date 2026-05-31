package com.datamaster.module.catalog.dal.mapper.task;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogPageReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 采集任务实例-日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface CatalogTaskInstanceLogMapper extends BaseMapperX<CatalogTaskInstanceLogDO> {

    default PageResult<CatalogTaskInstanceLogDO> selectPage(CatalogTaskInstanceLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CatalogTaskInstanceLogDO>()
                .eqIfPresent(CatalogTaskInstanceLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CatalogTaskInstanceLogDO::getLogContent, reqVO.getLogContent())
                .eqIfPresent(CatalogTaskInstanceLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CatalogTaskInstanceLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
