

package com.datamaster.module.taxonomy.convert.sourceSystem;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemRespVO;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.sourceSystem.TaxonomySourceSystemDO;

import java.util.List;

/**
 * 来源系统 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Mapper
public interface TaxonomySourceSystemConvert {
    TaxonomySourceSystemConvert INSTANCE = Mappers.getMapper(TaxonomySourceSystemConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomySourceSystemPageReqVO 请求参数
     * @return TaxonomySourceSystemDO
     */
     TaxonomySourceSystemDO convertToDO(TaxonomySourceSystemPageReqVO TaxonomySourceSystemPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomySourceSystemSaveReqVO 保存请求参数
     * @return TaxonomySourceSystemDO
     */
     TaxonomySourceSystemDO convertToDO(TaxonomySourceSystemSaveReqVO TaxonomySourceSystemSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomySourceSystemDO 实体对象
     * @return TaxonomySourceSystemRespVO
     */
     TaxonomySourceSystemRespVO convertToRespVO(TaxonomySourceSystemDO TaxonomySourceSystemDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomySourceSystemDOList 实体对象列表
     * @return List<TaxonomySourceSystemRespVO>
     */
     List<TaxonomySourceSystemRespVO> convertToRespVOList(List<TaxonomySourceSystemDO> TaxonomySourceSystemDOList);
}
