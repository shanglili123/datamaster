

package com.datamaster.module.collector.service.qa;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjSaveReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjPageReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskObjDO;
/**
 * 数据质量任务-稽查对象Service接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface ICollectorQualityTaskObjService extends IService<CollectorQualityTaskObjDO> {

    /**
     * 获得数据质量任务-稽查对象分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据质量任务-稽查对象分页列表
     */
    PageResult<CollectorQualityTaskObjDO> getCollectorQualityTaskObjPage(CollectorQualityTaskObjPageReqVO pageReqVO);

    /**
     * 创建数据质量任务-稽查对象
     *
     * @param createReqVO 数据质量任务-稽查对象信息
     * @return 数据质量任务-稽查对象编号
     */
    Long createCollectorQualityTaskObj(CollectorQualityTaskObjSaveReqVO createReqVO);

    /**
     * 更新数据质量任务-稽查对象
     *
     * @param updateReqVO 数据质量任务-稽查对象信息
     */
    int updateCollectorQualityTaskObj(CollectorQualityTaskObjSaveReqVO updateReqVO);

    /**
     * 删除数据质量任务-稽查对象
     *
     * @param idList 数据质量任务-稽查对象编号
     */
    int removeCollectorQualityTaskObj(Collection<Long> idList);

    /**
     * 获得数据质量任务-稽查对象详情
     *
     * @param id 数据质量任务-稽查对象编号
     * @return 数据质量任务-稽查对象
     */
    CollectorQualityTaskObjDO getCollectorQualityTaskObjById(Long id);

    /**
     * 获得全部数据质量任务-稽查对象列表
     *
     * @return 数据质量任务-稽查对象列表
     */
    List<CollectorQualityTaskObjDO> getCollectorQualityTaskObjList();
    List<CollectorQualityTaskObjDO> getCollectorQualityTaskObjList(CollectorQualityTaskObjPageReqVO pageReqVO);

    /**
     * 获得全部数据质量任务-稽查对象 Map
     *
     * @return 数据质量任务-稽查对象 Map
     */
    Map<Long, CollectorQualityTaskObjDO> getCollectorQualityTaskObjMap();


    /**
     * 导入数据质量任务-稽查对象数据
     *
     * @param importExcelList 数据质量任务-稽查对象数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorQualityTaskObj(List<CollectorQualityTaskObjRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
