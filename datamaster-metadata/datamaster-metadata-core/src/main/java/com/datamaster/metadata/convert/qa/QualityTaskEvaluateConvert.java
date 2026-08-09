

package com.datamaster.metadata.convert.qa;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluatePageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluateRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluateSaveReqVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskEvaluateDO;

import java.util.List;

/**
 * 质量探查任务-评测规则 Convert
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Mapper
public interface QualityTaskEvaluateConvert {
    QualityTaskEvaluateConvert INSTANCE = Mappers.getMapper(QualityTaskEvaluateConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param QualityTaskEvaluatePageReqVO 请求参数
     * @return QualityTaskEvaluateDO
     */
     QualityTaskEvaluateDO convertToDO(QualityTaskEvaluatePageReqVO QualityTaskEvaluatePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param QualityTaskEvaluateSaveReqVO 保存请求参数
     * @return QualityTaskEvaluateDO
     */
     QualityTaskEvaluateDO convertToDO(QualityTaskEvaluateSaveReqVO QualityTaskEvaluateSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param QualityTaskEvaluateDO 实体对象
     * @return QualityTaskEvaluateRespVO
     */
     QualityTaskEvaluateRespVO convertToRespVO(QualityTaskEvaluateDO QualityTaskEvaluateDO);

    /**
     * DOList 转换为 RespVOList
     * @param QualityTaskEvaluateDOList 实体对象列表
     * @return List<QualityTaskEvaluateRespVO>
     */
     List<QualityTaskEvaluateRespVO> convertToRespVOList(List<QualityTaskEvaluateDO> QualityTaskEvaluateDOList);
}
