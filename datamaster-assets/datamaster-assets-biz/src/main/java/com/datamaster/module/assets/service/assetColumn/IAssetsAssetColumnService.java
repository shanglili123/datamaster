package com.datamaster.module.assets.service.assetColumn;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnRespVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface IAssetsAssetColumnService extends IService<AssetsAssetColumnDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetColumnDO> getDaAssetColumnPage(AssetsAssetColumnPageReqVO pageReqVO);

    AjaxResult getColumnByAssetId(AssetsAssetColumnPageReqVO pageReqVO);

    List<AssetsAssetColumnDO> getDaAssetColumnList(AssetsAssetColumnPageReqVO pageReqVO);
    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaAssetColumn(AssetsAssetColumnSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaAssetColumn(AssetsAssetColumnSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaAssetColumn(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetColumnDO getDaAssetColumnById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetColumnDO> getDaAssetColumnList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetColumnDO> getDaAssetColumnMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetColumn(List<AssetsAssetColumnRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
