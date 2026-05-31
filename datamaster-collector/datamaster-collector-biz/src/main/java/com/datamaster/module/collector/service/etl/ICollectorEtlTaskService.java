

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据集成任务Service接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface ICollectorEtlTaskService extends IService<CollectorEtlTaskDO> {

    /**
     * 获得数据集成任务分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成任务分页列表
     */
    PageResult<CollectorEtlTaskDO> getCollectorEtlTaskPage(CollectorEtlTaskPageReqVO pageReqVO);

    /**
     * 创建数据集成任务
     *
     * @param createReqVO 数据集成任务信息
     * @return 数据集成任务编号
     */
    Long createCollectorEtlTask(CollectorEtlTaskSaveReqVO createReqVO);

    /**
     * 更新数据集成任务
     *
     * @param updateReqVO 数据集成任务信息
     */
    int updateCollectorEtlTask(CollectorEtlTaskSaveReqVO updateReqVO);

    /**
     * 删除数据集成任务
     *
     * @param idList 数据集成任务编号
     */
    int removeCollectorEtlTask(Collection<Long> idList);

    /**
     * 获得数据集成任务详情
     *
     * @param id 数据集成任务编号
     * @return 数据集成任务
     */
    CollectorEtlTaskRespVO getCollectorEtlTaskById(Long id);

    /**
     * 获得全部数据集成任务列表
     *
     * @return 数据集成任务列表
     */
    List<CollectorEtlTaskDO> getCollectorEtlTaskList();

    /**
     * 获得全部数据集成任务 Map
     *
     * @return 数据集成任务 Map
     */
    Map<Long, CollectorEtlTaskDO> getCollectorEtlTaskMap();


    /**
     * 导入数据集成任务数据
     *
     * @param importExcelList 数据集成任务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorEtlTask(List<CollectorEtlTaskRespVO> importExcelList, boolean isUpdateSupport, String operName);

    PageResult<CollectorEtlTaskRespVO> getCollectorEtlTaskPageList(CollectorEtlTaskPageReqVO CollectorEtlTask);

    CollectorEtlTaskSaveReqVO createProcessDefinition(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    Long getNodeUniqueKey(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    Map<String, Object> updateReleaseTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    CollectorEtlTaskSaveReqVO updateProcessDefinition(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    Map<String, Object> releaseTaskCrontab(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    CollectorEtlTaskUpdateQueryRespVO getuUpdateQueryInfo(Long id);

    /**
     * 通过任务编码获取任务id
     *
     * @param taskCode
     * @return
     */
    Long getTaskIdByTaskCode(String taskCode);

    /**
     * 通过任务编码获取任务信息
     *
     * @param taskCode
     * @return
     */
    CollectorEtlTaskRespDTO getTaskByTaskCode(String taskCode);

    List<CollectorEtlTaskTreeRespVO> getCollectorEtlTaskListTree(CollectorEtlTaskPageReqVO CollectorEtlTask);

    AjaxResult startCollectorEtlTask(Long id);

    List<CollectorEtlTaskRespVO> getSubTaskStatusList(CollectorEtlTaskPageReqVO CollectorEtlTask);

    Map<String, Object> updateReleaseJobTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    Map<String, Object> updateReleaseSchedule(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    /**
     * 新增ETL任务
     * @param CollectorEtlNewNodeSaveReqVO
     * @return
     */
    CollectorEtlTaskSaveReqVO createEtlTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    /**
     * 修改ETL任务
     * @param CollectorEtlNewNodeSaveReqVO
     * @return
     */
    CollectorEtlTaskSaveReqVO updateEtlTask(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);
    CollectorEtlNewNodeSaveReqVO createEtlTaskFront(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    CollectorEtlTaskSaveReqVO createEtlTaskFrontPostposition(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    CollectorEtlTaskUpdateQueryRespVO getupdateQueryFront(Long id);

    CollectorEtlTaskSaveReqVO copyCreateEtl(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO);

    /**
     * 发布任务：创建/更新DS任务并上线
     * @param reqVO 任务配置
     * @return 保存结果
     */
    CollectorEtlTaskSaveReqVO publishTask(CollectorEtlNewNodeSaveReqVO reqVO);

    /**
     * 卸载任务：从DS下线
     * @param reqVO 包含任务ID
     */
    void unpublishTask(CollectorEtlNewNodeSaveReqVO reqVO);
}
