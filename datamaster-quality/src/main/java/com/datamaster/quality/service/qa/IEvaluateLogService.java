

package com.datamaster.quality.service.qa;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.quality.controller.qa.vo.EvaluateLogPageReqVO;
import com.datamaster.quality.controller.qa.vo.EvaluateLogRespVO;
import com.datamaster.quality.controller.qa.vo.EvaluateLogSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.EvaluateLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 评测规则结果Service接口
 *
 * @author lili.shang
 * @date 2025-07-21
 */
public interface IEvaluateLogService extends IService<EvaluateLogDO> {

    /**
     * 获得评测规则结果分页列表
     *
     * @param pageReqVO 分页请求
     * @return 评测规则结果分页列表
     */
    PageResult<EvaluateLogDO> getEvaluateLogPage(EvaluateLogPageReqVO pageReqVO);

    /**
     * 创建评测规则结果
     *
     * @param createReqVO 评测规则结果信息
     * @return 评测规则结果编号
     */
    Long createEvaluateLog(EvaluateLogSaveReqVO createReqVO);

    /**
     * 更新评测规则结果
     *
     * @param updateReqVO 评测规则结果信息
     */
    int updateEvaluateLog(EvaluateLogSaveReqVO updateReqVO);

    /**
     * 删除评测规则结果
     *
     * @param idList 评测规则结果编号
     */
    int removeEvaluateLog(Collection<Long> idList);

    /**
     * 获得评测规则结果详情
     *
     * @param id 评测规则结果编号
     * @return 评测规则结果
     */
    EvaluateLogDO getEvaluateLogById(Long id);

    /**
     * 获得全部评测规则结果列表
     *
     * @return 评测规则结果列表
     */
    List<EvaluateLogDO> getEvaluateLogList();

    /**
     * 获得全部评测规则结果 Map
     *
     * @return 评测规则结果 Map
     */
    Map<Long, EvaluateLogDO> getEvaluateLogMap();


    /**
     * 导入评测规则结果数据
     *
     * @param importExcelList 评测规则结果数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importEvaluateLog(List<EvaluateLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
