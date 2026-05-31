package com.datamaster.module.assets.service.assetchild.theme;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.theme.AssetsAssetThemeRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface IAssetsAssetThemeRelService extends IService<AssetsAssetThemeRelDO> {

    /**
     * -
     *
     * @param pageReqVO
     * @return -
     */
    PageResult<AssetsAssetThemeRelDO> getDaAssetThemeRelPage(AssetsAssetThemeRelPageReqVO pageReqVO);

    /**
     * -
     *
     * @return -
     */
    List<AssetsAssetThemeRelRespVO> getDaAssetThemeRelList(AssetsAssetThemeRelPageReqVO pageReqVO);
    List<Long> getDaAssetIdList(List<Long> themeIdList);

    /**
     * -
     *
     * @param createReqVO -
     * @return -
     */
    Long createDaAssetThemeRel(AssetsAssetThemeRelSaveReqVO createReqVO);

    void createDaAssetThemeRelList(List<String> themeIdList, Long id);

    /**
     * -
     *
     * @param updateReqVO -
     */
    int updateDaAssetThemeRel(AssetsAssetThemeRelSaveReqVO updateReqVO);

    /**
     * -
     *
     * @param idList -
     */
    int removeDaAssetThemeRel(Collection<Long> idList);
    int removeThemeRelByAssetId( Long assetId);

    /**
     * -
     *
     * @param id -
     * @return -
     */
    AssetsAssetThemeRelDO getDaAssetThemeRelById(Long id);

    /**
     * -
     *
     * @return -
     */
    List<AssetsAssetThemeRelDO> getDaAssetThemeRelList();

    /**
     * - Map
     *
     * @return - Map
     */
    Map<Long, AssetsAssetThemeRelDO> getDaAssetThemeRelMap();

    /**
     * -
     *
     * @param importExcelList -
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetThemeRel(List<AssetsAssetThemeRelRespVO> importExcelList, boolean isUpdateSupport, String operName);
}
