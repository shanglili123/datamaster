

package com.datamaster.module.collector.dal.mapper.etl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.TaskCatEnum;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskRespVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据集成任务Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlTaskMapper extends BaseMapperX<CollectorEtlTaskDO> {

    IPage<CollectorEtlTaskRespVO> getCollectorEtlTaskPage(Page page, @Param("params") CollectorEtlTaskPageReqVO reqVO);

    default PageResult<CollectorEtlTaskDO> selectPage(CollectorEtlTaskPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<CollectorEtlTaskDO> lambdaWrapper = new MPJLambdaWrapper();

        String leftJoin = TaskCatEnum.findEnumByType(reqVO.getType()) + " t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'";

        lambdaWrapper.selectAll(CollectorEtlTaskDO.class)
                .select("t2.NAME AS catName",
                        "t3.CRON_EXPRESSION AS cronExpression"
                        ,"t3.STATUS AS schedulerState"
                        , "(SELECT MAX(ti.CREATE_TIME) FROM COL_ETL_TASK_INSTANCE ti WHERE ti.TASK_CODE = t.CODE AND ti.DEL_FLAG = '0') AS lastExecuteTime")
                .leftJoin(leftJoin)
                .leftJoin("COL_ETL_SCHEDULER t3 ON t.id = t3.task_id AND t3.DEL_FLAG = '0'")
                .eq(StringUtils.isNotBlank(reqVO.getType()), CollectorEtlTaskDO::getType, reqVO.getType())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), CollectorEtlTaskDO::getCatCode, reqVO.getCatCode())
                .like(StringUtils.isNotBlank(reqVO.getName()), CollectorEtlTaskDO::getName, reqVO.getName())
                .eq(StringUtils.isNotBlank(reqVO.getCode()), CollectorEtlTaskDO::getCode, reqVO.getCode())
                .ne(CollectorEtlTaskDO::getStatus,"-2")
                .ne(CollectorEtlTaskDO::getStatus,"-3")
                .eq(reqVO.getProjectId() != null, CollectorEtlTaskDO::getProjectId, reqVO.getProjectId())
                .eq(StringUtils.isNotBlank(reqVO.getProjectCode()), CollectorEtlTaskDO::getProjectCode, reqVO.getProjectCode())
                .eq(StringUtils.isNotBlank(reqVO.getPersonCharge()), CollectorEtlTaskDO::getPersonCharge, reqVO.getPersonCharge())
                .eq(StringUtils.isNotBlank(reqVO.getLocations()), CollectorEtlTaskDO::getLocations, reqVO.getLocations())
                .eq(StringUtils.isNotBlank(reqVO.getDescription()), CollectorEtlTaskDO::getDescription, reqVO.getDescription())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), CollectorEtlTaskDO::getStatus, reqVO.getStatus())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);

        // 构造动态查询条件
        return selectJoinPage(reqVO, CollectorEtlTaskDO.class, lambdaWrapper);
    }


    int checkTaskIdInSubTasks(Long id);

    int checkTaskIdInDatasource(@Param("datasourceIdList") List<Long> datasourceIdList,@Param("projectIdList") List<Long> projectIdList);

    int checkTaskIdInAsset(@Param("assetIdList") List<Long> assetIdList);
}
