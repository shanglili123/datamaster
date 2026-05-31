

package com.datamaster.module.modeling.convert.dm;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerSpecificationDO;

/**
 * 数仓分层-规范管理 Convert
 *
 * @author FXB
 * @date 2026-03-24
 */
@Mapper
public interface ModelingDataLayerSpecificationConvert {
    ModelingDataLayerSpecificationConvert INSTANCE = Mappers.getMapper(ModelingDataLayerSpecificationConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ModelingDataLayerSpecificationPageReqVO 请求参数
     * @return ModelingDataLayerSpecificationDO
     */
     ModelingDataLayerSpecificationDO convertToDO(ModelingDataLayerSpecificationPageReqVO ModelingDataLayerSpecificationPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ModelingDataLayerSpecificationSaveReqVO 保存请求参数
     * @return ModelingDataLayerSpecificationDO
     */
     ModelingDataLayerSpecificationDO convertToDO(ModelingDataLayerSpecificationSaveReqVO ModelingDataLayerSpecificationSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ModelingDataLayerSpecificationDO 实体对象
     * @return ModelingDataLayerSpecificationRespVO
     */
     ModelingDataLayerSpecificationRespVO convertToRespVO(ModelingDataLayerSpecificationDO ModelingDataLayerSpecificationDO);

    /**
     * DOList 转换为 RespVOList
     * @param ModelingDataLayerSpecificationDOList 实体对象列表
     * @return List<ModelingDataLayerSpecificationRespVO>
     */
     List<ModelingDataLayerSpecificationRespVO> convertToRespVOList(List<ModelingDataLayerSpecificationDO> ModelingDataLayerSpecificationDOList);
}
