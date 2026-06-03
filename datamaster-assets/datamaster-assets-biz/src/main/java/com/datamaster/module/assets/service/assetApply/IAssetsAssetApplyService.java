package com.datamaster.module.assets.service.assetApply;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplyRespVO;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplySaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetApply.AssetsAssetApplyDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author shu
 * @date 2025-03-19
 */
public interface IAssetsAssetApplyService extends IService<AssetsAssetApplyDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetApplyDO> getAssetApplyPage(AssetsAssetApplyPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createAssetApply(AssetsAssetApplySaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateAssetApply(AssetsAssetApplySaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeAssetApply(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetApplyDO getAssetApplyById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetApplyDO> getAssetApplyList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetApplyDO> getAssetApplyMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetApply(List<AssetsAssetApplyRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
