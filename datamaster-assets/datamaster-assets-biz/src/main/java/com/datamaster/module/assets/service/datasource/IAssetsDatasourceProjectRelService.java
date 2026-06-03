package com.datamaster.module.assets.service.datasource;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceProjectRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
public interface IAssetsDatasourceProjectRelService extends IService<AssetsDatasourceProjectRelDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsDatasourceProjectRelDO> getDatasourceProjectRelPage(AssetsDatasourceProjectRelPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDatasourceProjectRel(AssetsDatasourceProjectRelSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDatasourceProjectRel(AssetsDatasourceProjectRelSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDatasourceProjectRel(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsDatasourceProjectRelDO getDatasourceProjectRelById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceProjectRelDO> getDatasourceProjectRelList();

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceProjectRelDO> getDatasourceProjectRelList(AssetsDatasourceProjectRelDO AssetsDatasourceProjectRelDO);

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceProjectRelDO> getJoinProjectAndDatasource(AssetsDatasourceProjectRelDO AssetsDatasourceProjectRelDO);

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsDatasourceProjectRelDO> getDatasourceProjectRelMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDatasourceProjectRel(List<AssetsDatasourceProjectRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
