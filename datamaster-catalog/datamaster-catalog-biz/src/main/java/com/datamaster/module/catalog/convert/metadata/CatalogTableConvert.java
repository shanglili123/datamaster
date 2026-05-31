package com.datamaster.module.catalog.convert.metadata;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTablePageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogTableDO;

import java.util.List;

/**
 * 元数据信息 Convert
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Mapper
public interface CatalogTableConvert {
    CatalogTableConvert INSTANCE = Mappers.getMapper(CatalogTableConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogTablePageReqVO 请求参数
     * @return CatalogTableDO
     */
     CatalogTableDO convertToDO(CatalogTablePageReqVO CatalogTablePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogTableSaveReqVO 保存请求参数
     * @return CatalogTableDO
     */
     CatalogTableDO convertToDO(CatalogTableSaveReqVO CatalogTableSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogTableDO 实体对象
     * @return CatalogTableRespVO
     */
     CatalogTableRespVO convertToRespVO(CatalogTableDO CatalogTableDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogTableDOList 实体对象列表
     * @return List<CatalogTableRespVO>
     */
     List<CatalogTableRespVO> convertToRespVOList(List<CatalogTableDO> CatalogTableDOList);
}
