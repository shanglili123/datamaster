

package com.datamaster.module.taxonomy.convert.project;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectUserRelDO;

import java.util.List;

/**
 * 项目与用户关联关系 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Mapper
public interface TaxonomyProjectUserRelConvert {
    TaxonomyProjectUserRelConvert INSTANCE = Mappers.getMapper(TaxonomyProjectUserRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyProjectUserRelPageReqVO 请求参数
     * @return TaxonomyProjectUserRelDO
     */
     TaxonomyProjectUserRelDO convertToDO(TaxonomyProjectUserRelPageReqVO TaxonomyProjectUserRelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyProjectUserRelSaveReqVO 保存请求参数
     * @return TaxonomyProjectUserRelDO
     */
     TaxonomyProjectUserRelDO convertToDO(TaxonomyProjectUserRelSaveReqVO TaxonomyProjectUserRelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyProjectUserRelDO 实体对象
     * @return TaxonomyProjectUserRelRespVO
     */
     TaxonomyProjectUserRelRespVO convertToRespVO(TaxonomyProjectUserRelDO TaxonomyProjectUserRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyProjectUserRelDOList 实体对象列表
     * @return List<TaxonomyProjectUserRelRespVO>
     */
     List<TaxonomyProjectUserRelRespVO> convertToRespVOList(List<TaxonomyProjectUserRelDO> TaxonomyProjectUserRelDOList);
}
