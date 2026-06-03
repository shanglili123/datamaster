package com.datamaster.module.assets.service.discovery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service
 *
 * @author lili.shang
 * @date 2025-02-17
 */
public interface IAssetsDiscoveryTaskLogService extends IService<AssetsDiscoveryTaskLogDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsDiscoveryTaskLogDO> getDaDiscoveryTaskLogPage(AssetsDiscoveryTaskLogPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaDiscoveryTaskLog(AssetsDiscoveryTaskLogSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaDiscoveryTaskLog(AssetsDiscoveryTaskLogSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaDiscoveryTaskLog(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsDiscoveryTaskLogDO getDaDiscoveryTaskLogById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsDiscoveryTaskLogDO> getDaDiscoveryTaskLogList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsDiscoveryTaskLogDO> getDaDiscoveryTaskLogMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaDiscoveryTaskLog(List<AssetsDiscoveryTaskLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

    String getLogInfo(Long id);
}
