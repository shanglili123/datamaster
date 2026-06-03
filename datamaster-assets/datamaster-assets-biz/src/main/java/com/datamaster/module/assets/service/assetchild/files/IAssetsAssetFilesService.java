package com.datamaster.module.assets.service.assetchild.files;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.files.AssetsAssetFilesDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-06-26
 */
public interface IAssetsAssetFilesService extends IService<AssetsAssetFilesDO> {

    /**
     * -
     *
     * @param pageReqVO
     * @return -
     */
    PageResult<AssetsAssetFilesDO> getAssetFilesPage(AssetsAssetFilesPageReqVO pageReqVO);

    /**
     * -
     *
     * @param createReqVO -
     * @return -
     */
    Long createAssetFiles(AssetsAssetFilesSaveReqVO createReqVO);

    /**
     * -
     *
     * @param updateReqVO -
     */
    int updateAssetFiles(AssetsAssetFilesSaveReqVO updateReqVO);

    /**
     * -
     *
     * @param idList -
     */
    int removeAssetFiles(Collection<Long> idList);

    /**
     * -
     *
     * @param id -
     * @return -
     */
    AssetsAssetFilesDO getAssetFilesById(Long id);

    /**
     * -
     *
     * @return -
     */
    List<AssetsAssetFilesDO> getAssetFilesList();

    /**
     * - Map
     *
     * @return - Map
     */
    Map<Long, AssetsAssetFilesDO> getAssetFilesMap();

    /**
     * -
     *
     * @param importExcelList -
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetFiles(List<AssetsAssetFilesRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
