

package com.datamaster.metadata.service.qa;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluatePageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluateRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluateSaveReqVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskEvaluateDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 质量探查任务-评测规则Service接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface IQualityTaskEvaluateService extends IService<QualityTaskEvaluateDO> {

    /**
     * 获得质量探查任务-评测规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 质量探查任务-评测规则分页列表
     */
    PageResult<QualityTaskEvaluateDO> getQualityTaskEvaluatePage(QualityTaskEvaluatePageReqVO pageReqVO);

    /**
     * 创建质量探查任务-评测规则
     *
     * @param createReqVO 质量探查任务-评测规则信息
     * @return 质量探查任务-评测规则编号
     */
    Long createQualityTaskEvaluate(QualityTaskEvaluateSaveReqVO createReqVO);

    /**
     * 更新质量探查任务-评测规则
     *
     * @param updateReqVO 质量探查任务-评测规则信息
     */
    int updateQualityTaskEvaluate(QualityTaskEvaluateSaveReqVO updateReqVO);

    /**
     * 删除质量探查任务-评测规则
     *
     * @param idList 质量探查任务-评测规则编号
     */
    int removeQualityTaskEvaluate(Collection<Long> idList);

    /**
     * 获得质量探查任务-评测规则详情
     *
     * @param id 质量探查任务-评测规则编号
     * @return 质量探查任务-评测规则
     */
    QualityTaskEvaluateDO getQualityTaskEvaluateById(Long id);

    /**
     * 获得全部质量探查任务-评测规则列表
     *
     * @return 质量探查任务-评测规则列表
     */
    List<QualityTaskEvaluateDO> getQualityTaskEvaluateList();

    /**
     * 获得指定稽查对象ID集合的质量探查任务-评测规则列表
     *
     * @param objIdList 稽查对象ID集合
     * @return 质量探查任务-评测规则列表
     */
    List<QualityTaskEvaluateDO> getQualityTaskEvaluateList(List<Long> objIdList);

    /**
     * 获得全部质量探查任务-评测规则 Map
     *
     * @return 质量探查任务-评测规则 Map
     */
    Map<Long, QualityTaskEvaluateDO> getQualityTaskEvaluateMap();


    /**
     * 导入质量探查任务-评测规则数据
     *
     * @param importExcelList 质量探查任务-评测规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityTaskEvaluate(List<QualityTaskEvaluateRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
