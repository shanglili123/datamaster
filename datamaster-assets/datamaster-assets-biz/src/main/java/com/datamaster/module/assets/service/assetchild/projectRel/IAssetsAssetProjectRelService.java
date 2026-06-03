package com.datamaster.module.assets.service.assetchild.projectRel;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.projectRel.AssetsAssetProjectRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
public interface IAssetsAssetProjectRelService extends IService<AssetsAssetProjectRelDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetProjectRelDO> getAssetProjectRelPage(AssetsAssetProjectRelPageReqVO pageReqVO);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetProjectRelDO> getAssetProjectRelList(AssetsAssetProjectRelPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createAssetProjectRel(AssetsAssetProjectRelSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateAssetProjectRel(AssetsAssetProjectRelSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeAssetProjectRel(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetProjectRelDO getAssetProjectRelById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetProjectRelDO> getAssetProjectRelList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetProjectRelDO> getAssetProjectRelMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetProjectRel(List<AssetsAssetProjectRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    int removeProjectRelByAssetId(Long assetId);
}
