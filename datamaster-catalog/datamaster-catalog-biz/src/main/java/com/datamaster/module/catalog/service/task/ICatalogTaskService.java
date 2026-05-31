package com.datamaster.module.catalog.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSaveReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSourceTreeRespVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskScopeDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采集任务Service接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface ICatalogTaskService extends IService<CatalogTaskDO> {

    /**
     * 获得采集任务分页列表
     *
     * @param pageReqVO 分页请求
     * @return 采集任务分页列表
     */
    PageResult<CatalogTaskDO> getCatalogTaskPage(CatalogTaskPageReqVO pageReqVO);

    /**
     * 创建采集任务
     *
     * @param createReqVO 采集任务信息
     * @return 采集任务编号
     */
    Long createCatalogTask(CatalogTaskSaveReqVO createReqVO);

    /**
     * 更新采集任务
     *
     * @param updateReqVO 采集任务信息
     */
    int updateCatalogTask(CatalogTaskSaveReqVO updateReqVO);

    /**
     * 删除采集任务
     *
     * @param idList 采集任务编号
     */
    int removeCatalogTask(Collection<Long> idList);

    /**
     * 获得采集任务详情
     *
     * @param id 采集任务编号
     * @return 采集任务
     */
    CatalogTaskDO getCatalogTaskById(Long id);
    CatalogTaskRespVO getCatalogTaskByIdNew(Long id);

    /**
     * 获得全部采集任务列表
     *
     * @return 采集任务列表
     */
    List<CatalogTaskDO> getCatalogTaskList();

    /**
     * 获得全部采集任务 Map
     *
     * @return 采集任务 Map
     */
    Map<Long, CatalogTaskDO> getCatalogTaskMap();


    /**
     * 导入采集任务数据
     *
     * @param importExcelList 采集任务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCatalogTask(List<CatalogTaskRespVO> importExcelList, boolean isUpdateSupport, String operName);


    /**
     * 定时任务触发
     * @param taskId
     * @return
     */
    boolean runDaDiscoveryTask(Long taskId);

    /**
     * 获取实时采集范围
     *
     * @param id 数据源id
     * @return 实时采集范围列表
     */
    List<CatalogTaskScopeDO> getRealtimeCatalogTaskScopeList(Long id);

    Map<String, Object> updateReleaseJobTask(CatalogTaskSaveReqVO CatalogTask);

    Map<String, Object> updateReleaseSchedule(CatalogTaskSaveReqVO CatalogTask);

    BatchDeleteCheck<Long> batchDeleteCheck(List<Long> list);

    Map<String, Object> runJobOnce(CatalogTaskSaveReqVO CatalogTask);

    /**
     * 获取来源系统树形结构
     * 一级: 来源系统
     * 二级: 数据源
     * 三级: 数据库(根据采集模式展示)
     *
     * @return 树形结构列表
     */
    List<CatalogTaskSourceTreeRespVO> getSourceSystemTree();
}
