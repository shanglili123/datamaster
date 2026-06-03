

package com.datamaster.quality.service.qa;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.quality.controller.qa.vo.QualityTaskEvaluatePageReqVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskEvaluateRespVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskEvaluateSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskEvaluateDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据质量任务-评测规则Service接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface IQualityTaskEvaluateService extends IService<QualityTaskEvaluateDO> {

    /**
     * 获得数据质量任务-评测规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据质量任务-评测规则分页列表
     */
    PageResult<QualityTaskEvaluateDO> getQualityTaskEvaluatePage(QualityTaskEvaluatePageReqVO pageReqVO);

    /**
     * 创建数据质量任务-评测规则
     *
     * @param createReqVO 数据质量任务-评测规则信息
     * @return 数据质量任务-评测规则编号
     */
    Long createQualityTaskEvaluate(QualityTaskEvaluateSaveReqVO createReqVO);

    /**
     * 更新数据质量任务-评测规则
     *
     * @param updateReqVO 数据质量任务-评测规则信息
     */
    int updateQualityTaskEvaluate(QualityTaskEvaluateSaveReqVO updateReqVO);

    /**
     * 删除数据质量任务-评测规则
     *
     * @param idList 数据质量任务-评测规则编号
     */
    int removeQualityTaskEvaluate(Collection<Long> idList);

    /**
     * 获得数据质量任务-评测规则详情
     *
     * @param id 数据质量任务-评测规则编号
     * @return 数据质量任务-评测规则
     */
    QualityTaskEvaluateDO getQualityTaskEvaluateById(Long id);

    List<QualityTaskEvaluateDO> getQualityTaskEvaluateList(List<Long> idList);

    /**
     * 获得全部数据质量任务-评测规则列表
     *
     * @return 数据质量任务-评测规则列表
     */
    List<QualityTaskEvaluateDO> getQualityTaskEvaluateList();

    /**
     * 获得全部数据质量任务-评测规则 Map
     *
     * @return 数据质量任务-评测规则 Map
     */
    Map<Long, QualityTaskEvaluateDO> getQualityTaskEvaluateMap();


    /**
     * 导入数据质量任务-评测规则数据
     *
     * @param importExcelList 数据质量任务-评测规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityTaskEvaluate(List<QualityTaskEvaluateRespVO> importExcelList, boolean isUpdateSupport, String operName);
}
