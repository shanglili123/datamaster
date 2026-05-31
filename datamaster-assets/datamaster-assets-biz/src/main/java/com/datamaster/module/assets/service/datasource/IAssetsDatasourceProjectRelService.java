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
    PageResult<AssetsDatasourceProjectRelDO> getDaDatasourceProjectRelPage(AssetsDatasourceProjectRelPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaDatasourceProjectRel(AssetsDatasourceProjectRelSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaDatasourceProjectRel(AssetsDatasourceProjectRelSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaDatasourceProjectRel(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsDatasourceProjectRelDO getDaDatasourceProjectRelById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceProjectRelDO> getDaDatasourceProjectRelList();

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceProjectRelDO> getDaDatasourceProjectRelList(AssetsDatasourceProjectRelDO AssetsDatasourceProjectRelDO);

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
    Map<Long, AssetsDatasourceProjectRelDO> getDaDatasourceProjectRelMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaDatasourceProjectRel(List<AssetsDatasourceProjectRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
