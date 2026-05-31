

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSchedulerDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据集成调度信息Service接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface ICollectorEtlSchedulerService extends IService<CollectorEtlSchedulerDO> {

    /**
     * 获得数据集成调度信息分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成调度信息分页列表
     */
    PageResult<CollectorEtlSchedulerDO> getCollectorEtlSchedulerPage(CollectorEtlSchedulerPageReqVO pageReqVO);

    /**
     * 创建数据集成调度信息
     *
     * @param createReqVO 数据集成调度信息信息
     * @return 数据集成调度信息编号
     */
    Long createCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO createReqVO);
    CollectorEtlSchedulerDO createCollectorEtlSchedulerNew(CollectorEtlSchedulerSaveReqVO createReqVO);

    /**
     * 更新数据集成调度信息
     *
     * @param updateReqVO 数据集成调度信息信息
     */
    int updateCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO updateReqVO);

    /**
     * 删除数据集成调度信息
     *
     * @param idList 数据集成调度信息编号
     */
    int removeCollectorEtlScheduler(Collection<Long> idList);

    /**
     * 获得数据集成调度信息详情
     *
     * @param id 数据集成调度信息编号
     * @return 数据集成调度信息
     */
    CollectorEtlSchedulerDO getCollectorEtlSchedulerById(Long id);

    CollectorEtlSchedulerDO getCollectorEtlSchedulerById(CollectorEtlSchedulerPageReqVO pageReqVO);

    /**
     * 获得全部数据集成调度信息列表
     *
     * @return 数据集成调度信息列表
     */
    List<CollectorEtlSchedulerDO> getCollectorEtlSchedulerList();

    /**
     * 获得全部数据集成调度信息 Map
     *
     * @return 数据集成调度信息 Map
     */
    Map<Long, CollectorEtlSchedulerDO> getCollectorEtlSchedulerMap();


    /**
     * 导入数据集成调度信息数据
     *
     * @param importExcelList 数据集成调度信息数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorEtlScheduler(List<CollectorEtlSchedulerRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
