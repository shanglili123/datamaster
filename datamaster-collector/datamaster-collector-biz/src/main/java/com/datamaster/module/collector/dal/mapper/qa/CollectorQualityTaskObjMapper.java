

package com.datamaster.module.collector.dal.mapper.qa;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjPageReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskObjDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据质量任务-稽查对象Mapper接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface CollectorQualityTaskObjMapper extends BaseMapperX<CollectorQualityTaskObjDO> {

    default PageResult<CollectorQualityTaskObjDO> selectPage(CollectorQualityTaskObjPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorQualityTaskObjDO>()
                .likeIfPresent(CollectorQualityTaskObjDO::getName, reqVO.getName())
                .eqIfPresent(CollectorQualityTaskObjDO::getDatasourceId, reqVO.getDatasourceId())
                .likeIfPresent(CollectorQualityTaskObjDO::getTableName, reqVO.getTableName())
                .eqIfPresent(CollectorQualityTaskObjDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorQualityTaskObjDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
