

package com.datamaster.module.taxonomy.convert.client;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientApiRelDO;

import java.util.List;

/**
 * 应用API服务关联 Convert
 *
 * @author FXB
 * @date 2025-08-21
 */
@Mapper
public interface TaxonomyClientApiRelConvert {
    TaxonomyClientApiRelConvert INSTANCE = Mappers.getMapper(TaxonomyClientApiRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyClientApiRelPageReqVO 请求参数
     * @return TaxonomyClientApiRelDO
     */
     TaxonomyClientApiRelDO convertToDO(TaxonomyClientApiRelPageReqVO TaxonomyClientApiRelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyClientApiRelSaveReqVO 保存请求参数
     * @return TaxonomyClientApiRelDO
     */
     TaxonomyClientApiRelDO convertToDO(TaxonomyClientApiRelSaveReqVO TaxonomyClientApiRelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyClientApiRelDO 实体对象
     * @return TaxonomyClientApiRelRespVO
     */
     TaxonomyClientApiRelRespVO convertToRespVO(TaxonomyClientApiRelDO TaxonomyClientApiRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyClientApiRelDOList 实体对象列表
     * @return List<TaxonomyClientApiRelRespVO>
     */
     List<TaxonomyClientApiRelRespVO> convertToRespVOList(List<TaxonomyClientApiRelDO> TaxonomyClientApiRelDOList);
}
