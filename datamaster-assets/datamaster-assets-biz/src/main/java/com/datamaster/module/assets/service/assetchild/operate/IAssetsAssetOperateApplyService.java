package com.datamaster.module.assets.service.assetchild.operate;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplySaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateApplyDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
public interface IAssetsAssetOperateApplyService extends IService<AssetsAssetOperateApplyDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetOperateApplyDO> getDaAssetOperateApplyPage(AssetsAssetOperateApplyPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaAssetOperateApply(AssetsAssetOperateApplySaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaAssetOperateApply(AssetsAssetOperateApplySaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaAssetOperateApply(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetOperateApplyDO getDaAssetOperateApplyById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetOperateApplyDO> getDaAssetOperateApplyList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetOperateApplyDO> getDaAssetOperateApplyMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetOperateApply(List<AssetsAssetOperateApplyRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
