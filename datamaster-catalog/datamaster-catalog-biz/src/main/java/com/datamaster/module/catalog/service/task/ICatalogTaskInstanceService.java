package com.datamaster.module.catalog.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstancePageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采集任务实例Service接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface ICatalogTaskInstanceService extends IService<CatalogTaskInstanceDO> {

    /**
     * 获得采集任务实例分页列表
     *
     * @param pageReqVO 分页请求
     * @return 采集任务实例分页列表
     */
    PageResult<CatalogTaskInstanceDO> getCatalogTaskInstancePage(CatalogTaskInstancePageReqVO pageReqVO);

    /**
     * 创建采集任务实例
     *
     * @param createReqVO 采集任务实例信息
     * @return 采集任务实例编号
     */
    Long createCatalogTaskInstance(CatalogTaskInstanceSaveReqVO createReqVO);
    Long createCatalogTaskInstance(CatalogTaskInstanceDO createReqVO);

    /**
     * 更新采集任务实例
     *
     * @param updateReqVO 采集任务实例信息
     */
    int updateCatalogTaskInstance(CatalogTaskInstanceSaveReqVO updateReqVO);

    /**
     * 删除采集任务实例
     *
     * @param idList 采集任务实例编号
     */
    int removeCatalogTaskInstance(Collection<Long> idList);

    /**
     * 获得采集任务实例详情
     *
     * @param id 采集任务实例编号
     * @return 采集任务实例
     */
    CatalogTaskInstanceDO getCatalogTaskInstanceById(Long id);

    /**
     * 获得采集任务实例详情
     *
     * @param taskId
     * @return 采集任务实例
     */
    CatalogTaskInstanceDO getCatalogTaskInstanceByTaskId(Long taskId);

    /**
     * 获得全部采集任务实例列表
     *
     * @return 采集任务实例列表
     */
    List<CatalogTaskInstanceDO> getCatalogTaskInstanceList();

    /**
     * 获得全部采集任务实例 Map
     *
     * @return 采集任务实例 Map
     */
    Map<Long, CatalogTaskInstanceDO> getCatalogTaskInstanceMap();


    /**
     * 导入采集任务实例数据
     *
     * @param importExcelList 采集任务实例数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCatalogTaskInstance(List<CatalogTaskInstanceRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
