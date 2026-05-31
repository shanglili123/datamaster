

package com.datamaster.module.collector.dal.mapper.etl;

import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成任务-日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlTaskLogMapper extends BaseMapperX<CollectorEtlTaskLogDO> {

    default PageResult<CollectorEtlTaskLogDO> selectPage(CollectorEtlTaskLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorEtlTaskLogDO>()
                .eqIfPresent(CollectorEtlTaskLogDO::getType, reqVO.getType())
                .likeIfPresent(CollectorEtlTaskLogDO::getName, reqVO.getName())
                .eqIfPresent(CollectorEtlTaskLogDO::getCode, reqVO.getCode())
                .eqIfPresent(CollectorEtlTaskLogDO::getVersion, reqVO.getVersion())
                .eqIfPresent(CollectorEtlTaskLogDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(CollectorEtlTaskLogDO::getProjectCode, reqVO.getProjectCode())
                .eqIfPresent(CollectorEtlTaskLogDO::getPersonCharge, reqVO.getPersonCharge())
                .eqIfPresent(CollectorEtlTaskLogDO::getLocations, reqVO.getLocations())
                .eqIfPresent(CollectorEtlTaskLogDO::getDescription, reqVO.getDescription())
                .eqIfPresent(CollectorEtlTaskLogDO::getTimeout, reqVO.getTimeout())
                .eqIfPresent(CollectorEtlTaskLogDO::getExtractionCount, reqVO.getExtractionCount())
                .eqIfPresent(CollectorEtlTaskLogDO::getWriteCount, reqVO.getWriteCount())
                .eqIfPresent(CollectorEtlTaskLogDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CollectorEtlTaskLogDO::getDsId, reqVO.getDsId())
                .eqIfPresent(CollectorEtlTaskLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CollectorEtlTaskLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    /**
     * 根据任务编码获取最大版本号
     *
     * @param taskCode
     * @return
     */
    Integer queryMaxVersionByCode(@Param("taskCode") String taskCode);
}
