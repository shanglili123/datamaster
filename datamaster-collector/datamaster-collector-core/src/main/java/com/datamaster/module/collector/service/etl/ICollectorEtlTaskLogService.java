

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据集成任务-日志Service接口
 *
 * @author lili.shang
 * @date 2025-02-13
 */
public interface ICollectorEtlTaskLogService extends IService<CollectorEtlTaskLogDO> {

    /**
     * 获得数据集成任务-日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成任务-日志分页列表
     */
    PageResult<CollectorEtlTaskLogDO> getCollectorEtlTaskLogPage(CollectorEtlTaskLogPageReqVO pageReqVO);

    CollectorEtlTaskLogRespVO getCollectorEtlTaskLogById(CollectorEtlTaskLogPageReqVO pageReqVO);

    /**
     * 创建数据集成任务-日志
     *
     * @param createReqVO 数据集成任务-日志信息
     * @return 数据集成任务-日志编号
     */
    Long createCollectorEtlTaskLog(CollectorEtlTaskLogSaveReqVO createReqVO);

    /**
     * 更新数据集成任务-日志
     *
     * @param updateReqVO 数据集成任务-日志信息
     */
    int updateCollectorEtlTaskLog(CollectorEtlTaskLogSaveReqVO updateReqVO);

    /**
     * 删除数据集成任务-日志
     *
     * @param idList 数据集成任务-日志编号
     */
    int removeCollectorEtlTaskLog(Collection<Long> idList);

    /**
     * 获得数据集成任务-日志详情
     *
     * @param id 数据集成任务-日志编号
     * @return 数据集成任务-日志
     */
    CollectorEtlTaskLogDO getCollectorEtlTaskLogById(Long id);

    /**
     * 获得全部数据集成任务-日志列表
     *
     * @return 数据集成任务-日志列表
     */
    List<CollectorEtlTaskLogDO> getCollectorEtlTaskLogList();

    /**
     * 获得全部数据集成任务-日志 Map
     *
     * @return 数据集成任务-日志 Map
     */
    Map<Long, CollectorEtlTaskLogDO> getCollectorEtlTaskLogMap();


    /**
     * 导入数据集成任务-日志数据
     *
     * @param importExcelList 数据集成任务-日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCollectorEtlTaskLog(List<CollectorEtlTaskLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 根据任务编码获取最大版本号
     *
     * @param taskCode
     * @return
     */
    Integer queryMaxVersionByCode(String taskCode);
}
