

package com.datamaster.metadata.service.qa;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.metadata.api.qa.dto.QualitySummaryRespDTO;
import com.datamaster.metadata.controller.qa.vo.*;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 质量探查任务Service接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface IQualityTaskService extends IService<QualityTaskDO> {

    /**
     * 获取数据源表的最近一次质量探查结果摘要
     *
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 质量结果摘要
     */
    QualitySummaryRespDTO getLatestQualitySummary(Long datasourceId, String tableName);

    /**
     * 获得质量探查任务分页列表
     *
     * @param pageReqVO 分页请求
     * @return 质量探查任务分页列表
     */
    PageResult<QualityTaskDO> getQualityTaskPage(QualityTaskPageReqVO pageReqVO);

    /**
     * 创建质量探查任务
     *
     * @param createReqVO 质量探查任务信息
     * @return 质量探查任务编号
     */
    Long createQualityTask(QualityTaskSaveReqVO createReqVO);

    /**
     * 更新质量探查任务
     *
     * @param updateReqVO 质量探查任务信息
     */
    int updateQualityTask(QualityTaskSaveReqVO updateReqVO);

    /**
     * 删除质量探查任务
     *
     * @param idList 质量探查任务编号
     */
    int removeQualityTask(Collection<Long> idList);

    /**
     * 获得质量探查任务详情
     *
     * @param id 质量探查任务编号
     * @return 质量探查任务
     */
    QualityTaskRespVO getQualityTaskById(Long id);

    QualityTaskRespVO getQualityTaskAsset(QualityTaskAssetReqVO QualityTaskAssetReqVO);

    /**
     * 获得全部质量探查任务列表
     *
     * @return 质量探查任务列表
     */
    List<QualityTaskDO> getQualityTaskList();

    /**
     * 获得全部质量探查任务 Map
     *
     * @return 质量探查任务 Map
     */
    Map<Long, QualityTaskDO> getQualityTaskMap();


    /**
     * 导入质量探查任务数据
     *
     * @param importExcelList 质量探查任务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityTask(List<QualityTaskRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 检验数据格式是否有误
     * @param QualityTaskEvaluate
     */
    String verifyInterfaceValue(QualityTaskEvaluateSaveReqVO QualityTaskEvaluate);

    AjaxResult startQualityTask(Long id);

    boolean updateQualityTaskStatus(QualityTaskSaveReqVO daDiscoveryTask);

    JSONObject validationErrorDataSql(QualityTaskEvaluateSaveReqVO QualityTaskEvaluate);

    JSONObject validationValidDataSql(QualityTaskEvaluateSaveReqVO QualityTaskEvaluate);

    boolean updateDaDiscoveryTaskCronExpression(QualityTaskSaveReqVO daDiscoveryTask);
}
