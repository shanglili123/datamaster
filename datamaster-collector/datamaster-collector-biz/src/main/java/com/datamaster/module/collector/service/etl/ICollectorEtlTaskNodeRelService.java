

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据集成任务节点关系Service接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface ICollectorEtlTaskNodeRelService extends IService<CollectorEtlTaskNodeRelDO> {

    /**
     * 获得数据集成任务节点关系分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成任务节点关系分页列表
     */
    PageResult<CollectorEtlTaskNodeRelDO> getCollectorEtlTaskNodeRelPage(CollectorEtlTaskNodeRelPageReqVO pageReqVO);
    List<CollectorEtlTaskNodeRelRespVO> getCollectorEtlTaskNodeRelRespVOList(CollectorEtlTaskNodeRelPageReqVO pageReqVO);

    /**
     * 创建数据集成任务节点关系
     *
     * @param createReqVO 数据集成任务节点关系信息
     * @return 数据集成任务节点关系编号
     */
    Long createCollectorEtlTaskNodeRel(CollectorEtlTaskNodeRelSaveReqVO createReqVO);

    void createCollectorEtlTaskNodeRelBatch(List<CollectorEtlTaskNodeRelSaveReqVO> CollectorEtlTaskNodeRelSaveReqVOS);

    /**
     * 更新数据集成任务节点关系
     *
     * @param updateReqVO 数据集成任务节点关系信息
     */
    int updateCollectorEtlTaskNodeRel(CollectorEtlTaskNodeRelSaveReqVO updateReqVO);

    /**
     * 删除数据集成任务节点关系
     *
     * @param idList 数据集成任务节点关系编号
     */
    int removeCollectorEtlTaskNodeRel(Collection<Long> idList);

    /**
     * 获得数据集成任务节点关系详情
     *
     * @param id 数据集成任务节点关系编号
     * @return 数据集成任务节点关系
     */
    CollectorEtlTaskNodeRelDO getCollectorEtlTaskNodeRelById(Long id);

    /**
     * 获得全部数据集成任务节点关系列表
     *
     * @return 数据集成任务节点关系列表
     */
    List<CollectorEtlTaskNodeRelDO> getCollectorEtlTaskNodeRelList();

    /**
     * 获得全部数据集成任务节点关系 Map
     *
     * @return 数据集成任务节点关系 Map
     */
    Map<Long, CollectorEtlTaskNodeRelDO> getCollectorEtlTaskNodeRelMap();


    /**
     * 导入数据集成任务节点关系数据
     *
     * @param importExcelList 数据集成任务节点关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorEtlTaskNodeRel(List<CollectorEtlTaskNodeRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    List<CollectorEtlTaskNodeRelRespVO> removeOldCollectorEtlTaskNodeRel(String code);
}
