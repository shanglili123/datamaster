package com.datamaster.module.assets.service.assetchild.geo;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.geo.AssetsAssetGeoDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface IAssetsAssetGeoService extends IService<AssetsAssetGeoDO> {

    /**
     * -
     *
     * @param pageReqVO
     * @return -
     */
    PageResult<AssetsAssetGeoDO> getDaAssetGeoPage(AssetsAssetGeoPageReqVO pageReqVO);

    AssetsAssetGeoRespVO getDaAssetGeoByAssetId(Long assetId);

    /**
     * -
     *
     * @param createReqVO -
     * @return -
     */
    Long createDaAssetGeo(AssetsAssetGeoSaveReqVO createReqVO);

    /**
     * -
     *
     * @param updateReqVO -
     */
    int updateDaAssetGeo(AssetsAssetGeoSaveReqVO updateReqVO);

    /**
     * -
     *
     * @param idList -
     */
    int removeDaAssetGeo(Collection<Long> idList);

    /**
     * -
     *
     * @param id -
     * @return -
     */
    AssetsAssetGeoDO getDaAssetGeoById(Long id);

    /**
     * -
     *
     * @return -
     */
    List<AssetsAssetGeoDO> getDaAssetGeoList();

    /**
     * - Map
     *
     * @return - Map
     */
    Map<Long, AssetsAssetGeoDO> getDaAssetGeoMap();

    /**
     * -
     *
     * @param importExcelList -
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetGeo(List<AssetsAssetGeoRespVO> importExcelList, boolean isUpdateSupport, String operName);
}
