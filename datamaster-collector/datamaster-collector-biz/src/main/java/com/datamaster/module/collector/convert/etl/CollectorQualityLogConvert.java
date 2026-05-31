

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorQualityLogDO;

import java.util.List;

/**
 * 数据质量日志 Convert
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Mapper
public interface CollectorQualityLogConvert {
    CollectorQualityLogConvert INSTANCE = Mappers.getMapper(CollectorQualityLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorQualityLogPageReqVO 请求参数
     * @return CollectorQualityLogDO
     */
     CollectorQualityLogDO convertToDO(CollectorQualityLogPageReqVO CollectorQualityLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorQualityLogSaveReqVO 保存请求参数
     * @return CollectorQualityLogDO
     */
     CollectorQualityLogDO convertToDO(CollectorQualityLogSaveReqVO CollectorQualityLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorQualityLogDO 实体对象
     * @return CollectorQualityLogRespVO
     */
     CollectorQualityLogRespVO convertToRespVO(CollectorQualityLogDO CollectorQualityLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorQualityLogDOList 实体对象列表
     * @return List<CollectorQualityLogRespVO>
     */
     List<CollectorQualityLogRespVO> convertToRespVOList(List<CollectorQualityLogDO> CollectorQualityLogDOList);
}
