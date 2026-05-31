package com.datamaster.module.standards.convert.codeMap;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapPageReqVO;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapRespVO;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.codeMap.StandardsCodeMapDO;

import java.util.List;

/**
 * 数据元代码映射 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface StandardsCodeMapConvert {
    StandardsCodeMapConvert INSTANCE = Mappers.getMapper(StandardsCodeMapConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsCodeMapPageReqVO 请求参数
     * @return StandardsCodeMapDO
     */
     StandardsCodeMapDO convertToDO(StandardsCodeMapPageReqVO StandardsCodeMapPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsCodeMapSaveReqVO 保存请求参数
     * @return StandardsCodeMapDO
     */
     StandardsCodeMapDO convertToDO(StandardsCodeMapSaveReqVO StandardsCodeMapSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsCodeMapDO 实体对象
     * @return StandardsCodeMapRespVO
     */
     StandardsCodeMapRespVO convertToRespVO(StandardsCodeMapDO StandardsCodeMapDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsCodeMapDOList 实体对象列表
     * @return List<StandardsCodeMapRespVO>
     */
     List<StandardsCodeMapRespVO> convertToRespVOList(List<StandardsCodeMapDO> StandardsCodeMapDOList);
}
