

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
 * 数据质量日志Service接口
 *
 * @author lili.shang
 * @date 2025-07-19
 */
public interface IQualityLogService extends IService<QualityLogDO> {

    /**
     * 获得数据质量日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据质量日志分页列表
     */
    PageResult<QualityLogDO> getQualityLogPage(QualityLogPageReqVO pageReqVO);

    /**
     * 创建数据质量日志
     *
     * @param createReqVO 数据质量日志信息
     * @return 数据质量日志编号
     */
    Long createQualityLog(QualityLogSaveReqVO createReqVO);

    /**
     * 更新数据质量日志
     *
     * @param updateReqVO 数据质量日志信息
     */
    int updateQualityLog(QualityLogSaveReqVO updateReqVO);

    /**
     * 删除数据质量日志
     *
     * @param idList 数据质量日志编号
     */
    int removeQualityLog(Collection<Long> idList);

    /**
     * 获得数据质量日志详情
     *
     * @param id 数据质量日志编号
     * @return 数据质量日志
     */
    QualityLogDO getQualityLogById(Long id);

    /**
     * 获得全部数据质量日志列表
     *
     * @return 数据质量日志列表
     */
    List<QualityLogDO> getQualityLogList();

    /**
     * 获得全部数据质量日志 Map
     *
     * @return 数据质量日志 Map
     */
    Map<Long, QualityLogDO> getQualityLogMap();


    /**
     * 导入数据质量日志数据
     *
     * @param importExcelList 数据质量日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityLog(List<QualityLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
