package com.datamaster.module.assets.service.discovery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryColumnDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface IAssetsDiscoveryColumnService extends IService<AssetsDiscoveryColumnDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsDiscoveryColumnDO> getDaDiscoveryColumnPage(AssetsDiscoveryColumnPageReqVO pageReqVO);

    /**
     *
     *
     * @return
     */
    List<AssetsDiscoveryColumnDO> getDaDiscoveryColumnList(AssetsDiscoveryColumnPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaDiscoveryColumn(AssetsDiscoveryColumnSaveReqVO createReqVO);
    Long createDaDiscoveryColumn(AssetsDiscoveryColumnDO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaDiscoveryColumn(AssetsDiscoveryColumnSaveReqVO updateReqVO);
    int updateDaDiscoveryColumn(AssetsDiscoveryColumnDO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaDiscoveryColumn(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsDiscoveryColumnDO getDaDiscoveryColumnById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsDiscoveryColumnDO> getDaDiscoveryColumnList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsDiscoveryColumnDO> getDaDiscoveryColumnMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaDiscoveryColumn(List<AssetsDiscoveryColumnRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
