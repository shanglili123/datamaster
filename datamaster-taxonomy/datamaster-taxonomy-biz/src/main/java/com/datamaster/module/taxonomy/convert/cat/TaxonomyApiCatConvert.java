

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyApiCatDO;

import java.util.List;

/**
 * 数据服务类目管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Mapper
public interface TaxonomyApiCatConvert {
    TaxonomyApiCatConvert INSTANCE = Mappers.getMapper(TaxonomyApiCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyApiCatPageReqVO 请求参数
     * @return TaxonomyApiCatDO
     */
     TaxonomyApiCatDO convertToDO(TaxonomyApiCatPageReqVO TaxonomyApiCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyApiCatSaveReqVO 保存请求参数
     * @return TaxonomyApiCatDO
     */
     TaxonomyApiCatDO convertToDO(TaxonomyApiCatSaveReqVO TaxonomyApiCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyApiCatDO 实体对象
     * @return TaxonomyApiCatRespVO
     */
     TaxonomyApiCatRespVO convertToRespVO(TaxonomyApiCatDO TaxonomyApiCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyApiCatDOList 实体对象列表
     * @return List<TaxonomyApiCatRespVO>
     */
     List<TaxonomyApiCatRespVO> convertToRespVOList(List<TaxonomyApiCatDO> TaxonomyApiCatDOList);
}
