

package com.datamaster.module.standards.convert.dataLevel;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelRespVO;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataLevel.StandardsDataLevelDO;

/**
 * 数据分级 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Mapper
public interface StandardsDataLevelConvert {
    StandardsDataLevelConvert INSTANCE = Mappers.getMapper(StandardsDataLevelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDataLevelPageReqVO 请求参数
     * @return StandardsDataLevelDO
     */
     StandardsDataLevelDO convertToDO(StandardsDataLevelPageReqVO StandardsDataLevelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDataLevelSaveReqVO 保存请求参数
     * @return StandardsDataLevelDO
     */
     StandardsDataLevelDO convertToDO(StandardsDataLevelSaveReqVO StandardsDataLevelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDataLevelDO 实体对象
     * @return StandardsDataLevelRespVO
     */
     StandardsDataLevelRespVO convertToRespVO(StandardsDataLevelDO StandardsDataLevelDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDataLevelDOList 实体对象列表
     * @return List<StandardsDataLevelRespVO>
     */
     List<StandardsDataLevelRespVO> convertToRespVOList(List<StandardsDataLevelDO> StandardsDataLevelDOList);
}
