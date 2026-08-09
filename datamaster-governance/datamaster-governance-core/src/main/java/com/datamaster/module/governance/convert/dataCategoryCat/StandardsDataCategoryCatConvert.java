

package com.datamaster.module.governance.convert.dataCategoryCat;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.governance.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatPageReqVO;
import com.datamaster.module.governance.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatRespVO;
import com.datamaster.module.governance.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatSaveReqVO;
import com.datamaster.module.governance.dal.dataobject.dataCategoryCat.StandardsDataCategoryCatDO;

/**
 * 数据分类-类目 Convert
 *
 * @author FXB
 * @date 2026-04-07
 */
@Mapper
public interface StandardsDataCategoryCatConvert {
    StandardsDataCategoryCatConvert INSTANCE = Mappers.getMapper(StandardsDataCategoryCatConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDataCategoryCatPageReqVO 请求参数
     * @return StandardsDataCategoryCatDO
     */
     StandardsDataCategoryCatDO convertToDO(StandardsDataCategoryCatPageReqVO StandardsDataCategoryCatPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDataCategoryCatSaveReqVO 保存请求参数
     * @return StandardsDataCategoryCatDO
     */
     StandardsDataCategoryCatDO convertToDO(StandardsDataCategoryCatSaveReqVO StandardsDataCategoryCatSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDataCategoryCatDO 实体对象
     * @return StandardsDataCategoryCatRespVO
     */
     StandardsDataCategoryCatRespVO convertToRespVO(StandardsDataCategoryCatDO StandardsDataCategoryCatDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDataCategoryCatDOList 实体对象列表
     * @return List<StandardsDataCategoryCatRespVO>
     */
     List<StandardsDataCategoryCatRespVO> convertToRespVOList(List<StandardsDataCategoryCatDO> StandardsDataCategoryCatDOList);
}
