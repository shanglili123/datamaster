package com.datamaster.module.assets.service.assetchild.spaceRel;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel.AssetsAssetSpaceRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
public interface IAssetsAssetSpaceRelService extends IService<AssetsAssetSpaceRelDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetSpaceRelDO> getAssetSpaceRelPage(AssetsAssetSpaceRelPageReqVO pageReqVO);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetSpaceRelDO> getAssetSpaceRelList(AssetsAssetSpaceRelPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createAssetSpaceRel(AssetsAssetSpaceRelSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateAssetSpaceRel(AssetsAssetSpaceRelSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeAssetSpaceRel(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetSpaceRelDO getAssetSpaceRelById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetSpaceRelDO> getAssetSpaceRelList();

    /**
     *
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetSpaceRelDO> getAssetSpaceRelMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetSpaceRel(List<AssetsAssetSpaceRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    int removeSpaceRelByAssetId(Long assetId);
}
