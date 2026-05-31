

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSchedulerDO;

import java.util.List;

/**
 * 数据集成调度信息 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlSchedulerConvert {
    CollectorEtlSchedulerConvert INSTANCE = Mappers.getMapper(CollectorEtlSchedulerConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlSchedulerPageReqVO 请求参数
     * @return CollectorEtlSchedulerDO
     */
     CollectorEtlSchedulerDO convertToDO(CollectorEtlSchedulerPageReqVO CollectorEtlSchedulerPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlSchedulerSaveReqVO 保存请求参数
     * @return CollectorEtlSchedulerDO
     */
     CollectorEtlSchedulerDO convertToDO(CollectorEtlSchedulerSaveReqVO CollectorEtlSchedulerSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlSchedulerDO 实体对象
     * @return CollectorEtlSchedulerRespVO
     */
     CollectorEtlSchedulerRespVO convertToRespVO(CollectorEtlSchedulerDO CollectorEtlSchedulerDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlSchedulerDOList 实体对象列表
     * @return List<CollectorEtlSchedulerRespVO>
     */
     List<CollectorEtlSchedulerRespVO> convertToRespVOList(List<CollectorEtlSchedulerDO> CollectorEtlSchedulerDOList);
}
