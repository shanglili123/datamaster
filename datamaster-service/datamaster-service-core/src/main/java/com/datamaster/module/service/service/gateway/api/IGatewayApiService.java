package com.datamaster.module.service.service.gateway.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiRespVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiDO;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据服务-API网关 Service 接口
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface IGatewayApiService extends IService<GatewayApiDO> {

    PageResult<GatewayApiDO> getGatewayApiPage(GatewayApiPageReqVO pageReqVO);

    GatewayApiRespVO getGatewayApiByAssetId(Long assetId);

    Long createGatewayApi(GatewayApiSaveReqVO createReqVO);

    int updateGatewayApi(GatewayApiSaveReqVO updateReqVO);

    int removeGatewayApi(Collection<Long> idList);

    GatewayApiDO getGatewayApiById(Long id);

    List<GatewayApiDO> getGatewayApiList();

    Map<Long, GatewayApiDO> getGatewayApiMap();

    String importGatewayApi(List<GatewayApiRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void queryServiceForwarding(HttpServletResponse response, GatewayApiReqVO gatewayApi);
}
