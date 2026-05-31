

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskLogDO;

import java.util.List;

/**
 * 数据集成任务-日志 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlTaskLogConvert {
    CollectorEtlTaskLogConvert INSTANCE = Mappers.getMapper(CollectorEtlTaskLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlTaskLogPageReqVO 请求参数
     * @return CollectorEtlTaskLogDO
     */
     CollectorEtlTaskLogDO convertToDO(CollectorEtlTaskLogPageReqVO CollectorEtlTaskLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlTaskLogSaveReqVO 保存请求参数
     * @return CollectorEtlTaskLogDO
     */
     CollectorEtlTaskLogDO convertToDO(CollectorEtlTaskLogSaveReqVO CollectorEtlTaskLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlTaskLogDO 实体对象
     * @return CollectorEtlTaskLogRespVO
     */
     CollectorEtlTaskLogRespVO convertToRespVO(CollectorEtlTaskLogDO CollectorEtlTaskLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlTaskLogDOList 实体对象列表
     * @return List<CollectorEtlTaskLogRespVO>
     */
     List<CollectorEtlTaskLogRespVO> convertToRespVOList(List<CollectorEtlTaskLogDO> CollectorEtlTaskLogDOList);
}
