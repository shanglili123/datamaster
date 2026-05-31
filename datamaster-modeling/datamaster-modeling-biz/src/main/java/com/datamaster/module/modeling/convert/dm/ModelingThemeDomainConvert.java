

package com.datamaster.module.modeling.convert.dm;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingThemeDomainDO;

/**
 * 主题域管理 Convert
 *
 * @author FXB
 * @date 2026-03-24
 */
@Mapper
public interface ModelingThemeDomainConvert {
    ModelingThemeDomainConvert INSTANCE = Mappers.getMapper(ModelingThemeDomainConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ModelingThemeDomainPageReqVO 请求参数
     * @return ModelingThemeDomainDO
     */
     ModelingThemeDomainDO convertToDO(ModelingThemeDomainPageReqVO ModelingThemeDomainPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ModelingThemeDomainSaveReqVO 保存请求参数
     * @return ModelingThemeDomainDO
     */
     ModelingThemeDomainDO convertToDO(ModelingThemeDomainSaveReqVO ModelingThemeDomainSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ModelingThemeDomainDO 实体对象
     * @return ModelingThemeDomainRespVO
     */
     ModelingThemeDomainRespVO convertToRespVO(ModelingThemeDomainDO ModelingThemeDomainDO);

    /**
     * DOList 转换为 RespVOList
     * @param ModelingThemeDomainDOList 实体对象列表
     * @return List<ModelingThemeDomainRespVO>
     */
     List<ModelingThemeDomainRespVO> convertToRespVOList(List<ModelingThemeDomainDO> ModelingThemeDomainDOList);
}
