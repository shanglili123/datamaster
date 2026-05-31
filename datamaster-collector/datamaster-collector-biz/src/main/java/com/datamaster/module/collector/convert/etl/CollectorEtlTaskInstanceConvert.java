

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskInstancePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskInstanceRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskInstanceSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;

import java.util.List;

/**
 * 数据集成任务实例 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Mapper
public interface CollectorEtlTaskInstanceConvert {
    CollectorEtlTaskInstanceConvert INSTANCE = Mappers.getMapper(CollectorEtlTaskInstanceConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlTaskInstancePageReqVO 请求参数
     * @return CollectorEtlTaskInstanceDO
     */
     CollectorEtlTaskInstanceDO convertToDO(CollectorEtlTaskInstancePageReqVO CollectorEtlTaskInstancePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlTaskInstanceSaveReqVO 保存请求参数
     * @return CollectorEtlTaskInstanceDO
     */
     CollectorEtlTaskInstanceDO convertToDO(CollectorEtlTaskInstanceSaveReqVO CollectorEtlTaskInstanceSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlTaskInstanceDO 实体对象
     * @return CollectorEtlTaskInstanceRespVO
     */
     CollectorEtlTaskInstanceRespVO convertToRespVO(CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlTaskInstanceDOList 实体对象列表
     * @return List<CollectorEtlTaskInstanceRespVO>
     */
     List<CollectorEtlTaskInstanceRespVO> convertToRespVOList(List<CollectorEtlTaskInstanceDO> CollectorEtlTaskInstanceDOList);
}
