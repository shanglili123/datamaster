package com.datamaster.module.catalog.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceLogDO;

import java.util.Collection;
import java.util.List;

/**
 * 采集任务实例-日志Service接口
 *
 * @author qdata
 * @date 2025-12-16
 */
public interface ICatalogTaskInstanceLogService extends IService<CatalogTaskInstanceLogDO> {

    /**
     * 获得采集任务实例-日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 采集任务实例-日志分页列表
     */
    PageResult<CatalogTaskInstanceLogDO> getCatalogTaskInstanceLogPage(CatalogTaskInstanceLogPageReqVO pageReqVO);

    /**
     * 创建采集任务实例-日志
     *
     * @param createReqVO 采集任务实例-日志信息
     * @return 采集任务实例-日志编号
     */
    Long createCatalogTaskInstanceLog(CatalogTaskInstanceLogSaveReqVO createReqVO);

    /**
     * 更新采集任务实例-日志
     *
     * @param updateReqVO 采集任务实例-日志信息
     */
    int updateCatalogTaskInstanceLog(CatalogTaskInstanceLogSaveReqVO updateReqVO);

    /**
     * 删除采集任务实例-日志
     *
     * @param idList 采集任务实例-日志编号
     */
    int removeCatalogTaskInstanceLog(Collection<Long> idList);

    /**
     * 获得采集任务实例-日志详情
     *
     * @param id 采集任务实例-日志编号
     * @return 采集任务实例-日志
     */
    CatalogTaskInstanceLogDO getCatalogTaskInstanceLogById(Long id);

    /**
     * 获得全部采集任务实例-日志列表
     *
     * @return 采集任务实例-日志列表
     */
    List<CatalogTaskInstanceLogDO> getCatalogTaskInstanceLogList();

    void taskInstanceLogAppend(Long taskInstanceId, Long taskId, String logStr);

    int saveOrUpdateByPk(CatalogTaskInstanceLogDO entity);
}
