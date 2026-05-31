

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelLogDO;

import java.util.List;

/**
 * 数据集成任务节点关系-日志 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlTaskNodeRelLogConvert {
    CollectorEtlTaskNodeRelLogConvert INSTANCE = Mappers.getMapper(CollectorEtlTaskNodeRelLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlTaskNodeRelLogPageReqVO 请求参数
     * @return CollectorEtlTaskNodeRelLogDO
     */
     CollectorEtlTaskNodeRelLogDO convertToDO(CollectorEtlTaskNodeRelLogPageReqVO CollectorEtlTaskNodeRelLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlTaskNodeRelLogSaveReqVO 保存请求参数
     * @return CollectorEtlTaskNodeRelLogDO
     */
     CollectorEtlTaskNodeRelLogDO convertToDO(CollectorEtlTaskNodeRelLogSaveReqVO CollectorEtlTaskNodeRelLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlTaskNodeRelLogDO 实体对象
     * @return CollectorEtlTaskNodeRelLogRespVO
     */
     CollectorEtlTaskNodeRelLogRespVO convertToRespVO(CollectorEtlTaskNodeRelLogDO CollectorEtlTaskNodeRelLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlTaskNodeRelLogDOList 实体对象列表
     * @return List<CollectorEtlTaskNodeRelLogRespVO>
     */
     List<CollectorEtlTaskNodeRelLogRespVO> convertToRespVOList(List<CollectorEtlTaskNodeRelLogDO> CollectorEtlTaskNodeRelLogDOList);
}
