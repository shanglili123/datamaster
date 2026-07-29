

package com.datamaster.module.collector.service.etl;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogSaveReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogPageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskAssetReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorQualityLogDO;
/**
 * 质量探查日志Service接口
 *
 * @author lili.shang
 * @date 2025-07-19
 */
public interface ICollectorQualityLogService extends IService<CollectorQualityLogDO> {

    /**
     * 获得质量探查日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 质量探查日志分页列表
     */
    PageResult<CollectorQualityLogDO> getCollectorQualityLogPage(CollectorQualityLogPageReqVO pageReqVO);

    /**
     * 创建质量探查日志
     *
     * @param createReqVO 质量探查日志信息
     * @return 质量探查日志编号
     */
    Long createCollectorQualityLog(CollectorQualityLogSaveReqVO createReqVO);

    /**
     * 更新质量探查日志
     *
     * @param updateReqVO 质量探查日志信息
     */
    int updateCollectorQualityLog(CollectorQualityLogSaveReqVO updateReqVO);

    /**
     * 删除质量探查日志
     *
     * @param idList 质量探查日志编号
     */
    int removeCollectorQualityLog(Collection<Long> idList);

    /**
     * 获得质量探查日志详情
     *
     * @param id 质量探查日志编号
     * @return 质量探查日志
     */
    CollectorQualityLogDO getCollectorQualityLogById(Long id);
    CollectorQualityLogDO selectPrevLogByIdWithWrapper(Long id);

    /**
     * 获得质量探查日志详情
     * 资产专用
     * @return 质量探查日志
     */
    CollectorQualityLogDO getCollectorQualityLogById(CollectorQualityTaskAssetReqVO CollectorQualityTaskAssetReqVO);

    /**
     * 获得全部质量探查日志列表
     *
     * @return 质量探查日志列表
     */
    List<CollectorQualityLogDO> getCollectorQualityLogList();

    /**
     * 获得全部质量探查日志 Map
     *
     * @return 质量探查日志 Map
     */
    Map<Long, CollectorQualityLogDO> getCollectorQualityLogMap();


    /**
     * 导入质量探查日志数据
     *
     * @param importExcelList 质量探查日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorQualityLog(List<CollectorQualityLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 发送质量探查日志的消息
     * @param id
     */
    void sendMessage(Long id);

}
