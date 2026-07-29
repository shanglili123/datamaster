

package com.datamaster.module.taxonomy.convert.space;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpacePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceRespVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.space.TaxonomySpaceDO;

import java.util.List;

/**
 * 空间 Convert
 *
 * @author shu
 * @date 2025-01-20
 */
@Mapper
public interface TaxonomySpaceConvert {
    TaxonomySpaceConvert INSTANCE = Mappers.getMapper(TaxonomySpaceConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param pageReqVO 请求参数
     * @return TaxonomySpaceDO
     */
     TaxonomySpaceDO convertToDO(TaxonomySpacePageReqVO pageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param saveReqVO 保存请求参数
     * @return TaxonomySpaceDO
     */
     TaxonomySpaceDO convertToDO(TaxonomySpaceSaveReqVO saveReqVO);

    /**
     * DO 转换为 RespVO
     * @param spaceDO 实体对象
     * @return TaxonomySpaceRespVO
     */
     TaxonomySpaceRespVO convertToRespVO(TaxonomySpaceDO spaceDO);

    /**
     * DOList 转换为 RespVOList
     * @param spaceDOList 实体对象列表
     * @return List<TaxonomySpaceRespVO>
     */
     List<TaxonomySpaceRespVO> convertToRespVOList(List<TaxonomySpaceDO> spaceDOList);
}
