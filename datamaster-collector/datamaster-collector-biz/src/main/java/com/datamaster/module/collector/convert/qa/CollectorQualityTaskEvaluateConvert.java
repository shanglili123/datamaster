

package com.datamaster.module.collector.convert.qa;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluatePageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluateRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluateSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskEvaluateDO;

import java.util.List;

/**
 * 数据质量任务-评测规则 Convert
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Mapper
public interface CollectorQualityTaskEvaluateConvert {
    CollectorQualityTaskEvaluateConvert INSTANCE = Mappers.getMapper(CollectorQualityTaskEvaluateConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorQualityTaskEvaluatePageReqVO 请求参数
     * @return CollectorQualityTaskEvaluateDO
     */
     CollectorQualityTaskEvaluateDO convertToDO(CollectorQualityTaskEvaluatePageReqVO CollectorQualityTaskEvaluatePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorQualityTaskEvaluateSaveReqVO 保存请求参数
     * @return CollectorQualityTaskEvaluateDO
     */
     CollectorQualityTaskEvaluateDO convertToDO(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluateSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorQualityTaskEvaluateDO 实体对象
     * @return CollectorQualityTaskEvaluateRespVO
     */
     CollectorQualityTaskEvaluateRespVO convertToRespVO(CollectorQualityTaskEvaluateDO CollectorQualityTaskEvaluateDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorQualityTaskEvaluateDOList 实体对象列表
     * @return List<CollectorQualityTaskEvaluateRespVO>
     */
     List<CollectorQualityTaskEvaluateRespVO> convertToRespVOList(List<CollectorQualityTaskEvaluateDO> CollectorQualityTaskEvaluateDOList);
}
