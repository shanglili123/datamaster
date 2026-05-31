

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDataElemCatDO;

import java.util.List;

/**
 * 数据元类目管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Mapper
public interface TaxonomyDataElemCatConvert {
    TaxonomyDataElemCatConvert INSTANCE = Mappers.getMapper(TaxonomyDataElemCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyDataElemCatPageReqVO 请求参数
     * @return TaxonomyDataElemCatDO
     */
     TaxonomyDataElemCatDO convertToDO(TaxonomyDataElemCatPageReqVO TaxonomyDataElemCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyDataElemCatSaveReqVO 保存请求参数
     * @return TaxonomyDataElemCatDO
     */
     TaxonomyDataElemCatDO convertToDO(TaxonomyDataElemCatSaveReqVO TaxonomyDataElemCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyDataElemCatDO 实体对象
     * @return TaxonomyDataElemCatRespVO
     */
     TaxonomyDataElemCatRespVO convertToRespVO(TaxonomyDataElemCatDO TaxonomyDataElemCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyDataElemCatDOList 实体对象列表
     * @return List<TaxonomyDataElemCatRespVO>
     */
     List<TaxonomyDataElemCatRespVO> convertToRespVOList(List<TaxonomyDataElemCatDO> TaxonomyDataElemCatDOList);
}
