package com.datamaster.module.standards.convert.dataElem;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemDO;

import java.util.List;

/**
 * 数据元 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface StandardsDataElemConvert {
    StandardsDataElemConvert INSTANCE = Mappers.getMapper(StandardsDataElemConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDataElemPageReqVO 请求参数
     * @return StandardsDataElemDO
     */
     StandardsDataElemDO convertToDO(StandardsDataElemPageReqVO StandardsDataElemPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDataElemSaveReqVO 保存请求参数
     * @return StandardsDataElemDO
     */
     StandardsDataElemDO convertToDO(StandardsDataElemSaveReqVO StandardsDataElemSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDataElemDO 实体对象
     * @return StandardsDataElemRespVO
     */
     StandardsDataElemRespVO convertToRespVO(StandardsDataElemDO StandardsDataElemDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDataElemDOList 实体对象列表
     * @return List<StandardsDataElemRespVO>
     */
     List<StandardsDataElemRespVO> convertToRespVOList(List<StandardsDataElemDO> StandardsDataElemDOList);
}
