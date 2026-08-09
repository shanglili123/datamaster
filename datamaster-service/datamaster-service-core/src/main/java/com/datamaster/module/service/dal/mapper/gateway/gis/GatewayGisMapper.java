package com.datamaster.module.service.dal.mapper.gateway.gis;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisPageReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.gis.GatewayGisDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据服务-GIS网关 Mapper
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface GatewayGisMapper extends BaseMapperX<GatewayGisDO> {

    default PageResult<GatewayGisDO> selectPage(GatewayGisPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        return selectPage(reqVO, new LambdaQueryWrapperX<GatewayGisDO>()
                .eqIfPresent(GatewayGisDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(GatewayGisDO::getUrl, reqVO.getUrl())
                .eqIfPresent(GatewayGisDO::getType, reqVO.getType())
                .eqIfPresent(GatewayGisDO::getHttpMethod, reqVO.getHttpMethod())
                .eqIfPresent(GatewayGisDO::getCoordinateSystem, reqVO.getCoordinateSystem())
                .eqIfPresent(GatewayGisDO::getCreateTime, reqVO.getCreateTime())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
