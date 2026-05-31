

package com.datamaster.module.standards.convert.dataCategory;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryPageReqVO;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryRespVO;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategorySaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataCategory.StandardsDataCategoryDO;

/**
 * 数据分类 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
@Mapper
public interface StandardsDataCategoryConvert {
    StandardsDataCategoryConvert INSTANCE = Mappers.getMapper(StandardsDataCategoryConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDataCategoryPageReqVO 请求参数
     * @return StandardsDataCategoryDO
     */
     StandardsDataCategoryDO convertToDO(StandardsDataCategoryPageReqVO StandardsDataCategoryPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDataCategorySaveReqVO 保存请求参数
     * @return StandardsDataCategoryDO
     */
     StandardsDataCategoryDO convertToDO(StandardsDataCategorySaveReqVO StandardsDataCategorySaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDataCategoryDO 实体对象
     * @return StandardsDataCategoryRespVO
     */
     StandardsDataCategoryRespVO convertToRespVO(StandardsDataCategoryDO StandardsDataCategoryDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDataCategoryDOList 实体对象列表
     * @return List<StandardsDataCategoryRespVO>
     */
     List<StandardsDataCategoryRespVO> convertToRespVOList(List<StandardsDataCategoryDO> StandardsDataCategoryDOList);
}
