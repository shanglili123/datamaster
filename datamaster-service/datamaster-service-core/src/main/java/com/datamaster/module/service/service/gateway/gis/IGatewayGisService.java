package com.datamaster.module.service.service.gateway.gis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisReqVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisRespVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.gis.GatewayGisDO;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据服务-GIS网关 Service 接口
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface IGatewayGisService extends IService<GatewayGisDO> {

    PageResult<GatewayGisDO> getGatewayGisPage(GatewayGisPageReqVO pageReqVO);

    GatewayGisRespVO getGatewayGisByAssetId(Long assetId);

    Long createGatewayGis(GatewayGisSaveReqVO createReqVO);

    int updateGatewayGis(GatewayGisSaveReqVO updateReqVO);

    int removeGatewayGis(Collection<Long> idList);

    GatewayGisDO getGatewayGisById(Long id);

    List<GatewayGisDO> getGatewayGisList();

    Map<Long, GatewayGisDO> getGatewayGisMap();

    String importGatewayGis(List<GatewayGisRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void queryServiceForwarding(HttpServletResponse response, GatewayGisReqVO gatewayGis);
}
