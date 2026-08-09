package com.datamaster.module.service.convert.gateway.gis;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisRespVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.gis.GatewayGisDO;

import java.util.List;

/**
 * 数据服务-GIS网关 Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface GatewayGisConvert {
    GatewayGisConvert INSTANCE = Mappers.getMapper(GatewayGisConvert.class);

    GatewayGisDO convertToDO(GatewayGisPageReqVO gatewayGisPageReqVO);

    GatewayGisDO convertToDO(GatewayGisSaveReqVO gatewayGisSaveReqVO);

    GatewayGisRespVO convertToRespVO(GatewayGisDO gatewayGisDO);

    List<GatewayGisRespVO> convertToRespVOList(List<GatewayGisDO> gatewayGisDOList);
}
