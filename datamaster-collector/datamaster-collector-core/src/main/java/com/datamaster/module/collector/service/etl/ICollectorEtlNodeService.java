

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlNodeRespDTO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据集成节点Service接口
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
public interface ICollectorEtlNodeService extends IService<CollectorEtlNodeDO> {

    /**
     * 获得数据集成节点分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成节点分页列表
     */
    PageResult<CollectorEtlNodeDO> getCollectorEtlNodePage(CollectorEtlNodePageReqVO pageReqVO);

    List<CollectorEtlNodeRespVO> getCollectorEtlNodeRespList(CollectorEtlNodePageReqVO pageReqVO);

    /**
     * 通过任务id获取节点列表
     *
     * @param taskId
     * @return
     */
    List<CollectorEtlNodeRespVO> listNodeByTaskId(Long taskId);

    CollectorEtlNodeRespVO getCollectorEtlNodeRespVOByReqVO(CollectorEtlNodePageReqVO reqVOPre);

    /**
     * 创建数据集成节点
     *
     * @param createReqVO 数据集成节点信息
     * @return 数据集成节点编号
     */
    Long createCollectorEtlNode(CollectorEtlNodeSaveReqVO createReqVO);

    List<CollectorEtlNodeDO> createCollectorEtlNodeBatch(List<CollectorEtlNodeSaveReqVO> CollectorEtlNodeSaveReqVOList);

    /**
     * 更新数据集成节点
     *
     * @param updateReqVO 数据集成节点信息
     */
    int updateCollectorEtlNode(CollectorEtlNodeSaveReqVO updateReqVO);

    /**
     * 删除数据集成节点
     *
     * @param idList 数据集成节点编号
     */
    int removeCollectorEtlNode(Collection<Long> idList);

    /**
     * 获得数据集成节点详情
     *
     * @param id 数据集成节点编号
     * @return 数据集成节点
     */
    CollectorEtlNodeDO getCollectorEtlNodeById(Long id);

    /**
     * 获得全部数据集成节点列表
     *
     * @return 数据集成节点列表
     */
    List<CollectorEtlNodeDO> getCollectorEtlNodeList();

    /**
     * 获得全部数据集成节点 Map
     *
     * @return 数据集成节点 Map
     */
    Map<Long, CollectorEtlNodeDO> getCollectorEtlNodeMap();


    /**
     * 导入数据集成节点数据
     *
     * @param importExcelList 数据集成节点数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCollectorEtlNode(List<CollectorEtlNodeRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void removeOldCollectorEtlNode(List<String> code);

    /**
     * 通过节点编码获取节点id
     *
     * @param nodeCode
     * @return
     */
    Long getNodeIdByNodeCode(String nodeCode);

    /**
     * 通过节点编码获取节点信息
     *
     * @param nodeCode
     * @return
     */
    CollectorEtlNodeRespDTO getNodeByNodeCode(String nodeCode);
}
