package com.datamaster.module.service.dal.mapper.gateway.api;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiPageReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据服务-API网关 Mapper
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface GatewayApiMapper extends BaseMapperX<GatewayApiDO> {

    default PageResult<GatewayApiDO> selectPage(GatewayApiPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        return selectPage(reqVO, new LambdaQueryWrapperX<GatewayApiDO>()
                .eqIfPresent(GatewayApiDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(GatewayApiDO::getUrl, reqVO.getUrl())
                .eqIfPresent(GatewayApiDO::getHttpMethod, reqVO.getHttpMethod())
                .eqIfPresent(GatewayApiDO::getCreateTime, reqVO.getCreateTime())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
