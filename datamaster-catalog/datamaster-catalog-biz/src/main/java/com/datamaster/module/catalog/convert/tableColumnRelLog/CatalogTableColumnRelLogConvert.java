package com.datamaster.module.catalog.convert.tableColumnRelLog;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogRespVO;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.tableColumnRelLog.CatalogTableColumnRelLogDO;

import java.util.List;

/**
 * 元数据数据库与信息及字段信息关系-日志 Convert
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
@Mapper
public interface CatalogTableColumnRelLogConvert {
    CatalogTableColumnRelLogConvert INSTANCE = Mappers.getMapper(CatalogTableColumnRelLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogTableColumnRelLogPageReqVO 请求参数
     * @return CatalogTableColumnRelLogDO
     */
     CatalogTableColumnRelLogDO convertToDO(CatalogTableColumnRelLogPageReqVO CatalogTableColumnRelLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogTableColumnRelLogSaveReqVO 保存请求参数
     * @return CatalogTableColumnRelLogDO
     */
     CatalogTableColumnRelLogDO convertToDO(CatalogTableColumnRelLogSaveReqVO CatalogTableColumnRelLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogTableColumnRelLogDO 实体对象
     * @return CatalogTableColumnRelLogRespVO
     */
     CatalogTableColumnRelLogRespVO convertToRespVO(CatalogTableColumnRelLogDO CatalogTableColumnRelLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogTableColumnRelLogDOList 实体对象列表
     * @return List<CatalogTableColumnRelLogRespVO>
     */
     List<CatalogTableColumnRelLogRespVO> convertToRespVOList(List<CatalogTableColumnRelLogDO> CatalogTableColumnRelLogDOList);
}
