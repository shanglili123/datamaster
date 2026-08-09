

package com.datamaster.module.collector.convert.etl;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSqlTempDO;

import java.util.List;

/**
 * 数据集成SQL模版 Convert
 *
 * @author FXB
 * @date 2025-06-25
 */
@Mapper
public interface CollectorEtlSqlTempConvert {
    CollectorEtlSqlTempConvert INSTANCE = Mappers.getMapper(CollectorEtlSqlTempConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorEtlSqlTempPageReqVO 请求参数
     * @return CollectorEtlSqlTempDO
     */
     CollectorEtlSqlTempDO convertToDO(CollectorEtlSqlTempPageReqVO CollectorEtlSqlTempPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorEtlSqlTempSaveReqVO 保存请求参数
     * @return CollectorEtlSqlTempDO
     */
     CollectorEtlSqlTempDO convertToDO(CollectorEtlSqlTempSaveReqVO CollectorEtlSqlTempSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorEtlSqlTempDO 实体对象
     * @return CollectorEtlSqlTempRespVO
     */
     CollectorEtlSqlTempRespVO convertToRespVO(CollectorEtlSqlTempDO CollectorEtlSqlTempDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorEtlSqlTempDOList 实体对象列表
     * @return List<CollectorEtlSqlTempRespVO>
     */
     List<CollectorEtlSqlTempRespVO> convertToRespVOList(List<CollectorEtlSqlTempDO> CollectorEtlSqlTempDOList);
}
