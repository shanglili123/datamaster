package com.datamaster.module.assets.service.assetchild.gis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.gis.AssetsAssetGisDO;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface IAssetsAssetGisService extends IService<AssetsAssetGisDO> {

    /**
     * -
     *
     * @param pageReqVO
     * @return -
     */
    PageResult<AssetsAssetGisDO> getAssetGisPage(AssetsAssetGisPageReqVO pageReqVO);

    AssetsAssetGisRespVO getAssetGisByAssetId(Long assetId);

    /**
     * -
     *
     * @param createReqVO -
     * @return -
     */
    Long createAssetGis(AssetsAssetGisSaveReqVO createReqVO);

    /**
     * -
     *
     * @param updateReqVO -
     */
    int updateAssetGis(AssetsAssetGisSaveReqVO updateReqVO);

    /**
     * -
     *
     * @param idList -
     */
    int removeAssetGis(Collection<Long> idList);

    /**
     * -
     *
     * @param id -
     * @return -
     */
    AssetsAssetGisDO getAssetGisById(Long id);

    /**
     * -
     *
     * @return -
     */
    List<AssetsAssetGisDO> getAssetGisList();

    /**
     * - Map
     *
     * @return - Map
     */
    Map<Long, AssetsAssetGisDO> getAssetGisMap();

    /**
     * -
     *
     * @param importExcelList -
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetGis(List<AssetsAssetGisRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void queryServiceForwarding(HttpServletResponse response, AssetsAssetGisReqVO AssetsAssetGisReqVO);
}
