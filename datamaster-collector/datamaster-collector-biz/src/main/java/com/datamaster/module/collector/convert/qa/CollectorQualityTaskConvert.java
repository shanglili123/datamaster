

package com.datamaster.module.collector.convert.qa;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskPageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskDO;

import java.util.List;

/**
 * 数据质量任务 Convert
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Mapper
public interface CollectorQualityTaskConvert {
    CollectorQualityTaskConvert INSTANCE = Mappers.getMapper(CollectorQualityTaskConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorQualityTaskPageReqVO 请求参数
     * @return CollectorQualityTaskDO
     */
     CollectorQualityTaskDO convertToDO(CollectorQualityTaskPageReqVO CollectorQualityTaskPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorQualityTaskSaveReqVO 保存请求参数
     * @return CollectorQualityTaskDO
     */
     CollectorQualityTaskDO convertToDO(CollectorQualityTaskSaveReqVO CollectorQualityTaskSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorQualityTaskDO 实体对象
     * @return CollectorQualityTaskRespVO
     */
     CollectorQualityTaskRespVO convertToRespVO(CollectorQualityTaskDO CollectorQualityTaskDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorQualityTaskDOList 实体对象列表
     * @return List<CollectorQualityTaskRespVO>
     */
     List<CollectorQualityTaskRespVO> convertToRespVOList(List<CollectorQualityTaskDO> CollectorQualityTaskDOList);
}
