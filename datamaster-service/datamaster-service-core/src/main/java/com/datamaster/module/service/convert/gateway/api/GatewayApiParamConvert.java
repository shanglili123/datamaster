package com.datamaster.module.service.convert.gateway.api;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamRespVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiParamDO;

import java.util.List;

/**
 * 数据服务-API网关-参数 Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface GatewayApiParamConvert {
    GatewayApiParamConvert INSTANCE = Mappers.getMapper(GatewayApiParamConvert.class);

    GatewayApiParamDO convertToDO(GatewayApiParamPageReqVO gatewayApiParamPageReqVO);

    GatewayApiParamDO convertToDO(GatewayApiParamSaveReqVO gatewayApiParamSaveReqVO);

    GatewayApiParamRespVO convertToRespVO(GatewayApiParamDO gatewayApiParamDO);

    List<GatewayApiParamRespVO> convertToRespVOList(List<GatewayApiParamDO> gatewayApiParamDOList);
}
