

package com.datamaster.module.taxonomy.convert.Rel;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.Rel.TaxonomyTagAssetRelDO;

import java.util.List;

/**
 * 标签与资产关联关系 Convert
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Mapper
public interface TaxonomyTagAssetRelConvert {
    TaxonomyTagAssetRelConvert INSTANCE = Mappers.getMapper(TaxonomyTagAssetRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyTagAssetRelPageReqVO 请求参数
     * @return TaxonomyTagAssetRelDO
     */
     TaxonomyTagAssetRelDO convertToDO(TaxonomyTagAssetRelPageReqVO TaxonomyTagAssetRelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyTagAssetRelSaveReqVO 保存请求参数
     * @return TaxonomyTagAssetRelDO
     */
     TaxonomyTagAssetRelDO convertToDO(TaxonomyTagAssetRelSaveReqVO TaxonomyTagAssetRelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyTagAssetRelDO 实体对象
     * @return TaxonomyTagAssetRelRespVO
     */
     TaxonomyTagAssetRelRespVO convertToRespVO(TaxonomyTagAssetRelDO TaxonomyTagAssetRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyTagAssetRelDOList 实体对象列表
     * @return List<TaxonomyTagAssetRelRespVO>
     */
     List<TaxonomyTagAssetRelRespVO> convertToRespVOList(List<TaxonomyTagAssetRelDO> TaxonomyTagAssetRelDOList);
}
