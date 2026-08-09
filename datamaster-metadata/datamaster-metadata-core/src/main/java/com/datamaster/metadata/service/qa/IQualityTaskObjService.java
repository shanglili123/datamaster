

package com.datamaster.metadata.service.qa;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjSaveReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjPageReqVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskObjDO;
/**
 * 质量探查任务-稽查对象Service接口
 *
 * @author Chaos
 * @date 2025-07-21
 */
public interface IQualityTaskObjService extends IService<QualityTaskObjDO> {

    /**
     * 获得质量探查任务-稽查对象分页列表
     *
     * @param pageReqVO 分页请求
     * @return 质量探查任务-稽查对象分页列表
     */
    PageResult<QualityTaskObjDO> getQualityTaskObjPage(QualityTaskObjPageReqVO pageReqVO);

    /**
     * 创建质量探查任务-稽查对象
     *
     * @param createReqVO 质量探查任务-稽查对象信息
     * @return 质量探查任务-稽查对象编号
     */
    Long createQualityTaskObj(QualityTaskObjSaveReqVO createReqVO);

    /**
     * 更新质量探查任务-稽查对象
     *
     * @param updateReqVO 质量探查任务-稽查对象信息
     */
    int updateQualityTaskObj(QualityTaskObjSaveReqVO updateReqVO);

    /**
     * 删除质量探查任务-稽查对象
     *
     * @param idList 质量探查任务-稽查对象编号
     */
    int removeQualityTaskObj(Collection<Long> idList);

    /**
     * 获得质量探查任务-稽查对象详情
     *
     * @param id 质量探查任务-稽查对象编号
     * @return 质量探查任务-稽查对象
     */
    QualityTaskObjDO getQualityTaskObjById(Long id);

    /**
     * 获得全部质量探查任务-稽查对象列表
     *
     * @return 质量探查任务-稽查对象列表
     */
    List<QualityTaskObjDO> getQualityTaskObjList();
    List<QualityTaskObjDO> getQualityTaskObjList(QualityTaskObjPageReqVO pageReqVO);

    /**
     * 获得全部质量探查任务-稽查对象 Map
     *
     * @return 质量探查任务-稽查对象 Map
     */
    Map<Long, QualityTaskObjDO> getQualityTaskObjMap();


    /**
     * 导入质量探查任务-稽查对象数据
     *
     * @param importExcelList 质量探查任务-稽查对象数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importQualityTaskObj(List<QualityTaskObjRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
