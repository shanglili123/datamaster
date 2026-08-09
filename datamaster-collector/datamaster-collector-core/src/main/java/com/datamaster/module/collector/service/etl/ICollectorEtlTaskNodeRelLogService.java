

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据集成任务节点关系-日志Service接口
 *
 * @author lili.shang
 * @date 2025-02-13
 */
public interface ICollectorEtlTaskNodeRelLogService extends IService<CollectorEtlTaskNodeRelLogDO> {

    /**
     * 获得数据集成任务节点关系-日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成任务节点关系-日志分页列表
     */
    PageResult<CollectorEtlTaskNodeRelLogDO> getCollectorEtlTaskNodeRelLogPage(CollectorEtlTaskNodeRelLogPageReqVO pageReqVO);
    List<CollectorEtlTaskNodeRelLogRespVO> getCollectorEtlTaskNodeRelLogRespVOList(CollectorEtlTaskNodeRelLogPageReqVO pageReqVO);
    CollectorEtlTaskNodeRelLogRespVO getCollectorEtlTaskNodeRelLogById(CollectorEtlTaskNodeRelLogPageReqVO pageReqVO);

    /**
     * 创建数据集成任务节点关系-日志
     *
     * @param createReqVO 数据集成任务节点关系-日志信息
     * @return 数据集成任务节点关系-日志编号
     */
    Long createCollectorEtlTaskNodeRelLog(CollectorEtlTaskNodeRelLogSaveReqVO createReqVO);

    void createCollectorEtlTaskNodeRelLogBatch(List<CollectorEtlTaskNodeRelLogSaveReqVO> CollectorEtlTaskNodeRelLogSaveReqVOS);

    /**
     * 更新数据集成任务节点关系-日志
     *
     * @param updateReqVO 数据集成任务节点关系-日志信息
     */
    int updateCollectorEtlTaskNodeRelLog(CollectorEtlTaskNodeRelLogSaveReqVO updateReqVO);

    /**
     * 删除数据集成任务节点关系-日志
     *
     * @param idList 数据集成任务节点关系-日志编号
     */
    int removeCollectorEtlTaskNodeRelLog(Collection<Long> idList);

    /**
     * 获得数据集成任务节点关系-日志详情
     *
     * @param id 数据集成任务节点关系-日志编号
     * @return 数据集成任务节点关系-日志
     */
    CollectorEtlTaskNodeRelLogDO getCollectorEtlTaskNodeRelLogById(Long id);

    /**
     * 获得全部数据集成任务节点关系-日志列表
     *
     * @return 数据集成任务节点关系-日志列表
     */
    List<CollectorEtlTaskNodeRelLogDO> getCollectorEtlTaskNodeRelLogList();

    /**
     * 获得全部数据集成任务节点关系-日志 Map
     *
     * @return 数据集成任务节点关系-日志 Map
     */
    Map<Long, CollectorEtlTaskNodeRelLogDO> getCollectorEtlTaskNodeRelLogMap();


    /**
     * 导入数据集成任务节点关系-日志数据
     *
     * @param importExcelList 数据集成任务节点关系-日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorEtlTaskNodeRelLog(List<CollectorEtlTaskNodeRelLogRespVO> importExcelList, boolean isUpdateSupport, String operName);
}
