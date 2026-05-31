

package com.datamaster.module.modeling.convert.dm;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;

/**
 * 数据域管理 Convert
 *
 * @author FXB
 * @date 2026-03-24
 */
@Mapper
public interface ModelingDataDomainConvert {
    ModelingDataDomainConvert INSTANCE = Mappers.getMapper(ModelingDataDomainConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ModelingDataDomainPageReqVO 请求参数
     * @return ModelingDataDomainDO
     */
     ModelingDataDomainDO convertToDO(ModelingDataDomainPageReqVO ModelingDataDomainPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ModelingDataDomainSaveReqVO 保存请求参数
     * @return ModelingDataDomainDO
     */
     ModelingDataDomainDO convertToDO(ModelingDataDomainSaveReqVO ModelingDataDomainSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ModelingDataDomainDO 实体对象
     * @return ModelingDataDomainRespVO
     */
     ModelingDataDomainRespVO convertToRespVO(ModelingDataDomainDO ModelingDataDomainDO);

    /**
     * DOList 转换为 RespVOList
     * @param ModelingDataDomainDOList 实体对象列表
     * @return List<ModelingDataDomainRespVO>
     */
     List<ModelingDataDomainRespVO> convertToRespVOList(List<ModelingDataDomainDO> ModelingDataDomainDOList);
}
