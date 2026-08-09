

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeDO;

import java.util.List;

/**
 * 数据集成节点 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlNodeConvert {
    CollectorEtlNodeConvert INSTANCE = Mappers.getMapper(CollectorEtlNodeConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlNodePageReqVO 请求参数
     * @return CollectorEtlNodeDO
     */
     CollectorEtlNodeDO convertToDO(CollectorEtlNodePageReqVO CollectorEtlNodePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlNodeSaveReqVO 保存请求参数
     * @return CollectorEtlNodeDO
     */
     CollectorEtlNodeDO convertToDO(CollectorEtlNodeSaveReqVO CollectorEtlNodeSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlNodeDO 实体对象
     * @return CollectorEtlNodeRespVO
     */
     CollectorEtlNodeRespVO convertToRespVO(CollectorEtlNodeDO CollectorEtlNodeDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlNodeDOList 实体对象列表
     * @return List<CollectorEtlNodeRespVO>
     */
     List<CollectorEtlNodeRespVO> convertToRespVOList(List<CollectorEtlNodeDO> CollectorEtlNodeDOList);
}
