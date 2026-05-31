package com.datamaster.module.assets.service.discovery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface IAssetsDiscoveryTaskService extends IService<AssetsDiscoveryTaskDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsDiscoveryTaskDO> getDaDiscoveryTaskPage(AssetsDiscoveryTaskPageReqVO pageReqVO);
    PageResult<AssetsDiscoveryTaskRespVO> getDaDiscoveryTaskListPage(AssetsDiscoveryTaskPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaDiscoveryTask(AssetsDiscoveryTaskSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaDiscoveryTask(AssetsDiscoveryTaskSaveReqVO updateReqVO);
    int updateDaDiscoveryTask(AssetsDiscoveryTaskRespVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaDiscoveryTask(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsDiscoveryTaskRespVO getDaDiscoveryTaskById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsDiscoveryTaskDO> getDaDiscoveryTaskList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsDiscoveryTaskDO> getDaDiscoveryTaskMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaDiscoveryTask(List<AssetsDiscoveryTaskRespVO> importExcelList, boolean isUpdateSupport, String operName);

    boolean runDaDiscoveryTask(Long taskId);

    boolean updateDaDiscoveryTaskStatus(AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask);

    boolean updateDaDiscoveryTaskCronExpression(AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask);

    AjaxResult startDaDiscoveryTask(Long id);
}
