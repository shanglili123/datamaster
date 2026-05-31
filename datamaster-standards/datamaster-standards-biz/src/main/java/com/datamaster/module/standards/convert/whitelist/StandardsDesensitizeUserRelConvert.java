

package com.datamaster.module.standards.convert.whitelist;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelPageReqVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelRespVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeUserRelDO;

/**
 * 脱敏白名单与用户关联关系 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Mapper
public interface StandardsDesensitizeUserRelConvert {
    StandardsDesensitizeUserRelConvert INSTANCE = Mappers.getMapper(StandardsDesensitizeUserRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDesensitizeUserRelPageReqVO 请求参数
     * @return StandardsDesensitizeUserRelDO
     */
     StandardsDesensitizeUserRelDO convertToDO(StandardsDesensitizeUserRelPageReqVO StandardsDesensitizeUserRelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDesensitizeUserRelSaveReqVO 保存请求参数
     * @return StandardsDesensitizeUserRelDO
     */
     StandardsDesensitizeUserRelDO convertToDO(StandardsDesensitizeUserRelSaveReqVO StandardsDesensitizeUserRelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDesensitizeUserRelDO 实体对象
     * @return StandardsDesensitizeUserRelRespVO
     */
     StandardsDesensitizeUserRelRespVO convertToRespVO(StandardsDesensitizeUserRelDO StandardsDesensitizeUserRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDesensitizeUserRelDOList 实体对象列表
     * @return List<StandardsDesensitizeUserRelRespVO>
     */
     List<StandardsDesensitizeUserRelRespVO> convertToRespVOList(List<StandardsDesensitizeUserRelDO> StandardsDesensitizeUserRelDOList);
}
