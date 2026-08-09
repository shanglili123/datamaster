

package com.datamaster.metadata.dal.mapper.qa;

import org.apache.commons.lang3.StringUtils;
import com.datamaster.metadata.dal.dataobject.qa.QualityLogDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.metadata.controller.qa.vo.QualityLogPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 质量探查日志Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
public interface QualityLogMapper extends BaseMapperX<QualityLogDO> {

//    default PageResult<QualityLogDO> selectPage(QualityLogPageReqVO reqVO) {
//        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
//        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
//
//        // 构造动态查询条件
//        return selectPage(reqVO, new LambdaQueryWrapperX<QualityLogDO>()
//                .likeIfPresent(QualityLogDO::getName, reqVO.getName())
//                .eqIfPresent(QualityLogDO::getSuccessFlag, reqVO.getSuccessFlag())
//                .eqIfPresent(QualityLogDO::getStartTime, reqVO.getStartTime())
//                .eqIfPresent(QualityLogDO::getEndTime, reqVO.getEndTime())
//                .eqIfPresent(QualityLogDO::getQualityId, reqVO.getQualityId())
//                .eqIfPresent(QualityLogDO::getScore, reqVO.getScore())
//                .eqIfPresent(QualityLogDO::getProblemData, reqVO.getProblemData())
//                .eqIfPresent(QualityLogDO::getCreateTime, reqVO.getCreateTime())
//                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
//                // .likeIfPresent(QualityLogDO::getName, reqVO.getName())
//                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
//    }

    default PageResult<QualityLogDO> selectPage(QualityLogPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList(
                "id",
                "name",
                "success_flag",
                "start_time",
                "end_time",
                "quality_id",
                "score",
                "problem_data",
                "create_time",
                "update_time"
        ));

        MPJLambdaWrapperX<QualityLogDO> wrapper = new MPJLambdaWrapperX<>();
        wrapper.selectAll(QualityLogDO.class)
                .innerJoin("COL_QUALITY_TASK t2 ON t.QUALITY_ID = t2.ID AND t2.DEL_FLAG = '0' AND t2.ASSET_FLAG = '0'")
                .likeIfExists(QualityLogDO::getName, reqVO.getName())
                .eqIfExists(QualityLogDO::getSuccessFlag, reqVO.getSuccessFlag())
                .eqIfExists(QualityLogDO::getStartTime, reqVO.getStartTime())
                .eqIfExists(QualityLogDO::getEndTime, reqVO.getEndTime())
                .eqIfExists(QualityLogDO::getQualityId, reqVO.getQualityId())
                .eqIfExists(QualityLogDO::getScore, reqVO.getScore())
                .eqIfExists(QualityLogDO::getProblemData, reqVO.getProblemData())
                .eqIfExists(QualityLogDO::getCreateTime, reqVO.getCreateTime());
        // 动态排序处理
        String orderByColumn = reqVO.getOrderByColumn();
        Boolean isAsc = StringUtils.equalsAny(reqVO.getIsAsc(), "asc", "ascending");
        if (StringUtils.isNotBlank(orderByColumn) && allowedColumns.contains(orderByColumn)) {
            wrapper.orderBy(true, Boolean.TRUE.equals(isAsc), orderByColumn);
        } else {
            wrapper.orderByDesc(QualityLogDO::getStartTime,
                    QualityLogDO::getEndTime,
                    QualityLogDO::getUpdateTime,
                    QualityLogDO::getId);
        }
        return selectJoinPage(reqVO, QualityLogDO.class, wrapper);
    }

    default QualityLogDO selectPrevLogByIdWithWrapper(Long id) {
        // 1) 先拿当前记录的关键字段
        QualityLogDO cur = this.selectById(id);
        if (cur == null || cur.getQualityId() == null || cur.getStartTime() == null) {
            return null;
        }

        // 2) 构造 wrapper：同一 QUALITY_ID，下一个更“早”的一条
        MPJLambdaWrapperX<QualityLogDO> wrapper = new MPJLambdaWrapperX<>();
        wrapper.selectAll(QualityLogDO.class)
                .eq(QualityLogDO::getQualityId, cur.getQualityId())
                .eq(QualityLogDO::getDelFlag, "0")
                .eq(QualityLogDO::getValidFlag, "1")
                // (start_time < 当前) OR (start_time = 当前 AND id <> 当前)
                .and(w -> w.lt(QualityLogDO::getStartTime, cur.getStartTime())
                        .or(x -> x.eq(QualityLogDO::getStartTime, cur.getStartTime())
                                .ne(QualityLogDO::getId, id)))
                // 时间倒序，保证“最近的一条早于当前”
                .orderByDesc(QualityLogDO::getStartTime,
                        QualityLogDO::getEndTime,
                        QualityLogDO::getUpdateTime);

        // 3) 用分页只取一条（与你现有 selectPage(reqVO, wrapper) 兼容）
        QualityLogPageReqVO req = new QualityLogPageReqVO();
        req.setPageNum(1);
        req.setPageSize(1);

        PageResult<QualityLogDO> page = selectPage(req, wrapper);
        return (page == null || page.getRows() == null || page.getRows().isEmpty())
                ? null
                : (QualityLogDO)page.getRows().get(0);
    }
}
