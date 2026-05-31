

package com.datamaster.module.taxonomy.convert.theme;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemeRespVO;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemeSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.theme.TaxonomyThemeDO;

import java.util.List;

/**
 * 主题 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Mapper
public interface TaxonomyThemeConvert {
    TaxonomyThemeConvert INSTANCE = Mappers.getMapper(TaxonomyThemeConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyThemePageReqVO 请求参数
     * @return TaxonomyThemeDO
     */
     TaxonomyThemeDO convertToDO(TaxonomyThemePageReqVO TaxonomyThemePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyThemeSaveReqVO 保存请求参数
     * @return TaxonomyThemeDO
     */
     TaxonomyThemeDO convertToDO(TaxonomyThemeSaveReqVO TaxonomyThemeSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyThemeDO 实体对象
     * @return TaxonomyThemeRespVO
     */
     TaxonomyThemeRespVO convertToRespVO(TaxonomyThemeDO TaxonomyThemeDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyThemeDOList 实体对象列表
     * @return List<TaxonomyThemeRespVO>
     */
     List<TaxonomyThemeRespVO> convertToRespVOList(List<TaxonomyThemeDO> TaxonomyThemeDOList);
}
