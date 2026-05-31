

package com.datamaster.module.standards.convert.whitelist;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistPageReqVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistRespVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;

/**
 * 脱敏白名单 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Mapper
public interface StandardsDesensitizeWhitelistConvert {
    StandardsDesensitizeWhitelistConvert INSTANCE = Mappers.getMapper(StandardsDesensitizeWhitelistConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDesensitizeWhitelistPageReqVO 请求参数
     * @return StandardsDesensitizeWhitelistDO
     */
     StandardsDesensitizeWhitelistDO convertToDO(StandardsDesensitizeWhitelistPageReqVO StandardsDesensitizeWhitelistPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDesensitizeWhitelistSaveReqVO 保存请求参数
     * @return StandardsDesensitizeWhitelistDO
     */
     StandardsDesensitizeWhitelistDO convertToDO(StandardsDesensitizeWhitelistSaveReqVO StandardsDesensitizeWhitelistSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDesensitizeWhitelistDO 实体对象
     * @return StandardsDesensitizeWhitelistRespVO
     */
     StandardsDesensitizeWhitelistRespVO convertToRespVO(StandardsDesensitizeWhitelistDO StandardsDesensitizeWhitelistDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDesensitizeWhitelistDOList 实体对象列表
     * @return List<StandardsDesensitizeWhitelistRespVO>
     */
     List<StandardsDesensitizeWhitelistRespVO> convertToRespVOList(List<StandardsDesensitizeWhitelistDO> StandardsDesensitizeWhitelistDOList);
}
