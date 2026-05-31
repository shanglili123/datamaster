

package com.datamaster.module.taxonomy.convert.client;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientRespVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientDO;

import java.util.List;

/**
 * 应用管理 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-18
 */
@Mapper
public interface TaxonomyClientConvert {
    TaxonomyClientConvert INSTANCE = Mappers.getMapper(TaxonomyClientConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyClientPageReqVO 请求参数
     * @return TaxonomyClientDO
     */
     TaxonomyClientDO convertToDO(TaxonomyClientPageReqVO TaxonomyClientPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyClientSaveReqVO 保存请求参数
     * @return TaxonomyClientDO
     */
     TaxonomyClientDO convertToDO(TaxonomyClientSaveReqVO TaxonomyClientSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyClientDO 实体对象
     * @return TaxonomyClientRespVO
     */
     TaxonomyClientRespVO convertToRespVO(TaxonomyClientDO TaxonomyClientDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyClientDOList 实体对象列表
     * @return List<TaxonomyClientRespVO>
     */
     List<TaxonomyClientRespVO> convertToRespVOList(List<TaxonomyClientDO> TaxonomyClientDOList);
}
