

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据集成节点-日志Service接口
 *
 * @author qdata
 * @date 2025-02-13
 */
public interface ICollectorEtlNodeLogService extends IService<CollectorEtlNodeLogDO> {

    /**
     * 获得数据集成节点-日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成节点-日志分页列表
     */
    PageResult<CollectorEtlNodeLogDO> getCollectorEtlNodeLogPage(CollectorEtlNodeLogPageReqVO pageReqVO);

    CollectorEtlNodeLogDO getCollectorEtlNodeLogRespVOByReqVO(CollectorEtlNodeLogPageReqVO reqVO);

    /**
     * 创建数据集成节点-日志
     *
     * @param createReqVO 数据集成节点-日志信息
     * @return 数据集成节点-日志编号
     */
    Long createCollectorEtlNodeLog(CollectorEtlNodeLogSaveReqVO createReqVO);

    CollectorEtlNodeLogDO createCollectorEtlNodeLogNew(CollectorEtlNodeLogSaveReqVO CollectorEtlNodeLogSaveReqVO);

    List<CollectorEtlNodeLogDO> createCollectorEtlNodeLogBatch(List<CollectorEtlNodeLogSaveReqVO> CollectorEtlNodeLogSaveReqVOS);

    /**
     * 更新数据集成节点-日志
     *
     * @param updateReqVO 数据集成节点-日志信息
     */
    int updateCollectorEtlNodeLog(CollectorEtlNodeLogSaveReqVO updateReqVO);

    /**
     * 删除数据集成节点-日志
     *
     * @param idList 数据集成节点-日志编号
     */
    int removeCollectorEtlNodeLog(Collection<Long> idList);

    /**
     * 获得数据集成节点-日志详情
     *
     * @param id 数据集成节点-日志编号
     * @return 数据集成节点-日志
     */
    CollectorEtlNodeLogDO getCollectorEtlNodeLogById(Long id);

    /**
     * 获得全部数据集成节点-日志列表
     *
     * @return 数据集成节点-日志列表
     */
    List<CollectorEtlNodeLogDO> getCollectorEtlNodeLogList();

    /**
     * 获得全部数据集成节点-日志 Map
     *
     * @return 数据集成节点-日志 Map
     */
    Map<Long, CollectorEtlNodeLogDO> getCollectorEtlNodeLogMap();


    /**
     * 导入数据集成节点-日志数据
     *
     * @param importExcelList 数据集成节点-日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCollectorEtlNodeLog(List<CollectorEtlNodeLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 根据编码和版本获取节点信息
     *
     * @param nodeCode
     * @param version
     * @return
     */
    CollectorEtlNodeLogDO getByNodeCodeAndVersion(String nodeCode, Integer version);

    /**
     * 通过节点编码获取最大版本号
     *
     * @param nodeCode
     * @return
     */
    Integer getMaxVersionByNodeCode(String nodeCode);


    /**
     * 根据任务编码及任务版本获取节点信息
     *
     * @param taskCode
     * @param version
     * @return
     */
    List<CollectorEtlNodeLogDO> listByTaskCode(String taskCode, Integer version);
}
