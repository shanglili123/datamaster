package com.datamaster.module.catalog.convert.domain;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.domain.vo.CatalogDomainPageReqVO;
import com.datamaster.module.catalog.controller.admin.domain.vo.CatalogDomainRespVO;
import com.datamaster.module.catalog.controller.admin.domain.vo.CatalogDomainSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.domain.CatalogDomainDO;

import java.util.List;

/**
 * 业务域 Convert
 *
 * @author DATAMASTER
 * @date 2026-02-12
 */
@Mapper
public interface CatalogDomainConvert {
    CatalogDomainConvert INSTANCE = Mappers.getMapper(CatalogDomainConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogDomainPageReqVO 请求参数
     * @return CatalogDomainDO
     */
     CatalogDomainDO convertToDO(CatalogDomainPageReqVO CatalogDomainPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogDomainSaveReqVO 保存请求参数
     * @return CatalogDomainDO
     */
     CatalogDomainDO convertToDO(CatalogDomainSaveReqVO CatalogDomainSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogDomainDO 实体对象
     * @return CatalogDomainRespVO
     */
     CatalogDomainRespVO convertToRespVO(CatalogDomainDO CatalogDomainDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogDomainDOList 实体对象列表
     * @return List<CatalogDomainRespVO>
     */
     List<CatalogDomainRespVO> convertToRespVOList(List<CatalogDomainDO> CatalogDomainDOList);
}
