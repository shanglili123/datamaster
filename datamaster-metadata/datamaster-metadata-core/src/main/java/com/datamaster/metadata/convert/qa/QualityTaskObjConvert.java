

package com.datamaster.metadata.convert.qa;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjPageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjSaveReqVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskObjDO;

import java.util.List;

/**
 * 质量探查任务-稽查对象 Convert
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Mapper
public interface QualityTaskObjConvert {
    QualityTaskObjConvert INSTANCE = Mappers.getMapper(QualityTaskObjConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param QualityTaskObjPageReqVO 请求参数
     * @return QualityTaskObjDO
     */
     QualityTaskObjDO convertToDO(QualityTaskObjPageReqVO QualityTaskObjPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param QualityTaskObjSaveReqVO 保存请求参数
     * @return QualityTaskObjDO
     */
     QualityTaskObjDO convertToDO(QualityTaskObjSaveReqVO QualityTaskObjSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param QualityTaskObjDO 实体对象
     * @return QualityTaskObjRespVO
     */
     QualityTaskObjRespVO convertToRespVO(QualityTaskObjDO QualityTaskObjDO);

    /**
     * DOList 转换为 RespVOList
     * @param QualityTaskObjDOList 实体对象列表
     * @return List<QualityTaskObjRespVO>
     */
     List<QualityTaskObjRespVO> convertToRespVOList(List<QualityTaskObjDO> QualityTaskObjDOList);
}
