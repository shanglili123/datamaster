package com.datamaster.module.assets.service.assetchild.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiDO;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * -APIService
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface IAssetsAssetApiService extends IService<AssetsAssetApiDO> {

    /**
     * -API
     *
     * @param pageReqVO
     * @return -API
     */
    PageResult<AssetsAssetApiDO> getDaAssetApiPage(AssetsAssetApiPageReqVO pageReqVO);

    AssetsAssetApiRespVO getDaAssetApiByAssetId(Long assetId);

    /**
     * -API
     *
     * @param createReqVO -API
     * @return -API
     */
    Long createDaAssetApi(AssetsAssetApiSaveReqVO createReqVO);

    /**
     * -API
     *
     * @param updateReqVO -API
     */
    int updateDaAssetApi(AssetsAssetApiSaveReqVO updateReqVO);

    /**
     * -API
     *
     * @param idList -API
     */
    int removeDaAssetApi(Collection<Long> idList);

    /**
     * -API
     *
     * @param id -API
     * @return -API
     */
    AssetsAssetApiDO getDaAssetApiById(Long id);

    /**
     * -API
     *
     * @return -API
     */
    List<AssetsAssetApiDO> getDaAssetApiList();

    /**
     * -API Map
     *
     * @return -API Map
     */
    Map<Long, AssetsAssetApiDO> getDaAssetApiMap();

    /**
     * -API
     *
     * @param importExcelList -API
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetApi(List<AssetsAssetApiRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void queryServiceForwarding(HttpServletResponse response, AssetsAssetApiReqVO AssetsAssetApi);
}
