package com.datamaster.module.assets.service.datasource;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceSpaceRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
public interface IAssetsDatasourceSpaceRelService extends IService<AssetsDatasourceSpaceRelDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsDatasourceSpaceRelDO> getDatasourceSpaceRelPage(AssetsDatasourceSpaceRelPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDatasourceSpaceRel(AssetsDatasourceSpaceRelSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDatasourceSpaceRel(AssetsDatasourceSpaceRelSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDatasourceSpaceRel(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsDatasourceSpaceRelDO getDatasourceSpaceRelById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceSpaceRelDO> getDatasourceSpaceRelList();

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceSpaceRelDO> getDatasourceSpaceRelList(AssetsDatasourceSpaceRelDO AssetsDatasourceSpaceRelDO);

    /**
     *
     *
     * @return
     */
    List<AssetsDatasourceSpaceRelDO> getJoinSpaceAndDatasource(AssetsDatasourceSpaceRelDO AssetsDatasourceSpaceRelDO);

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsDatasourceSpaceRelDO> getDatasourceSpaceRelMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDatasourceSpaceRel(List<AssetsDatasourceSpaceRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
