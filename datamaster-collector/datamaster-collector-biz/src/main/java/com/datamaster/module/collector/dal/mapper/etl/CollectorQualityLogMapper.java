

package com.datamaster.module.collector.dal.mapper.etl;

import org.apache.commons.lang3.StringUtils;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorQualityLogDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 数据质量日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
public interface CollectorQualityLogMapper extends BaseMapperX<CollectorQualityLogDO> {

//    default PageResult<CollectorQualityLogDO> selectPage(CollectorQualityLogPageReqVO reqVO) {
//        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
//        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
//
//        // 构造动态查询条件
//        return selectPage(reqVO, new LambdaQueryWrapperX<CollectorQualityLogDO>()
//                .likeIfPresent(CollectorQualityLogDO::getName, reqVO.getName())
//                .eqIfPresent(CollectorQualityLogDO::getSuccessFlag, reqVO.getSuccessFlag())
//                .eqIfPresent(CollectorQualityLogDO::getStartTime, reqVO.getStartTime())
//                .eqIfPresent(CollectorQualityLogDO::getEndTime, reqVO.getEndTime())
//                .eqIfPresent(CollectorQualityLogDO::getQualityId, reqVO.getQualityId())
//                .eqIfPresent(CollectorQualityLogDO::getScore, reqVO.getScore())
//                .eqIfPresent(CollectorQualityLogDO::getProblemData, reqVO.getProblemData())
//                .eqIfPresent(CollectorQualityLogDO::getCreateTime, reqVO.getCreateTime())
//                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
//                // .likeIfPresent(CollectorQualityLogDO::getName, reqVO.getName())
//                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
//    }

    default PageResult<CollectorQualityLogDO> selectPage(CollectorQualityLogPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapperX<CollectorQualityLogDO> wrapper = new MPJLambdaWrapperX<>();
        wrapper.selectAll(CollectorQualityLogDO.class)
                .innerJoin("COL_QUALITY_TASK t2 ON t.QUALITY_ID = t2.ID AND t2.DEL_FLAG = '0' AND t2.ASSET_FLAG = '0'")
                .likeIfExists(CollectorQualityLogDO::getName, reqVO.getName())
                .eqIfExists(CollectorQualityLogDO::getSuccessFlag, reqVO.getSuccessFlag())
                .eqIfExists(CollectorQualityLogDO::getStartTime, reqVO.getStartTime())
                .eqIfExists(CollectorQualityLogDO::getEndTime, reqVO.getEndTime())
                .eqIfExists(CollectorQualityLogDO::getQualityId, reqVO.getQualityId())
                .eqIfExists(CollectorQualityLogDO::getScore, reqVO.getScore())
                .eqIfExists(CollectorQualityLogDO::getProblemData, reqVO.getProblemData())
                .eqIfExists(CollectorQualityLogDO::getCreateTime, reqVO.getCreateTime());
        // 动态排序处理
        String orderByColumn = reqVO.getOrderByColumn();
        Boolean isAsc = StringUtils.equals("asc", reqVO.getIsAsc());
        if (StringUtils.isNotBlank(orderByColumn) && allowedColumns.contains(orderByColumn)) {
            wrapper.orderBy(true, Boolean.TRUE.equals(isAsc), orderByColumn);
        }
        return selectPage(reqVO, wrapper);
    }

    default CollectorQualityLogDO selectPrevLogByIdWithWrapper(String id) {
        // 1) 先拿当前记录的关键字段
        CollectorQualityLogDO cur = this.selectById(id);
        if (cur == null || cur.getQualityId() == null || cur.getStartTime() == null) {
            return null;
        }

        // 2) 构造 wrapper：同一 QUALITY_ID，下一个更“早”的一条
        MPJLambdaWrapperX<CollectorQualityLogDO> wrapper = new MPJLambdaWrapperX<>();
        wrapper.selectAll(CollectorQualityLogDO.class)
                .eq(CollectorQualityLogDO::getQualityId, cur.getQualityId())
                .eq(CollectorQualityLogDO::getDelFlag, "0")
                .eq(CollectorQualityLogDO::getValidFlag, "1")
                // (start_time < 当前) OR (start_time = 当前 AND id <> 当前)
                .and(w -> w.lt(CollectorQualityLogDO::getStartTime, cur.getStartTime())
                        .or(x -> x.eq(CollectorQualityLogDO::getStartTime, cur.getStartTime())
                                .ne(CollectorQualityLogDO::getId, id)))
                // 时间倒序，保证“最近的一条早于当前”
                .orderByDesc(CollectorQualityLogDO::getStartTime,
                        CollectorQualityLogDO::getEndTime,
                        CollectorQualityLogDO::getUpdateTime);

        // 3) 用分页只取一条（与你现有 selectPage(reqVO, wrapper) 兼容）
        CollectorQualityLogPageReqVO req = new CollectorQualityLogPageReqVO();
        req.setPageNum(1);
        req.setPageSize(1);

        PageResult<CollectorQualityLogDO> page = selectPage(req, wrapper);
        return (page == null || page.getRows() == null || page.getRows().isEmpty())
                ? null
                : (CollectorQualityLogDO)page.getRows().get(0);
    }
}
