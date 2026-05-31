

package com.datamaster.module.standards.convert.desensitizeList;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnPageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;

/**
 * 脱敏清单关联关系 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Mapper
public interface StandardsDesensitizeAssetcolumnConvert {
    StandardsDesensitizeAssetcolumnConvert INSTANCE = Mappers.getMapper(StandardsDesensitizeAssetcolumnConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDesensitizeAssetcolumnPageReqVO 请求参数
     * @return StandardsDesensitizeAssetcolumnDO
     */
     StandardsDesensitizeAssetcolumnDO convertToDO(StandardsDesensitizeAssetcolumnPageReqVO StandardsDesensitizeAssetcolumnPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDesensitizeAssetcolumnSaveReqVO 保存请求参数
     * @return StandardsDesensitizeAssetcolumnDO
     */
     StandardsDesensitizeAssetcolumnDO convertToDO(StandardsDesensitizeAssetcolumnSaveReqVO StandardsDesensitizeAssetcolumnSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDesensitizeAssetcolumnDO 实体对象
     * @return StandardsDesensitizeAssetcolumnRespVO
     */
     StandardsDesensitizeAssetcolumnRespVO convertToRespVO(StandardsDesensitizeAssetcolumnDO StandardsDesensitizeAssetcolumnDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDesensitizeAssetcolumnDOList 实体对象列表
     * @return List<StandardsDesensitizeAssetcolumnRespVO>
     */
     List<StandardsDesensitizeAssetcolumnRespVO> convertToRespVOList(List<StandardsDesensitizeAssetcolumnDO> StandardsDesensitizeAssetcolumnDOList);
}
