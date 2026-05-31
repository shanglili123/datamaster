

package com.datamaster.module.collector.service.qa;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluatePageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluateRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluateSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskEvaluateDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据质量任务-评测规则Service接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface ICollectorQualityTaskEvaluateService extends IService<CollectorQualityTaskEvaluateDO> {

    /**
     * 获得数据质量任务-评测规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据质量任务-评测规则分页列表
     */
    PageResult<CollectorQualityTaskEvaluateDO> getCollectorQualityTaskEvaluatePage(CollectorQualityTaskEvaluatePageReqVO pageReqVO);

    /**
     * 创建数据质量任务-评测规则
     *
     * @param createReqVO 数据质量任务-评测规则信息
     * @return 数据质量任务-评测规则编号
     */
    Long createCollectorQualityTaskEvaluate(CollectorQualityTaskEvaluateSaveReqVO createReqVO);

    /**
     * 更新数据质量任务-评测规则
     *
     * @param updateReqVO 数据质量任务-评测规则信息
     */
    int updateCollectorQualityTaskEvaluate(CollectorQualityTaskEvaluateSaveReqVO updateReqVO);

    /**
     * 删除数据质量任务-评测规则
     *
     * @param idList 数据质量任务-评测规则编号
     */
    int removeCollectorQualityTaskEvaluate(Collection<Long> idList);

    /**
     * 获得数据质量任务-评测规则详情
     *
     * @param id 数据质量任务-评测规则编号
     * @return 数据质量任务-评测规则
     */
    CollectorQualityTaskEvaluateDO getCollectorQualityTaskEvaluateById(Long id);

    /**
     * 获得全部数据质量任务-评测规则列表
     *
     * @return 数据质量任务-评测规则列表
     */
    List<CollectorQualityTaskEvaluateDO> getCollectorQualityTaskEvaluateList();

    /**
     * 获得全部数据质量任务-评测规则 Map
     *
     * @return 数据质量任务-评测规则 Map
     */
    Map<Long, CollectorQualityTaskEvaluateDO> getCollectorQualityTaskEvaluateMap();


    /**
     * 导入数据质量任务-评测规则数据
     *
     * @param importExcelList 数据质量任务-评测规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorQualityTaskEvaluate(List<CollectorQualityTaskEvaluateRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
