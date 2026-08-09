package com.datamaster.module.service.convert.gateway.api;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiRespVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiDO;

import java.util.List;

/**
 * 数据服务-API网关 Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface GatewayApiConvert {
    GatewayApiConvert INSTANCE = Mappers.getMapper(GatewayApiConvert.class);

    GatewayApiDO convertToDO(GatewayApiPageReqVO gatewayApiPageReqVO);

    GatewayApiDO convertToDO(GatewayApiSaveReqVO gatewayApiSaveReqVO);

    GatewayApiRespVO convertToRespVO(GatewayApiDO gatewayApiDO);

    List<GatewayApiRespVO> convertToRespVOList(List<GatewayApiDO> gatewayApiDOList);
}
