package com.datamaster.module.catalog.convert.columnLog;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogRespVO;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.columnLog.CatalogColumnLogDO;

import java.util.List;

/**
 * 元数据字段信息 - 日志 Convert
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
@Mapper
public interface CatalogColumnLogConvert {
    CatalogColumnLogConvert INSTANCE = Mappers.getMapper(CatalogColumnLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogColumnLogPageReqVO 请求参数
     * @return CatalogColumnLogDO
     */
     CatalogColumnLogDO convertToDO(CatalogColumnLogPageReqVO CatalogColumnLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogColumnLogSaveReqVO 保存请求参数
     * @return CatalogColumnLogDO
     */
     CatalogColumnLogDO convertToDO(CatalogColumnLogSaveReqVO CatalogColumnLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogColumnLogDO 实体对象
     * @return CatalogColumnLogRespVO
     */
     CatalogColumnLogRespVO convertToRespVO(CatalogColumnLogDO CatalogColumnLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogColumnLogDOList 实体对象列表
     * @return List<CatalogColumnLogRespVO>
     */
     List<CatalogColumnLogRespVO> convertToRespVOList(List<CatalogColumnLogDO> CatalogColumnLogDOList);
}
