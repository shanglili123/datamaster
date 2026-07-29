

package com.datamaster.quality.service.qa;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.quality.controller.qa.vo.QualityTaskPageReqVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskRespVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskDO;

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

}
