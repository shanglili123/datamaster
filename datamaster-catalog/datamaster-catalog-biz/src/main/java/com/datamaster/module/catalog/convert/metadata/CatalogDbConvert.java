package com.datamaster.module.catalog.convert.metadata;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbPageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;

import java.util.List;

/**
 * 数据库 Convert
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Mapper
public interface CatalogDbConvert {
    CatalogDbConvert INSTANCE = Mappers.getMapper(CatalogDbConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogDbPageReqVO 请求参数
     * @return CatalogDbDO
     */
     CatalogDbDO convertToDO(CatalogDbPageReqVO CatalogDbPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogDbSaveReqVO 保存请求参数
     * @return CatalogDbDO
     */
     CatalogDbDO convertToDO(CatalogDbSaveReqVO CatalogDbSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogDbDO 实体对象
     * @return CatalogDbRespVO
     */
     CatalogDbRespVO convertToRespVO(CatalogDbDO CatalogDbDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogDbDOList 实体对象列表
     * @return List<CatalogDbRespVO>
     */
     List<CatalogDbRespVO> convertToRespVOList(List<CatalogDbDO> CatalogDbDOList);
}
