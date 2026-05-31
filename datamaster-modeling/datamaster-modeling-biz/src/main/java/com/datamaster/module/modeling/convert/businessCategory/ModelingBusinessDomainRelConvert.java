

package com.datamaster.module.modeling.convert.businessCategory;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelPageReqVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelRespVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessDomainRelDO;

import java.util.List;

/**
 * 业务分类数据域关联关系 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Mapper
public interface ModelingBusinessDomainRelConvert {
    ModelingBusinessDomainRelConvert INSTANCE = Mappers.getMapper(ModelingBusinessDomainRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ModelingBusinessDomainRelPageReqVO 请求参数
     * @return ModelingBusinessDomainRelDO
     */
     ModelingBusinessDomainRelDO convertToDO(ModelingBusinessDomainRelPageReqVO ModelingBusinessDomainRelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ModelingBusinessDomainRelSaveReqVO 保存请求参数
     * @return ModelingBusinessDomainRelDO
     */
     ModelingBusinessDomainRelDO convertToDO(ModelingBusinessDomainRelSaveReqVO ModelingBusinessDomainRelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ModelingBusinessDomainRelDO 实体对象
     * @return ModelingBusinessDomainRelRespVO
     */
     ModelingBusinessDomainRelRespVO convertToRespVO(ModelingBusinessDomainRelDO ModelingBusinessDomainRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param ModelingBusinessDomainRelDOList 实体对象列表
     * @return List<ModelingBusinessDomainRelRespVO>
     */
     List<ModelingBusinessDomainRelRespVO> convertToRespVOList(List<ModelingBusinessDomainRelDO> ModelingBusinessDomainRelDOList);
}
