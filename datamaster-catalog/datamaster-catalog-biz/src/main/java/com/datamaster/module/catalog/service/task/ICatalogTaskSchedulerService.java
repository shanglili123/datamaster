package com.datamaster.module.catalog.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskSchedulerDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据集成调度信息Service接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface ICatalogTaskSchedulerService extends IService<CatalogTaskSchedulerDO> {

    /**
     * 获得数据集成调度信息分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成调度信息分页列表
     */
    PageResult<CatalogTaskSchedulerDO> getCatalogTaskSchedulerPage(CatalogTaskSchedulerPageReqVO pageReqVO);

    /**
     * 创建数据集成调度信息
     *
     * @param createReqVO 数据集成调度信息信息
     * @return 数据集成调度信息编号
     */
    Long createCatalogTaskScheduler(CatalogTaskSchedulerSaveReqVO createReqVO);

    /**
     * 更新数据集成调度信息
     *
     * @param updateReqVO 数据集成调度信息信息
     */
    int updateCatalogTaskScheduler(CatalogTaskSchedulerSaveReqVO updateReqVO);

    /**
     * 删除数据集成调度信息
     *
     * @param idList 数据集成调度信息编号
     */
    int removeCatalogTaskScheduler(Collection<Long> idList);

    /**
     * 获得数据集成调度信息详情
     *
     * @param id 数据集成调度信息编号
     * @return 数据集成调度信息
     */
    CatalogTaskSchedulerDO getCatalogTaskSchedulerById(Long id);

    CatalogTaskSchedulerDO getCatalogTaskSchedulerBytaskId(Long taskId);

    /**
     * 获得全部数据集成调度信息列表
     *
     * @return 数据集成调度信息列表
     */
    List<CatalogTaskSchedulerDO> getCatalogTaskSchedulerList();

    /**
     * 获得全部数据集成调度信息 Map
     *
     * @return 数据集成调度信息 Map
     */
    Map<Long, CatalogTaskSchedulerDO> getCatalogTaskSchedulerMap();


    /**
     * 导入数据集成调度信息数据
     *
     * @param importExcelList 数据集成调度信息数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCatalogTaskScheduler(List<CatalogTaskSchedulerRespVO> importExcelList, boolean isUpdateSupport, String operName);


    void updateReleaseSchedule(CatalogTaskSchedulerSaveReqVO updateReqVO);
}
