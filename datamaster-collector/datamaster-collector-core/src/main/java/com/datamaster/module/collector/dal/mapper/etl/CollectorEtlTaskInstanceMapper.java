

package com.datamaster.module.collector.dal.mapper.etl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskInstancePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskInstanceTreeListReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskInstanceTreeListRespVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Collection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据集成任务实例Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface CollectorEtlTaskInstanceMapper extends BaseMapperX<CollectorEtlTaskInstanceDO> {

    @org.apache.ibatis.annotations.Select("SELECT id FROM col_etl_task WHERE id=#{taskId} FOR UPDATE")
    Long lockOwnerTask(@Param("taskId") Long taskId);

    @org.apache.ibatis.annotations.Select("SELECT * FROM col_etl_task_instance WHERE ds_id=#{dsId} FOR UPDATE")
    CollectorEtlTaskInstanceDO lockBySchedulerId(@Param("dsId") Long dsId);

    @org.apache.ibatis.annotations.Select("SELECT * FROM col_etl_task_instance WHERE task_id=#{taskId} AND del_flag='0' " +
            "AND ds_id IS NOT NULL AND parent_task_instance_id IS NULL AND (sub_task_flag IS NULL OR sub_task_flag <> '1') " +
            "ORDER BY ds_id DESC, id DESC LIMIT 1")
    CollectorEtlTaskInstanceDO latestMainInstance(@Param("taskId") Long taskId);

    @org.apache.ibatis.annotations.Select({"<script>",
            "SELECT i.* FROM col_etl_task_instance i WHERE i.del_flag='0' AND i.parent_task_instance_id IS NULL",
            "AND (i.sub_task_flag IS NULL OR i.sub_task_flag &lt;&gt; '1') AND i.task_id IN",
            "<foreach collection='taskIds' item='taskId' open='(' separator=',' close=')'>#{taskId}</foreach>",
            "AND i.ds_id=(SELECT MAX(m.ds_id) FROM col_etl_task_instance m WHERE m.task_id=i.task_id",
            "AND m.del_flag='0' AND m.parent_task_instance_id IS NULL AND (m.sub_task_flag IS NULL OR m.sub_task_flag &lt;&gt; '1'))",
            "</script>"})
    List<CollectorEtlTaskInstanceDO> latestMainInstances(@Param("taskIds") Collection<Long> taskIds);

    default PageResult<CollectorEtlTaskInstanceDO> selectPage(CollectorEtlTaskInstancePageReqVO reqVO) {

        MPJLambdaWrapper<CollectorEtlTaskInstanceDO> lambdaWrapper = new MPJLambdaWrapper();


        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        lambdaWrapper.selectAll(CollectorEtlTaskInstanceDO.class)
                .select("t3.NICK_NAME AS personChargeName")
                .leftJoin("SYSTEM_USER t3 ON t.PERSON_CHARGE = " + ("mysql".equals(MasterDataSourceConfig.getDatabaseType()) ? "CAST(t3.USER_ID AS CHAR)" : "CAST(t3.USER_ID AS VARCHAR)") + " AND t3.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()),CollectorEtlTaskInstanceDO::getName, reqVO.getName())
                .eq(StringUtils.isNotBlank(reqVO.getTaskType()),CollectorEtlTaskInstanceDO::getTaskType, reqVO.getTaskType())
                .eq(reqVO.getTaskId() !=null,CollectorEtlTaskInstanceDO::getTaskId, reqVO.getTaskId())
                .eq(StringUtils.isNotBlank(reqVO.getTaskCode()),CollectorEtlTaskInstanceDO::getTaskCode, reqVO.getTaskCode())
                .eq(reqVO.getTaskVersion() !=null,CollectorEtlTaskInstanceDO::getTaskVersion, reqVO.getTaskVersion())
                .eq(StringUtils.isNotBlank(reqVO.getPersonCharge()),CollectorEtlTaskInstanceDO::getPersonCharge, reqVO.getPersonCharge())
                .eq(reqVO.getSpaceId() !=null,CollectorEtlTaskInstanceDO::getSpaceId, reqVO.getSpaceId())
                .eq(StringUtils.isNotBlank(reqVO.getSpaceCode()),CollectorEtlTaskInstanceDO::getSpaceCode, reqVO.getSpaceCode())
                .eq(reqVO.getStartTime() !=null,CollectorEtlTaskInstanceDO::getStartTime, reqVO.getStartTime())
                .eq(reqVO.getEndTime() !=null,CollectorEtlTaskInstanceDO::getEndTime, reqVO.getEndTime())
                .eq(StringUtils.isNotBlank(reqVO.getCommandType()),CollectorEtlTaskInstanceDO::getCommandType, reqVO.getCommandType())
                .eq(reqVO.getMaxTryTimes() !=null,CollectorEtlTaskInstanceDO::getMaxTryTimes, reqVO.getMaxTryTimes())
                .eq(StringUtils.isNotBlank(reqVO.getFailureStrategy()),CollectorEtlTaskInstanceDO::getFailureStrategy, reqVO.getFailureStrategy())
                .eq(StringUtils.isNotBlank(reqVO.getSubTaskFlag()),CollectorEtlTaskInstanceDO::getSubTaskFlag, reqVO.getSubTaskFlag())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()),CollectorEtlTaskInstanceDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getDsId() !=null,CollectorEtlTaskInstanceDO::getDsId, reqVO.getDsId())
                .eq(reqVO.getCreateTime() !=null,CollectorEtlTaskInstanceDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CollectorEtlTaskInstanceDO::getStartTime);


        // 构造动态查询条件
        return selectJoinPage(reqVO, CollectorEtlTaskInstanceDO.class, lambdaWrapper);
    }

    CollectorEtlTaskInstanceDO selectOneNew(@Param("reqVO") CollectorEtlTaskInstanceDO reqVO);

    /**
     * 根据任务实例ID查询节点实例列表
     *
     * @param taskInstanceId
     * @return
     */
    List<CollectorEtlTaskInstanceTreeListRespVO> nodeListByTaskInstanceId(@Param("taskInstanceId") Long taskInstanceId);

    IPage<CollectorEtlTaskInstanceTreeListRespVO> treeList(Page page, @Param("params") CollectorEtlTaskInstanceTreeListReqVO params);

    /**
     * 获取子任务下所以节点实例
     *
     * @param taskInstanceId
     * @param nodeInstanceId
     * @return
     */
    List<CollectorEtlTaskInstanceTreeListRespVO> listSubNodeInstance(@Param("taskInstanceId") Long taskInstanceId, @Param("nodeInstanceId") Long nodeInstanceId);

    /**
     * 获取最新的任务实例
     *
     * @param taskIdList
     * @return
     */
    List<CollectorEtlTaskInstanceDO> getLastTaskInstance(@Param("taskIdList") List<Long> taskIdList);
}
