

package com.datamaster.module.collector.service.etl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogSaveReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEvaluateLogStatisticsVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CheckErrorDataReqDTO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEvaluateLogDO;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * 评测规则结果Service接口
 *
 * @author lili.shang
 * @date 2025-07-21
 */
public interface ICollectorEvaluateLogService extends IService<CollectorEvaluateLogDO> {

    /**
     * 获得评测规则结果分页列表
     *
     * @param pageReqVO 分页请求
     * @return 评测规则结果分页列表
     */
    PageResult<CollectorEvaluateLogDO> getCollectorEvaluateLogPage(CollectorEvaluateLogPageReqVO pageReqVO);

    /**
     * 创建评测规则结果
     *
     * @param createReqVO 评测规则结果信息
     * @return 评测规则结果编号
     */
    Long createCollectorEvaluateLog(CollectorEvaluateLogSaveReqVO createReqVO);

    /**
     * 更新评测规则结果
     *
     * @param updateReqVO 评测规则结果信息
     */
    int updateCollectorEvaluateLog(CollectorEvaluateLogSaveReqVO updateReqVO);

    /**
     * 删除评测规则结果
     *
     * @param idList 评测规则结果编号
     */
    int removeCollectorEvaluateLog(Collection<Long> idList);

    /**
     * 获得评测规则结果详情
     *
     * @param id 评测规则结果编号
     * @return 评测规则结果
     */
    CollectorEvaluateLogDO getCollectorEvaluateLogById(Long id);

    /**
     * 获得全部评测规则结果列表
     *
     * @return 评测规则结果列表
     */
    List<CollectorEvaluateLogDO> getCollectorEvaluateLogList();

    /**
     * 获得全部评测规则结果 Map
     *
     * @return 评测规则结果 Map
     */
    Map<Long, CollectorEvaluateLogDO> getCollectorEvaluateLogMap();

    Map<String, Object> sumTotalAndProblemTotalByTaskLogId(String taskLogId);


    /**
     * 导入评测规则结果数据
     *
     * @param importExcelList 评测规则结果数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorEvaluateLog(List<CollectorEvaluateLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

    List<CollectorEvaluateLogStatisticsVO> statisticsEvaluateOne(Long id);

    JSONObject statisticsEvaluateTow(Long id , Date deDate , Date oldDate , int type);

    List<CollectorEvaluateLogRespVO> statisticsEvaluateTable(Long id);

    JSONObject pageErrorData(CheckErrorDataReqDTO checkErrorDataReqDTO);

    boolean updateErrorData(CheckErrorDataReqDTO checkErrorDataReqDTO);
}
