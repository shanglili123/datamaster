

package com.datamaster.module.taxonomy.convert.Tag;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagRespVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.Tag.TaxonomyTagDO;

import java.util.List;

/**
 * 标签管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Mapper
public interface TaxonomyTagConvert {
    TaxonomyTagConvert INSTANCE = Mappers.getMapper(TaxonomyTagConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyTagPageReqVO 请求参数
     * @return TaxonomyTagDO
     */
     TaxonomyTagDO convertToDO(TaxonomyTagPageReqVO TaxonomyTagPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyTagSaveReqVO 保存请求参数
     * @return TaxonomyTagDO
     */
     TaxonomyTagDO convertToDO(TaxonomyTagSaveReqVO TaxonomyTagSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyTagDO 实体对象
     * @return TaxonomyTagRespVO
     */
     TaxonomyTagRespVO convertToRespVO(TaxonomyTagDO TaxonomyTagDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyTagDOList 实体对象列表
     * @return List<TaxonomyTagRespVO>
     */
     List<TaxonomyTagRespVO> convertToRespVOList(List<TaxonomyTagDO> TaxonomyTagDOList);
}
