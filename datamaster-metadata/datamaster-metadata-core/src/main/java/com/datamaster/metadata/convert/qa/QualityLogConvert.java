

package com.datamaster.metadata.convert.qa;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.metadata.controller.qa.vo.QualityLogPageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityLogRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityLogSaveReqVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityLogDO;

import java.util.List;

/**
 * 质量探查日志 Convert
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Mapper
public interface QualityLogConvert {
    QualityLogConvert INSTANCE = Mappers.getMapper(QualityLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param QualityLogPageReqVO 请求参数
     * @return QualityLogDO
     */
     QualityLogDO convertToDO(QualityLogPageReqVO QualityLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param QualityLogSaveReqVO 保存请求参数
     * @return QualityLogDO
     */
     QualityLogDO convertToDO(QualityLogSaveReqVO QualityLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param QualityLogDO 实体对象
     * @return QualityLogRespVO
     */
     QualityLogRespVO convertToRespVO(QualityLogDO QualityLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param QualityLogDOList 实体对象列表
     * @return List<QualityLogRespVO>
     */
     List<QualityLogRespVO> convertToRespVOList(List<QualityLogDO> QualityLogDOList);
}
