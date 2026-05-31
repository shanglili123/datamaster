

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstancePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceDO;

import java.util.List;

/**
 * 数据集成节点实例 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlNodeInstanceConvert {
    CollectorEtlNodeInstanceConvert INSTANCE = Mappers.getMapper(CollectorEtlNodeInstanceConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlNodeInstancePageReqVO 请求参数
     * @return CollectorEtlNodeInstanceDO
     */
     CollectorEtlNodeInstanceDO convertToDO(CollectorEtlNodeInstancePageReqVO CollectorEtlNodeInstancePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlNodeInstanceSaveReqVO 保存请求参数
     * @return CollectorEtlNodeInstanceDO
     */
     CollectorEtlNodeInstanceDO convertToDO(CollectorEtlNodeInstanceSaveReqVO CollectorEtlNodeInstanceSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlNodeInstanceDO 实体对象
     * @return CollectorEtlNodeInstanceRespVO
     */
     CollectorEtlNodeInstanceRespVO convertToRespVO(CollectorEtlNodeInstanceDO CollectorEtlNodeInstanceDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlNodeInstanceDOList 实体对象列表
     * @return List<CollectorEtlNodeInstanceRespVO>
     */
     List<CollectorEtlNodeInstanceRespVO> convertToRespVOList(List<CollectorEtlNodeInstanceDO> CollectorEtlNodeInstanceDOList);
}
