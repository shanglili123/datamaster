

package com.datamaster.module.taxonomy.convert.project;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectRespVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectDO;

import java.util.List;

/**
 * 项目 Convert
 *
 * @author shu
 * @date 2025-01-20
 */
@Mapper
public interface TaxonomyProjectConvert {
    TaxonomyProjectConvert INSTANCE = Mappers.getMapper(TaxonomyProjectConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyProjectPageReqVO 请求参数
     * @return TaxonomyProjectDO
     */
     TaxonomyProjectDO convertToDO(TaxonomyProjectPageReqVO TaxonomyProjectPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyProjectSaveReqVO 保存请求参数
     * @return TaxonomyProjectDO
     */
     TaxonomyProjectDO convertToDO(TaxonomyProjectSaveReqVO TaxonomyProjectSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyProjectDO 实体对象
     * @return TaxonomyProjectRespVO
     */
     TaxonomyProjectRespVO convertToRespVO(TaxonomyProjectDO TaxonomyProjectDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyProjectDOList 实体对象列表
     * @return List<TaxonomyProjectRespVO>
     */
     List<TaxonomyProjectRespVO> convertToRespVOList(List<TaxonomyProjectDO> TaxonomyProjectDOList);
}
