

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelDO;

import java.util.List;

/**
 * 数据集成任务节点关系 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlTaskNodeRelConvert {
    CollectorEtlTaskNodeRelConvert INSTANCE = Mappers.getMapper(CollectorEtlTaskNodeRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlTaskNodeRelPageReqVO 请求参数
     * @return CollectorEtlTaskNodeRelDO
     */
     CollectorEtlTaskNodeRelDO convertToDO(CollectorEtlTaskNodeRelPageReqVO CollectorEtlTaskNodeRelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlTaskNodeRelSaveReqVO 保存请求参数
     * @return CollectorEtlTaskNodeRelDO
     */
     CollectorEtlTaskNodeRelDO convertToDO(CollectorEtlTaskNodeRelSaveReqVO CollectorEtlTaskNodeRelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlTaskNodeRelDO 实体对象
     * @return CollectorEtlTaskNodeRelRespVO
     */
     CollectorEtlTaskNodeRelRespVO convertToRespVO(CollectorEtlTaskNodeRelDO CollectorEtlTaskNodeRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlTaskNodeRelDOList 实体对象列表
     * @return List<CollectorEtlTaskNodeRelRespVO>
     */
     List<CollectorEtlTaskNodeRelRespVO> convertToRespVOList(List<CollectorEtlTaskNodeRelDO> CollectorEtlTaskNodeRelDOList);
}
