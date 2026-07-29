

package com.datamaster.quality.service.qa;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.quality.controller.qa.vo.QualityLogPageReqVO;
import com.datamaster.quality.controller.qa.vo.QualityLogRespVO;
import com.datamaster.quality.controller.qa.vo.QualityLogSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 质量探查日志Service接口
 *
 * @author lili.shang
 * @date 2025-07-19
 */
public interface IQualityLogService extends IService<QualityLogDO> {

    /**
     * 获得质量探查日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 质量探查日志分页列表
     */
    PageResult<QualityLogDO> getQualityLogPage(QualityLogPageReqVO pageReqVO);

    /**
     * 创建质量探查日志
     *
     * @param createReqVO 质量探查日志信息
     * @return 质量探查日志编号
     */
    Long createQualityLog(QualityLogSaveReqVO createReqVO);

    /**
     * 更新质量探查日志
     *
     * @param updateReqVO 质量探查日志信息
     */
    int updateQualityLog(QualityLogSaveReqVO updateReqVO);

    /**
     * 删除质量探查日志
     *
     * @param idList 质量探查日志编号
     */
    int removeQualityLog(Collection<Long> idList);

    /**
     * 获得质量探查日志详情
     *
     * @param id 质量探查日志编号
     * @return 质量探查日志
     */
    QualityLogDO getQualityLogById(Long id);

    /**
     * 获得全部质量探查日志列表
     *
     * @return 质量探查日志列表
     */
    List<QualityLogDO> getQualityLogList();

    /**
     * 获得全部质量探查日志 Map
     *
     * @return 质量探查日志 Map
     */
    Map<Long, QualityLogDO> getQualityLogMap();


    /**
     * 导入质量探查日志数据
     *
     * @param importExcelList 质量探查日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityLog(List<QualityLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
