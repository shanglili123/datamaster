

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTagCatDO;

import java.util.List;

/**
 * 标签类目管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Mapper
public interface TaxonomyTagCatConvert {
    TaxonomyTagCatConvert INSTANCE = Mappers.getMapper(TaxonomyTagCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyTagCatPageReqVO 请求参数
     * @return TaxonomyTagCatDO
     */
     TaxonomyTagCatDO convertToDO(TaxonomyTagCatPageReqVO TaxonomyTagCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyTagCatSaveReqVO 保存请求参数
     * @return TaxonomyTagCatDO
     */
     TaxonomyTagCatDO convertToDO(TaxonomyTagCatSaveReqVO TaxonomyTagCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyTagCatDO 实体对象
     * @return TaxonomyTagCatRespVO
     */
     TaxonomyTagCatRespVO convertToRespVO(TaxonomyTagCatDO TaxonomyTagCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyTagCatDOList 实体对象列表
     * @return List<TaxonomyTagCatRespVO>
     */
     List<TaxonomyTagCatRespVO> convertToRespVOList(List<TaxonomyTagCatDO> TaxonomyTagCatDOList);
}
