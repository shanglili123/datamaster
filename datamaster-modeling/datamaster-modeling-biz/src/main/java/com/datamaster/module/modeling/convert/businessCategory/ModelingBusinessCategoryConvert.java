

package com.datamaster.module.modeling.convert.businessCategory;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryPageReqVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryRespVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategorySaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessCategoryDO;

import java.util.List;

/**
 * 业务分类 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
@Mapper
public interface ModelingBusinessCategoryConvert {
    ModelingBusinessCategoryConvert INSTANCE = Mappers.getMapper(ModelingBusinessCategoryConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ModelingBusinessCategoryPageReqVO 请求参数
     * @return ModelingBusinessCategoryDO
     */
     ModelingBusinessCategoryDO convertToDO(ModelingBusinessCategoryPageReqVO ModelingBusinessCategoryPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ModelingBusinessCategorySaveReqVO 保存请求参数
     * @return ModelingBusinessCategoryDO
     */
     ModelingBusinessCategoryDO convertToDO(ModelingBusinessCategorySaveReqVO ModelingBusinessCategorySaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ModelingBusinessCategoryDO 实体对象
     * @return ModelingBusinessCategoryRespVO
     */
     ModelingBusinessCategoryRespVO convertToRespVO(ModelingBusinessCategoryDO ModelingBusinessCategoryDO);

    /**
     * DOList 转换为 RespVOList
     * @param ModelingBusinessCategoryDOList 实体对象列表
     * @return List<ModelingBusinessCategoryRespVO>
     */
     List<ModelingBusinessCategoryRespVO> convertToRespVOList(List<ModelingBusinessCategoryDO> ModelingBusinessCategoryDOList);
}
