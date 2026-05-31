

package com.datamaster.module.taxonomy.convert.cat;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTaskCatDO;

import java.util.List;

/**
 * 数据集成任务类目管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Mapper
public interface TaxonomyTaskCatConvert {
    TaxonomyTaskCatConvert INSTANCE = Mappers.getMapper(TaxonomyTaskCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyTaskCatPageReqVO 请求参数
     * @return TaxonomyTaskCatDO
     */
     TaxonomyTaskCatDO convertToDO(TaxonomyTaskCatPageReqVO TaxonomyTaskCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyTaskCatSaveReqVO 保存请求参数
     * @return TaxonomyTaskCatDO
     */
     TaxonomyTaskCatDO convertToDO(TaxonomyTaskCatSaveReqVO TaxonomyTaskCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyTaskCatDO 实体对象
     * @return TaxonomyTaskCatRespVO
     */
     TaxonomyTaskCatRespVO convertToRespVO(TaxonomyTaskCatDO TaxonomyTaskCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyTaskCatDOList 实体对象列表
     * @return List<TaxonomyTaskCatRespVO>
     */
     List<TaxonomyTaskCatRespVO> convertToRespVOList(List<TaxonomyTaskCatDO> TaxonomyTaskCatDOList);
}
