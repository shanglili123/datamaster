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
    PageResult<AssetsAssetOperateApplyDO> getAssetOperateApplyPage(AssetsAssetOperateApplyPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createAssetOperateApply(AssetsAssetOperateApplySaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateAssetOperateApply(AssetsAssetOperateApplySaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeAssetOperateApply(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetOperateApplyDO getAssetOperateApplyById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetOperateApplyDO> getAssetOperateApplyList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetOperateApplyDO> getAssetOperateApplyMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetOperateApply(List<AssetsAssetOperateApplyRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
