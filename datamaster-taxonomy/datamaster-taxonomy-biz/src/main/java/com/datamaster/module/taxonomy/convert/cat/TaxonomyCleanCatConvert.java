

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyCleanCatDO;

import java.util.List;

/**
 * 清洗规则类目 Convert
 *
 * @author DATAMASTER
 * @date 2025-08-11
 */
@Mapper
public interface TaxonomyCleanCatConvert {
    TaxonomyCleanCatConvert INSTANCE = Mappers.getMapper(TaxonomyCleanCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyCleanCatPageReqVO 请求参数
     * @return TaxonomyCleanCatDO
     */
     TaxonomyCleanCatDO convertToDO(TaxonomyCleanCatPageReqVO TaxonomyCleanCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyCleanCatSaveReqVO 保存请求参数
     * @return TaxonomyCleanCatDO
     */
     TaxonomyCleanCatDO convertToDO(TaxonomyCleanCatSaveReqVO TaxonomyCleanCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyCleanCatDO 实体对象
     * @return TaxonomyCleanCatRespVO
     */
     TaxonomyCleanCatRespVO convertToRespVO(TaxonomyCleanCatDO TaxonomyCleanCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyCleanCatDOList 实体对象列表
     * @return List<TaxonomyCleanCatRespVO>
     */
     List<TaxonomyCleanCatRespVO> convertToRespVOList(List<TaxonomyCleanCatDO> TaxonomyCleanCatDOList);
}
