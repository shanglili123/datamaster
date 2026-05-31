

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEvaluateLogDO;

import java.util.List;

/**
 * 评测规则结果 Convert
 *
 * @author DATAMASTER
 * @date 2025-07-21
 */
@Mapper
public interface CollectorEvaluateLogConvert {
    CollectorEvaluateLogConvert INSTANCE = Mappers.getMapper(CollectorEvaluateLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEvaluateLogPageReqVO 请求参数
     * @return CollectorEvaluateLogDO
     */
     CollectorEvaluateLogDO convertToDO(CollectorEvaluateLogPageReqVO CollectorEvaluateLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEvaluateLogSaveReqVO 保存请求参数
     * @return CollectorEvaluateLogDO
     */
     CollectorEvaluateLogDO convertToDO(CollectorEvaluateLogSaveReqVO CollectorEvaluateLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEvaluateLogDO 实体对象
     * @return CollectorEvaluateLogRespVO
     */
     CollectorEvaluateLogRespVO convertToRespVO(CollectorEvaluateLogDO CollectorEvaluateLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEvaluateLogDOList 实体对象列表
     * @return List<CollectorEvaluateLogRespVO>
     */
     List<CollectorEvaluateLogRespVO> convertToRespVOList(List<CollectorEvaluateLogDO> CollectorEvaluateLogDOList);
}
