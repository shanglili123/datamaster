package com.datamaster.module.assets.service.assetchild.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiParamDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * -API-Service
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
public interface IAssetsAssetApiParamService extends IService<AssetsAssetApiParamDO> {

    /**
     * -API-
     *
     * @param pageReqVO
     * @return -API-
     */
    PageResult<AssetsAssetApiParamDO> getAssetApiParamPage(AssetsAssetApiParamPageReqVO pageReqVO);

    /**
     * -API-
     *
     * @param createReqVO -API-
     * @return -API-
     */
    Long createAssetApiParam(AssetsAssetApiParamSaveReqVO createReqVO);

    void createAssetApiParamDeep(List<AssetsAssetApiParamSaveReqVO> AssetsAssetApiParamList, Long AssetsAssetApiId);

    /**
     * -API-
     *
     * @param updateReqVO -API-
     */
    int updateAssetApiParam(AssetsAssetApiParamSaveReqVO updateReqVO);

    /**
     * -API-
     *
     * @param idList -API-
     */
    int removeAssetApiParam(Collection<Long> idList);
    int removeThemeRelByAssetApiId( Long assetApiId);

    /**
     * -API-
     *
     * @param id -API-
     * @return -API-
     */
    AssetsAssetApiParamDO getAssetApiParamById(Long id);

    /**
     * -API-
     *
     * @return -API-
     */
    List<AssetsAssetApiParamDO> getAssetApiParamList();
    List<AssetsAssetApiParamRespVO> getAssetApiParamList(Long AssetsAssetApiId);

    /**
     * -API- Map
     *
     * @return -API- Map
     */
    Map<Long, AssetsAssetApiParamDO> getAssetApiParamMap();

    /**
     * -API-
     *
     * @param importExcelList -API-
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetApiParam(List<AssetsAssetApiParamRespVO> importExcelList, boolean isUpdateSupport, String operName);
}
