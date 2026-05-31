

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDataDevCatDO;

import java.util.List;

/**
 * 数据开发类目管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Mapper
public interface TaxonomyDataDevCatConvert {
    TaxonomyDataDevCatConvert INSTANCE = Mappers.getMapper(TaxonomyDataDevCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyDataDevCatPageReqVO 请求参数
     * @return TaxonomyDataDevCatDO
     */
     TaxonomyDataDevCatDO convertToDO(TaxonomyDataDevCatPageReqVO TaxonomyDataDevCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyDataDevCatSaveReqVO 保存请求参数
     * @return TaxonomyDataDevCatDO
     */
     TaxonomyDataDevCatDO convertToDO(TaxonomyDataDevCatSaveReqVO TaxonomyDataDevCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyDataDevCatDO 实体对象
     * @return TaxonomyDataDevCatRespVO
     */
     TaxonomyDataDevCatRespVO convertToRespVO(TaxonomyDataDevCatDO TaxonomyDataDevCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyDataDevCatDOList 实体对象列表
     * @return List<TaxonomyDataDevCatRespVO>
     */
     List<TaxonomyDataDevCatRespVO> convertToRespVOList(List<TaxonomyDataDevCatDO> TaxonomyDataDevCatDOList);
}
