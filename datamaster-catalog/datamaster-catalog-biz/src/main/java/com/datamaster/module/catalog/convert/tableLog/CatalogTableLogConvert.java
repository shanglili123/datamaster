package com.datamaster.module.catalog.convert.tableLog;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogRespVO;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.tableLog.CatalogTableLogDO;

import java.util.List;

/**
 * 元数据信息 - 日志 Convert
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
@Mapper
public interface CatalogTableLogConvert {
    CatalogTableLogConvert INSTANCE = Mappers.getMapper(CatalogTableLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogTableLogPageReqVO 请求参数
     * @return CatalogTableLogDO
     */
     CatalogTableLogDO convertToDO(CatalogTableLogPageReqVO CatalogTableLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogTableLogSaveReqVO 保存请求参数
     * @return CatalogTableLogDO
     */
     CatalogTableLogDO convertToDO(CatalogTableLogSaveReqVO CatalogTableLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogTableLogDO 实体对象
     * @return CatalogTableLogRespVO
     */
     CatalogTableLogRespVO convertToRespVO(CatalogTableLogDO CatalogTableLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogTableLogDOList 实体对象列表
     * @return List<CatalogTableLogRespVO>
     */
     List<CatalogTableLogRespVO> convertToRespVOList(List<CatalogTableLogDO> CatalogTableLogDOList);
}
