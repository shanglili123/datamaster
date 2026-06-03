package com.datamaster.module.assets.service.assetchild.video;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.video.AssetsAssetVideoDO;

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
public interface IAssetsAssetVideoService extends IService<AssetsAssetVideoDO> {

    /**
     * -
     *
     * @param pageReqVO
     * @return -
     */
    PageResult<AssetsAssetVideoDO> getAssetVideoPage(AssetsAssetVideoPageReqVO pageReqVO);

    AssetsAssetVideoRespVO getAssetVideoByAssetId(Long assetId);

    /**
     * -
     *
     * @param createReqVO -
     * @return -
     */
    Long createAssetVideo(AssetsAssetVideoSaveReqVO createReqVO);

    /**
     * -
     *
     * @param updateReqVO -
     */
    int updateAssetVideo(AssetsAssetVideoSaveReqVO updateReqVO);

    /**
     * -
     *
     * @param idList -
     */
    int removeAssetVideo(Collection<Long> idList);

    /**
     * -
     *
     * @param id -
     * @return -
     */
    AssetsAssetVideoDO getAssetVideoById(Long id);

    /**
     * -
     *
     * @return -
     */
    List<AssetsAssetVideoDO> getAssetVideoList();

    /**
     * - Map
     *
     * @return - Map
     */
    Map<Long, AssetsAssetVideoDO> getAssetVideoMap();

    /**
     * -
     *
     * @param importExcelList -
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetVideo(List<AssetsAssetVideoRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void queryServiceForwarding(HttpServletResponse response, AssetsAssetVideoReqVO AssetsAssetVideoReqVO);
}
