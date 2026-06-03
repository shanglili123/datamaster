

package com.datamaster.quality.service.qa;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.quality.controller.qa.vo.QualityTaskObjPageReqVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskObjRespVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskObjSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskObjDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据质量任务-稽查对象Service接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface IQualityTaskObjService extends IService<QualityTaskObjDO> {

    /**
     * 获得数据质量任务-稽查对象分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据质量任务-稽查对象分页列表
     */
    PageResult<QualityTaskObjDO> getQualityTaskObjPage(QualityTaskObjPageReqVO pageReqVO);

    /**
     * 创建数据质量任务-稽查对象
     *
     * @param createReqVO 数据质量任务-稽查对象信息
     * @return 数据质量任务-稽查对象编号
     */
    Long createQualityTaskObj(QualityTaskObjSaveReqVO createReqVO);

    /**
     * 更新数据质量任务-稽查对象
     *
     * @param updateReqVO 数据质量任务-稽查对象信息
     */
    int updateQualityTaskObj(QualityTaskObjSaveReqVO updateReqVO);

    /**
     * 删除数据质量任务-稽查对象
     *
     * @param idList 数据质量任务-稽查对象编号
     */
    int removeQualityTaskObj(Collection<Long> idList);

    /**
     * 获得数据质量任务-稽查对象详情
     *
     * @param id 数据质量任务-稽查对象编号
     * @return 数据质量任务-稽查对象
     */
    QualityTaskObjDO getQualityTaskObjById(Long id);

    List<QualityTaskObjDO> getQualityTaskObjList(String taskId);

    /**
     * 获得全部数据质量任务-稽查对象列表
     *
     * @return 数据质量任务-稽查对象列表
     */
    List<QualityTaskObjDO> getQualityTaskObjList();

    /**
     * 获得全部数据质量任务-稽查对象 Map
     *
     * @return 数据质量任务-稽查对象 Map
     */
    Map<Long, QualityTaskObjDO> getQualityTaskObjMap();


    /**
     * 导入数据质量任务-稽查对象数据
     *
     * @param importExcelList 数据质量任务-稽查对象数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityTaskObj(List<QualityTaskObjRespVO> importExcelList, boolean isUpdateSupport, String operName);
}
