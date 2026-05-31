package com.datamaster.module.catalog.convert.task;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopePageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopeRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopeSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskScopeDO;

import java.util.List;

/**
 * 采集范围 Convert
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Mapper
public interface CatalogTaskScopeConvert {
    CatalogTaskScopeConvert INSTANCE = Mappers.getMapper(CatalogTaskScopeConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogTaskScopePageReqVO 请求参数
     * @return CatalogTaskScopeDO
     */
     CatalogTaskScopeDO convertToDO(CatalogTaskScopePageReqVO CatalogTaskScopePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogTaskScopeSaveReqVO 保存请求参数
     * @return CatalogTaskScopeDO
     */
     CatalogTaskScopeDO convertToDO(CatalogTaskScopeSaveReqVO CatalogTaskScopeSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogTaskScopeDO 实体对象
     * @return CatalogTaskScopeRespVO
     */
     CatalogTaskScopeRespVO convertToRespVO(CatalogTaskScopeDO CatalogTaskScopeDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogTaskScopeDOList 实体对象列表
     * @return List<CatalogTaskScopeRespVO>
     */
     List<CatalogTaskScopeRespVO> convertToRespVOList(List<CatalogTaskScopeDO> CatalogTaskScopeDOList);
}
