

package com.datamaster.module.governance.convert.space;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpaceUserRelPageReqVO;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpaceUserRelRespVO;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpaceUserRelSaveReqVO;
import com.datamaster.module.governance.dal.dataobject.space.TaxonomySpaceUserRelDO;

import java.util.List;

/**
 * 空间与用户关联关系 Convert
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Mapper
public interface TaxonomySpaceUserRelConvert {
    TaxonomySpaceUserRelConvert INSTANCE = Mappers.getMapper(TaxonomySpaceUserRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param pageReqVO 请求参数
     * @return TaxonomySpaceUserRelDO
     */
     TaxonomySpaceUserRelDO convertToDO(TaxonomySpaceUserRelPageReqVO pageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param saveReqVO 保存请求参数
     * @return TaxonomySpaceUserRelDO
     */
     TaxonomySpaceUserRelDO convertToDO(TaxonomySpaceUserRelSaveReqVO saveReqVO);

    /**
     * DO 转换为 RespVO
     * @param spaceUserRelDO 实体对象
     * @return TaxonomySpaceUserRelRespVO
     */
     TaxonomySpaceUserRelRespVO convertToRespVO(TaxonomySpaceUserRelDO spaceUserRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param spaceUserRelDOList 实体对象列表
     * @return List<TaxonomySpaceUserRelRespVO>
     */
     List<TaxonomySpaceUserRelRespVO> convertToRespVOList(List<TaxonomySpaceUserRelDO> spaceUserRelDOList);
}
