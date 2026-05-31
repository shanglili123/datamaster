package com.datamaster.module.standards.convert.document;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.document.vo.StandardsDocumentPageReqVO;
import com.datamaster.module.standards.controller.admin.document.vo.StandardsDocumentRespVO;
import com.datamaster.module.standards.controller.admin.document.vo.StandardsDocumentSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.document.StandardsDocumentDO;

import java.util.List;

/**
 * 标准信息登记 Convert
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
@Mapper
public interface StandardsDocumentConvert {
    StandardsDocumentConvert INSTANCE = Mappers.getMapper(StandardsDocumentConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDocumentPageReqVO 请求参数
     * @return StandardsDocumentDO
     */
     StandardsDocumentDO convertToDO(StandardsDocumentPageReqVO StandardsDocumentPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDocumentSaveReqVO 保存请求参数
     * @return StandardsDocumentDO
     */
     StandardsDocumentDO convertToDO(StandardsDocumentSaveReqVO StandardsDocumentSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDocumentDO 实体对象
     * @return StandardsDocumentRespVO
     */
     StandardsDocumentRespVO convertToRespVO(StandardsDocumentDO StandardsDocumentDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDocumentDOList 实体对象列表
     * @return List<StandardsDocumentRespVO>
     */
     List<StandardsDocumentRespVO> convertToRespVOList(List<StandardsDocumentDO> StandardsDocumentDOList);
}
