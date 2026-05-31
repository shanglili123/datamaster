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
    PageResult<AssetsAssetProjectRelDO> getDaAssetProjectRelPage(AssetsAssetProjectRelPageReqVO pageReqVO);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetProjectRelDO> getDaAssetProjectRelList(AssetsAssetProjectRelPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaAssetProjectRel(AssetsAssetProjectRelSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaAssetProjectRel(AssetsAssetProjectRelSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaAssetProjectRel(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetProjectRelDO getDaAssetProjectRelById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetProjectRelDO> getDaAssetProjectRelList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetProjectRelDO> getDaAssetProjectRelMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetProjectRel(List<AssetsAssetProjectRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    int removeProjectRelByAssetId(Long assetId);
}
