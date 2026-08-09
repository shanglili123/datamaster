

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskExtPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskExtRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskExtSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据集成任务-扩展数据Service接口
 *
 * @author DATAMASTER
 * @date 2025-04-16
 */
public interface ICollectorEtlTaskExtService extends IService<CollectorEtlTaskExtDO> {

    /**
     * 获得数据集成任务-扩展数据分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成任务-扩展数据分页列表
     */
    PageResult<CollectorEtlTaskExtDO> getCollectorEtlTaskExtPage(CollectorEtlTaskExtPageReqVO pageReqVO);

    /**
     * 创建数据集成任务-扩展数据
     *
     * @param createReqVO 数据集成任务-扩展数据信息
     * @return 数据集成任务-扩展数据编号
     */
    Long createCollectorEtlTaskExt(CollectorEtlTaskExtSaveReqVO createReqVO);

    /**
     * 更新数据集成任务-扩展数据
     *
     * @param updateReqVO 数据集成任务-扩展数据信息
     */
    int updateCollectorEtlTaskExt(CollectorEtlTaskExtSaveReqVO updateReqVO);

    /**
     * 删除数据集成任务-扩展数据
     *
     * @param idList 数据集成任务-扩展数据编号
     */
    int removeCollectorEtlTaskExt(Collection<Long> idList);

    /**
     * 获得数据集成任务-扩展数据详情
     *
     * @param id 数据集成任务-扩展数据编号
     * @return 数据集成任务-扩展数据
     */
    CollectorEtlTaskExtDO getCollectorEtlTaskExtById(Long id);

    /**
     * 获得全部数据集成任务-扩展数据列表
     *
     * @return 数据集成任务-扩展数据列表
     */
    List<CollectorEtlTaskExtDO> getCollectorEtlTaskExtList();

    /**
     * 获得全部数据集成任务-扩展数据 Map
     *
     * @return 数据集成任务-扩展数据 Map
     */
    Map<Long, CollectorEtlTaskExtDO> getCollectorEtlTaskExtMap();


    /**
     * 导入数据集成任务-扩展数据数据
     *
     * @param importExcelList 数据集成任务-扩展数据数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCollectorEtlTaskExt(List<CollectorEtlTaskExtRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 根据任务id获取信息
     *
     * @param taskId
     * @return
     */
    CollectorEtlTaskExtDO getByTaskId(Long taskId);

}
