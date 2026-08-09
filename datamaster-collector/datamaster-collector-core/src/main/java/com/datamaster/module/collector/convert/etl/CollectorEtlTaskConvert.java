

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;

import java.util.List;

/**
 * 数据集成任务 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlTaskConvert {
    CollectorEtlTaskConvert INSTANCE = Mappers.getMapper(CollectorEtlTaskConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlTaskPageReqVO 请求参数
     * @return CollectorEtlTaskDO
     */
     CollectorEtlTaskDO convertToDO(CollectorEtlTaskPageReqVO CollectorEtlTaskPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlTaskSaveReqVO 保存请求参数
     * @return CollectorEtlTaskDO
     */
     CollectorEtlTaskDO convertToDO(CollectorEtlTaskSaveReqVO CollectorEtlTaskSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlTaskDO 实体对象
     * @return CollectorEtlTaskRespVO
     */
     CollectorEtlTaskRespVO convertToRespVO(CollectorEtlTaskDO CollectorEtlTaskDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlTaskDOList 实体对象列表
     * @return List<CollectorEtlTaskRespVO>
     */
     List<CollectorEtlTaskRespVO> convertToRespVOList(List<CollectorEtlTaskDO> CollectorEtlTaskDOList);
}
