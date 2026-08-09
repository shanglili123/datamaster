package com.datamaster.module.service.dal.mapper.gateway.api;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamPageReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiParamDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据服务-API网关-参数 Mapper
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface GatewayApiParamMapper extends BaseMapperX<GatewayApiParamDO> {

    default PageResult<GatewayApiParamDO> selectPage(GatewayApiParamPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        return selectPage(reqVO, new LambdaQueryWrapperX<GatewayApiParamDO>()
                .eqIfPresent(GatewayApiParamDO::getApiId, reqVO.getApiId())
                .eqIfPresent(GatewayApiParamDO::getParentId, reqVO.getParentId())
                .likeIfPresent(GatewayApiParamDO::getName, reqVO.getName())
                .eqIfPresent(GatewayApiParamDO::getType, reqVO.getType())
                .eqIfPresent(GatewayApiParamDO::getRequestFlag, reqVO.getRequestFlag())
                .eqIfPresent(GatewayApiParamDO::getColumnType, reqVO.getColumnType())
                .eqIfPresent(GatewayApiParamDO::getCreateTime, reqVO.getCreateTime())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    void removeThemeRelByGatewayApiId(Long id);
}
