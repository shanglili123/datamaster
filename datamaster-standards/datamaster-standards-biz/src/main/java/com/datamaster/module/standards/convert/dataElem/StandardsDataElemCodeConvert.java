package com.datamaster.module.standards.convert.dataElem;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodePageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodeRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodeSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemCodeDO;

import java.util.List;

/**
 * 数据元代码 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface StandardsDataElemCodeConvert {
    StandardsDataElemCodeConvert INSTANCE = Mappers.getMapper(StandardsDataElemCodeConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDataElemCodePageReqVO 请求参数
     * @return StandardsDataElemCodeDO
     */
     StandardsDataElemCodeDO convertToDO(StandardsDataElemCodePageReqVO StandardsDataElemCodePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDataElemCodeSaveReqVO 保存请求参数
     * @return StandardsDataElemCodeDO
     */
     StandardsDataElemCodeDO convertToDO(StandardsDataElemCodeSaveReqVO StandardsDataElemCodeSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDataElemCodeDO 实体对象
     * @return StandardsDataElemCodeRespVO
     */
     StandardsDataElemCodeRespVO convertToRespVO(StandardsDataElemCodeDO StandardsDataElemCodeDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDataElemCodeDOList 实体对象列表
     * @return List<StandardsDataElemCodeRespVO>
     */
     List<StandardsDataElemCodeRespVO> convertToRespVOList(List<StandardsDataElemCodeDO> StandardsDataElemCodeDOList);
}
