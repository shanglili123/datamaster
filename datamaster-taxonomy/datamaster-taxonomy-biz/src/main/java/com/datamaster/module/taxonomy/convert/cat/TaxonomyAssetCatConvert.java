

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyAssetCatDO;

import java.util.List;

/**
 * 数据资产类目管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Mapper
public interface TaxonomyAssetCatConvert {
    TaxonomyAssetCatConvert INSTANCE = Mappers.getMapper(TaxonomyAssetCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyAssetCatPageReqVO 请求参数
     * @return TaxonomyAssetCatDO
     */
     TaxonomyAssetCatDO convertToDO(TaxonomyAssetCatPageReqVO TaxonomyAssetCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyAssetCatSaveReqVO 保存请求参数
     * @return TaxonomyAssetCatDO
     */
     TaxonomyAssetCatDO convertToDO(TaxonomyAssetCatSaveReqVO TaxonomyAssetCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyAssetCatDO 实体对象
     * @return TaxonomyAssetCatRespVO
     */
     TaxonomyAssetCatRespVO convertToRespVO(TaxonomyAssetCatDO TaxonomyAssetCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyAssetCatDOList 实体对象列表
     * @return List<TaxonomyAssetCatRespVO>
     */
     List<TaxonomyAssetCatRespVO> convertToRespVOList(List<TaxonomyAssetCatDO> TaxonomyAssetCatDOList);
}
