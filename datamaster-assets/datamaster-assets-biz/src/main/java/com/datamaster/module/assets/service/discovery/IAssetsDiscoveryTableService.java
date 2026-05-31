package com.datamaster.module.assets.service.discovery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTablePageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTableRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTableSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTableDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface IAssetsDiscoveryTableService extends IService<AssetsDiscoveryTableDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsDiscoveryTableDO> getDaDiscoveryTablePage(AssetsDiscoveryTablePageReqVO pageReqVO);

    /**
     *
     *
     * @return
     */
    List<AssetsDiscoveryTableDO> getDaDiscoveryTableList(AssetsDiscoveryTablePageReqVO discoveryTablePageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaDiscoveryTable(AssetsDiscoveryTableSaveReqVO createReqVO);
    Long createDaDiscoveryTable(AssetsDiscoveryTableDO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaDiscoveryTable(AssetsDiscoveryTableSaveReqVO updateReqVO);
    int updateDaDiscoveryTable(AssetsDiscoveryTableDO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaDiscoveryTable(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsDiscoveryTableDO getDaDiscoveryTableById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsDiscoveryTableDO> getDaDiscoveryTableList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsDiscoveryTableDO> getDaDiscoveryTableMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaDiscoveryTable(List<AssetsDiscoveryTableRespVO> importExcelList, boolean isUpdateSupport, String operName);

    Integer commitOrRevokeDiscoveryInfo(AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTable);

    Integer updateByTaskIdListAndTableNameStatus(AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTable);
}
