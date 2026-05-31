package com.datamaster.module.assets.service.daAssetApply;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.daAssetApply.vo.AssetsAssetApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.daAssetApply.vo.AssetsAssetApplyRespVO;
import com.datamaster.module.assets.controller.admin.daAssetApply.vo.AssetsAssetApplySaveReqVO;
import com.datamaster.module.assets.dal.dataobject.daAssetApply.AssetsAssetApplyDO;

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
    PageResult<AssetsAssetApplyDO> getDaAssetApplyPage(AssetsAssetApplyPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaAssetApply(AssetsAssetApplySaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaAssetApply(AssetsAssetApplySaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaAssetApply(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetApplyDO getDaAssetApplyById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetApplyDO> getDaAssetApplyList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetApplyDO> getDaAssetApplyMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetApply(List<AssetsAssetApplyRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
