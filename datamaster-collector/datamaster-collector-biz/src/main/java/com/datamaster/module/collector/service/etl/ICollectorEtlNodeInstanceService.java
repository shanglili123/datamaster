

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstancePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据集成节点实例Service接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface ICollectorEtlNodeInstanceService extends IService<CollectorEtlNodeInstanceDO> {

    /**
     * 获得数据集成节点实例分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成节点实例分页列表
     */
    PageResult<CollectorEtlNodeInstanceDO> getCollectorEtlNodeInstancePage(CollectorEtlNodeInstancePageReqVO pageReqVO);

    /**
     * 创建数据集成节点实例
     *
     * @param createReqVO 数据集成节点实例信息
     * @return 数据集成节点实例编号
     */
    Long createCollectorEtlNodeInstance(CollectorEtlNodeInstanceSaveReqVO createReqVO);

    /**
     * 更新数据集成节点实例
     *
     * @param updateReqVO 数据集成节点实例信息
     */
    int updateCollectorEtlNodeInstance(CollectorEtlNodeInstanceSaveReqVO updateReqVO);

    /**
     * 删除数据集成节点实例
     *
     * @param idList 数据集成节点实例编号
     */
    int removeCollectorEtlNodeInstance(Collection<Long> idList);

    /**
     * 获得数据集成节点实例详情
     *
     * @param id 数据集成节点实例编号
     * @return 数据集成节点实例
     */
    CollectorEtlNodeInstanceDO getCollectorEtlNodeInstanceById(Long id);

    /**
     * 获得全部数据集成节点实例列表
     *
     * @return 数据集成节点实例列表
     */
    List<CollectorEtlNodeInstanceDO> getCollectorEtlNodeInstanceList();

    /**
     * 获得全部数据集成节点实例 Map
     *
     * @return 数据集成节点实例 Map
     */
    Map<Long, CollectorEtlNodeInstanceDO> getCollectorEtlNodeInstanceMap();


    /**
     * 导入数据集成节点实例数据
     *
     * @param importExcelList 数据集成节点实例数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCollectorEtlNodeInstance(List<CollectorEtlNodeInstanceRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 创建任务节点实例
     *
     * @param taskInstance
     * @return
     */
    Boolean createNodeInstance(TaskInstance taskInstance);

    /**
     * 更细任务节点实例
     *
     * @param taskInstance
     * @return
     */
    Boolean updateNodeInstance(TaskInstance taskInstance);

    /**
     * 根据dsId获取数据
     *
     * @param dsId
     * @return
     */
    CollectorEtlNodeInstanceDO getByDsId(Long dsId);

    /**
     * 节点实例日志的处理
     *
     * @param taskInstanceId
     * @param processInstanceId
     * @param logStr
     */
    void taskInstanceLogInsert(String taskInstanceId, String processInstanceId, String logStr);

    String getLogByNodeInstanceId(Long nodeInstanceId);
}
