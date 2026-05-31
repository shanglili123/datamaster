

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyModelCatDO;

import java.util.List;

/**
 * 逻辑模型类目管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Mapper
public interface TaxonomyModelCatConvert {
    TaxonomyModelCatConvert INSTANCE = Mappers.getMapper(TaxonomyModelCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyModelCatPageReqVO 请求参数
     * @return TaxonomyModelCatDO
     */
     TaxonomyModelCatDO convertToDO(TaxonomyModelCatPageReqVO TaxonomyModelCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyModelCatSaveReqVO 保存请求参数
     * @return TaxonomyModelCatDO
     */
     TaxonomyModelCatDO convertToDO(TaxonomyModelCatSaveReqVO TaxonomyModelCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyModelCatDO 实体对象
     * @return TaxonomyModelCatRespVO
     */
     TaxonomyModelCatRespVO convertToRespVO(TaxonomyModelCatDO TaxonomyModelCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyModelCatDOList 实体对象列表
     * @return List<TaxonomyModelCatRespVO>
     */
     List<TaxonomyModelCatRespVO> convertToRespVOList(List<TaxonomyModelCatDO> TaxonomyModelCatDOList);
}
