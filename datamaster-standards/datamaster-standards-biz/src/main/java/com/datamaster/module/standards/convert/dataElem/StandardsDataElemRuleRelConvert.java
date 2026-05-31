package com.datamaster.module.standards.convert.dataElem;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemRuleRelDO;

import java.util.List;

/**
 * 数据元数据规则关联信息 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface StandardsDataElemRuleRelConvert {
    StandardsDataElemRuleRelConvert INSTANCE = Mappers.getMapper(StandardsDataElemRuleRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDataElemRuleRelPageReqVO 请求参数
     * @return StandardsDataElemRuleRelDO
     */
     StandardsDataElemRuleRelDO convertToDO(StandardsDataElemRuleRelPageReqVO StandardsDataElemRuleRelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDataElemRuleRelSaveReqVO 保存请求参数
     * @return StandardsDataElemRuleRelDO
     */
     StandardsDataElemRuleRelDO convertToDO(StandardsDataElemRuleRelSaveReqVO StandardsDataElemRuleRelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDataElemRuleRelDO 实体对象
     * @return StandardsDataElemRuleRelRespVO
     */
     StandardsDataElemRuleRelRespVO convertToRespVO(StandardsDataElemRuleRelDO StandardsDataElemRuleRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDataElemRuleRelDOList 实体对象列表
     * @return List<StandardsDataElemRuleRelRespVO>
     */
     List<StandardsDataElemRuleRelRespVO> convertToRespVOList(List<StandardsDataElemRuleRelDO> StandardsDataElemRuleRelDOList);
}
