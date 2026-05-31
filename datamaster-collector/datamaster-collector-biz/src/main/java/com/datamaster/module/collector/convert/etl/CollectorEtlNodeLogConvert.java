

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeLogDO;

import java.util.List;

/**
 * 数据集成节点-日志 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlNodeLogConvert {
    CollectorEtlNodeLogConvert INSTANCE = Mappers.getMapper(CollectorEtlNodeLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlNodeLogPageReqVO 请求参数
     * @return CollectorEtlNodeLogDO
     */
     CollectorEtlNodeLogDO convertToDO(CollectorEtlNodeLogPageReqVO CollectorEtlNodeLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlNodeLogSaveReqVO 保存请求参数
     * @return CollectorEtlNodeLogDO
     */
     CollectorEtlNodeLogDO convertToDO(CollectorEtlNodeLogSaveReqVO CollectorEtlNodeLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlNodeLogDO 实体对象
     * @return CollectorEtlNodeLogRespVO
     */
     CollectorEtlNodeLogRespVO convertToRespVO(CollectorEtlNodeLogDO CollectorEtlNodeLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlNodeLogDOList 实体对象列表
     * @return List<CollectorEtlNodeLogRespVO>
     */
     List<CollectorEtlNodeLogRespVO> convertToRespVOList(List<CollectorEtlNodeLogDO> CollectorEtlNodeLogDOList);
}
