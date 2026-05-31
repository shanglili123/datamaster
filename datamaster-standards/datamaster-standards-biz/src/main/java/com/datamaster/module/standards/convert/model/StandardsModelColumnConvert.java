package com.datamaster.module.standards.convert.model;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelColumnDO;

import java.util.List;

/**
 * 逻辑模型属性信息 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface StandardsModelColumnConvert {
    StandardsModelColumnConvert INSTANCE = Mappers.getMapper(StandardsModelColumnConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsModelColumnPageReqVO 请求参数
     * @return StandardsModelColumnDO
     */
     StandardsModelColumnDO convertToDO(StandardsModelColumnPageReqVO StandardsModelColumnPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsModelColumnSaveReqVO 保存请求参数
     * @return StandardsModelColumnDO
     */
     StandardsModelColumnDO convertToDO(StandardsModelColumnSaveReqVO StandardsModelColumnSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsModelColumnDO 实体对象
     * @return StandardsModelColumnRespVO
     */
     StandardsModelColumnRespVO convertToRespVO(StandardsModelColumnDO StandardsModelColumnDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsModelColumnDOList 实体对象列表
     * @return List<StandardsModelColumnRespVO>
     */
     List<StandardsModelColumnRespVO> convertToRespVOList(List<StandardsModelColumnDO> StandardsModelColumnDOList);
}
