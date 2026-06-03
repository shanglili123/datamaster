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
    PageResult<AssetsAssetGeoDO> getAssetGeoPage(AssetsAssetGeoPageReqVO pageReqVO);

    AssetsAssetGeoRespVO getAssetGeoByAssetId(Long assetId);

    /**
     * -
     *
     * @param createReqVO -
     * @return -
     */
    Long createAssetGeo(AssetsAssetGeoSaveReqVO createReqVO);

    /**
     * -
     *
     * @param updateReqVO -
     */
    int updateAssetGeo(AssetsAssetGeoSaveReqVO updateReqVO);

    /**
     * -
     *
     * @param idList -
     */
    int removeAssetGeo(Collection<Long> idList);

    /**
     * -
     *
     * @param id -
     * @return -
     */
    AssetsAssetGeoDO getAssetGeoById(Long id);

    /**
     * -
     *
     * @return -
     */
    List<AssetsAssetGeoDO> getAssetGeoList();

    /**
     * - Map
     *
     * @return - Map
     */
    Map<Long, AssetsAssetGeoDO> getAssetGeoMap();

    /**
     * -
     *
     * @param importExcelList -
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetGeo(List<AssetsAssetGeoRespVO> importExcelList, boolean isUpdateSupport, String operName);
}
