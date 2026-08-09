

package com.datamaster.metadata.convert.qa;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.metadata.controller.qa.vo.QualityTaskPageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskSaveReqVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskDO;

import java.util.List;

/**
 * 质量探查任务 Convert
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Mapper
public interface QualityTaskConvert {
    QualityTaskConvert INSTANCE = Mappers.getMapper(QualityTaskConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param QualityTaskPageReqVO 请求参数
     * @return QualityTaskDO
     */
     QualityTaskDO convertToDO(QualityTaskPageReqVO QualityTaskPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param QualityTaskSaveReqVO 保存请求参数
     * @return QualityTaskDO
     */
     QualityTaskDO convertToDO(QualityTaskSaveReqVO QualityTaskSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param QualityTaskDO 实体对象
     * @return QualityTaskRespVO
     */
     QualityTaskRespVO convertToRespVO(QualityTaskDO QualityTaskDO);

    /**
     * DOList 转换为 RespVOList
     * @param QualityTaskDOList 实体对象列表
     * @return List<QualityTaskRespVO>
     */
     List<QualityTaskRespVO> convertToRespVOList(List<QualityTaskDO> QualityTaskDOList);
}
