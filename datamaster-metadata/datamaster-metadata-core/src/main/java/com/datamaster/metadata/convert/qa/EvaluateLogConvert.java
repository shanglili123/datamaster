

package com.datamaster.metadata.convert.qa;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.metadata.controller.qa.vo.EvaluateLogPageReqVO;
import com.datamaster.metadata.controller.qa.vo.EvaluateLogRespVO;
import com.datamaster.metadata.controller.qa.vo.EvaluateLogSaveReqVO;
import com.datamaster.metadata.dal.dataobject.qa.EvaluateLogDO;

import java.util.List;

/**
 * 评测规则结果 Convert
 *
 * @author DATAMASTER
 * @date 2025-07-21
 */
@Mapper
public interface EvaluateLogConvert {
    EvaluateLogConvert INSTANCE = Mappers.getMapper(EvaluateLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param EvaluateLogPageReqVO 请求参数
     * @return EvaluateLogDO
     */
     EvaluateLogDO convertToDO(EvaluateLogPageReqVO EvaluateLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param EvaluateLogSaveReqVO 保存请求参数
     * @return EvaluateLogDO
     */
     EvaluateLogDO convertToDO(EvaluateLogSaveReqVO EvaluateLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param EvaluateLogDO 实体对象
     * @return EvaluateLogRespVO
     */
     EvaluateLogRespVO convertToRespVO(EvaluateLogDO EvaluateLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param EvaluateLogDOList 实体对象列表
     * @return List<EvaluateLogRespVO>
     */
     List<EvaluateLogRespVO> convertToRespVOList(List<EvaluateLogDO> EvaluateLogDOList);
}
