

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
 * 数据质量日志Service接口
 *
 * @author lili.shang
 * @date 2025-07-19
 */
public interface ICollectorQualityLogService extends IService<CollectorQualityLogDO> {

    /**
     * 获得数据质量日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据质量日志分页列表
     */
    PageResult<CollectorQualityLogDO> getCollectorQualityLogPage(CollectorQualityLogPageReqVO pageReqVO);

    /**
     * 创建数据质量日志
     *
     * @param createReqVO 数据质量日志信息
     * @return 数据质量日志编号
     */
    Long createCollectorQualityLog(CollectorQualityLogSaveReqVO createReqVO);

    /**
     * 更新数据质量日志
     *
     * @param updateReqVO 数据质量日志信息
     */
    int updateCollectorQualityLog(CollectorQualityLogSaveReqVO updateReqVO);

    /**
     * 删除数据质量日志
     *
     * @param idList 数据质量日志编号
     */
    int removeCollectorQualityLog(Collection<Long> idList);

    /**
     * 获得数据质量日志详情
     *
     * @param id 数据质量日志编号
     * @return 数据质量日志
     */
    CollectorQualityLogDO getCollectorQualityLogById(Long id);
    CollectorQualityLogDO selectPrevLogByIdWithWrapper(Long id);

    /**
     * 获得数据质量日志详情
     * 资产专用
     * @return 数据质量日志
     */
    CollectorQualityLogDO getCollectorQualityLogById(CollectorQualityTaskAssetReqVO CollectorQualityTaskAssetReqVO);

    /**
     * 获得全部数据质量日志列表
     *
     * @return 数据质量日志列表
     */
    List<CollectorQualityLogDO> getCollectorQualityLogList();

    /**
     * 获得全部数据质量日志 Map
     *
     * @return 数据质量日志 Map
     */
    Map<Long, CollectorQualityLogDO> getCollectorQualityLogMap();


    /**
     * 导入数据质量日志数据
     *
     * @param importExcelList 数据质量日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorQualityLog(List<CollectorQualityLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 发送数据质量日志的消息
     * @param id
     */
    void sendMessage(Long id);

}
