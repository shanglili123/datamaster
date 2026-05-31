

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyQualityCatDO;

import java.util.List;

/**
 * 数据质量类目 Convert
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Mapper
public interface TaxonomyQualityCatConvert {
    TaxonomyQualityCatConvert INSTANCE = Mappers.getMapper(TaxonomyQualityCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyQualityCatPageReqVO 请求参数
     * @return TaxonomyQualityCatDO
     */
     TaxonomyQualityCatDO convertToDO(TaxonomyQualityCatPageReqVO TaxonomyQualityCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyQualityCatSaveReqVO 保存请求参数
     * @return TaxonomyQualityCatDO
     */
     TaxonomyQualityCatDO convertToDO(TaxonomyQualityCatSaveReqVO TaxonomyQualityCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyQualityCatDO 实体对象
     * @return TaxonomyQualityCatRespVO
     */
     TaxonomyQualityCatRespVO convertToRespVO(TaxonomyQualityCatDO TaxonomyQualityCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyQualityCatDOList 实体对象列表
     * @return List<TaxonomyQualityCatRespVO>
     */
     List<TaxonomyQualityCatRespVO> convertToRespVOList(List<TaxonomyQualityCatDO> TaxonomyQualityCatDOList);
}
