package com.datamaster.module.catalog.convert.metadata;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnPageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;

import java.util.List;

/**
 * 元数据字段信息 Convert
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Mapper
public interface CatalogColumnConvert {
    CatalogColumnConvert INSTANCE = Mappers.getMapper(CatalogColumnConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogColumnPageReqVO 请求参数
     * @return CatalogColumnDTO
     */
     CatalogColumnDO convertToDO(CatalogColumnPageReqVO CatalogColumnPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogColumnSaveReqVO 保存请求参数
     * @return CatalogColumnDTO
     */
     CatalogColumnDO convertToDO(CatalogColumnSaveReqVO CatalogColumnSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogColumnDO 实体对象
     * @return CatalogColumnRespDTO
     */
     CatalogColumnRespVO convertToRespVO(CatalogColumnDO CatalogColumnDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogColumnDOList 实体对象列表
     * @return List<CatalogColumnRespDTO>
     */
     List<CatalogColumnRespVO> convertToRespVOList(List<CatalogColumnDO> CatalogColumnDOList);
}
