

package com.datamaster.module.modeling.convert.dm;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerDO;

/**
 * 数仓分层管理 Convert
 *
 * @author FXB
 * @date 2026-03-24
 */
@Mapper
public interface ModelingDataLayerConvert {
    ModelingDataLayerConvert INSTANCE = Mappers.getMapper(ModelingDataLayerConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ModelingDataLayerPageReqVO 请求参数
     * @return ModelingDataLayerDO
     */
     ModelingDataLayerDO convertToDO(ModelingDataLayerPageReqVO ModelingDataLayerPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ModelingDataLayerSaveReqVO 保存请求参数
     * @return ModelingDataLayerDO
     */
     ModelingDataLayerDO convertToDO(ModelingDataLayerSaveReqVO ModelingDataLayerSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ModelingDataLayerDO 实体对象
     * @return ModelingDataLayerRespVO
     */
     ModelingDataLayerRespVO convertToRespVO(ModelingDataLayerDO ModelingDataLayerDO);

    /**
     * DOList 转换为 RespVOList
     * @param ModelingDataLayerDOList 实体对象列表
     * @return List<ModelingDataLayerRespVO>
     */
     List<ModelingDataLayerRespVO> convertToRespVOList(List<ModelingDataLayerDO> ModelingDataLayerDOList);
}
