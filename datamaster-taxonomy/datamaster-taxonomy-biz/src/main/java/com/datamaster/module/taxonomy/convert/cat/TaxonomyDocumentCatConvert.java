

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDocumentCatDO;

import java.util.List;

/**
 * 标准信息分类管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
@Mapper
public interface TaxonomyDocumentCatConvert {
    TaxonomyDocumentCatConvert INSTANCE = Mappers.getMapper(TaxonomyDocumentCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyDocumentCatPageReqVO 请求参数
     * @return TaxonomyDocumentCatDO
     */
     TaxonomyDocumentCatDO convertToDO(TaxonomyDocumentCatPageReqVO TaxonomyDocumentCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyDocumentCatSaveReqVO 保存请求参数
     * @return TaxonomyDocumentCatDO
     */
     TaxonomyDocumentCatDO convertToDO(TaxonomyDocumentCatSaveReqVO TaxonomyDocumentCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyDocumentCatDO 实体对象
     * @return TaxonomyDocumentCatRespVO
     */
     TaxonomyDocumentCatRespVO convertToRespVO(TaxonomyDocumentCatDO TaxonomyDocumentCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyDocumentCatDOList 实体对象列表
     * @return List<TaxonomyDocumentCatRespVO>
     */
     List<TaxonomyDocumentCatRespVO> convertToRespVOList(List<TaxonomyDocumentCatDO> TaxonomyDocumentCatDOList);
}
