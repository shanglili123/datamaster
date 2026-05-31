

package com.datamaster.module.collector.service.qa;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.qa.vo.*;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据质量任务Service接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface ICollectorQualityTaskService extends IService<CollectorQualityTaskDO> {

    /**
     * 获得数据质量任务分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据质量任务分页列表
     */
    PageResult<CollectorQualityTaskDO> getCollectorQualityTaskPage(CollectorQualityTaskPageReqVO pageReqVO);

    /**
     * 创建数据质量任务
     *
     * @param createReqVO 数据质量任务信息
     * @return 数据质量任务编号
     */
    Long createCollectorQualityTask(CollectorQualityTaskSaveReqVO createReqVO);

    /**
     * 更新数据质量任务
     *
     * @param updateReqVO 数据质量任务信息
     */
    int updateCollectorQualityTask(CollectorQualityTaskSaveReqVO updateReqVO);

    /**
     * 删除数据质量任务
     *
     * @param idList 数据质量任务编号
     */
    int removeCollectorQualityTask(Collection<Long> idList);

    /**
     * 获得数据质量任务详情
     *
     * @param id 数据质量任务编号
     * @return 数据质量任务
     */
    CollectorQualityTaskRespVO getCollectorQualityTaskById(Long id);

    CollectorQualityTaskRespVO getQualityTaskAsset(CollectorQualityTaskAssetReqVO CollectorQualityTaskAssetReqVO);

    /**
     * 获得全部数据质量任务列表
     *
     * @return 数据质量任务列表
     */
    List<CollectorQualityTaskDO> getCollectorQualityTaskList();

    /**
     * 获得全部数据质量任务 Map
     *
     * @return 数据质量任务 Map
     */
    Map<Long, CollectorQualityTaskDO> getCollectorQualityTaskMap();


    /**
     * 导入数据质量任务数据
     *
     * @param importExcelList 数据质量任务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorQualityTask(List<CollectorQualityTaskRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 检验数据格式是否有误
     * @param CollectorQualityTaskEvaluate
     */
    String verifyInterfaceValue(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate);

    AjaxResult startCollectorQualityTask(Long id);

    boolean updateCollectorQualityTaskStatus(CollectorQualityTaskSaveReqVO daDiscoveryTask);

    JSONObject validationErrorDataSql(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate);

    JSONObject validationValidDataSql(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate);

    boolean updateDaDiscoveryTaskCronExpression(CollectorQualityTaskSaveReqVO daDiscoveryTask);
}
