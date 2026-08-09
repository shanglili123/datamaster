package com.datamaster.module.service.service.gateway.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamRespVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiParamDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据服务-API网关-参数 Service 接口
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface IGatewayApiParamService extends IService<GatewayApiParamDO> {

    PageResult<GatewayApiParamDO> getGatewayApiParamPage(GatewayApiParamPageReqVO pageReqVO);

    Long createGatewayApiParam(GatewayApiParamSaveReqVO createReqVO);

    void createGatewayApiParamDeep(List<GatewayApiParamSaveReqVO> gatewayApiParamList, Long gatewayApiId);

    int updateGatewayApiParam(GatewayApiParamSaveReqVO updateReqVO);

    int removeGatewayApiParam(Collection<Long> idList);

    int removeThemeRelByGatewayApiId(Long gatewayApiId);

    GatewayApiParamDO getGatewayApiParamById(Long id);

    List<GatewayApiParamDO> getGatewayApiParamList();

    List<GatewayApiParamRespVO> getGatewayApiParamList(Long gatewayApiId);

    Map<Long, GatewayApiParamDO> getGatewayApiParamMap();

    String importGatewayApiParam(List<GatewayApiParamRespVO> importExcelList, boolean isUpdateSupport, String operName);
}
