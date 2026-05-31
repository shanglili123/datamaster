package com.datamaster.module.catalog.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopePageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopeRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopeSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskScopeDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采集范围Service接口
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
public interface ICatalogTaskScopeService extends IService<CatalogTaskScopeDO> {

    /**
     * 获得采集范围分页列表
     *
     * @param pageReqVO 分页请求
     * @return 采集范围分页列表
     */
    PageResult<CatalogTaskScopeDO> getCatalogTaskScopePage(CatalogTaskScopePageReqVO pageReqVO);

    /**
     * 创建采集范围
     *
     * @param createReqVO 采集范围信息
     * @return 采集范围编号
     */
    Long createCatalogTaskScope(CatalogTaskScopeSaveReqVO createReqVO);

    /**
     * 更新采集范围
     *
     * @param updateReqVO 采集范围信息
     */
    int updateCatalogTaskScope(CatalogTaskScopeSaveReqVO updateReqVO);

    /**
     * 删除采集范围
     *
     * @param idList 采集范围编号
     */
    int removeCatalogTaskScope(Collection<Long> idList);

    /**
     * 获得采集范围详情
     *
     * @param id 采集范围编号
     * @return 采集范围
     */
    CatalogTaskScopeDO getCatalogTaskScopeById(Long id);

    /**
     * 获得全部采集范围列表
     *
     * @return 采集范围列表
     */
    List<CatalogTaskScopeDO> getCatalogTaskScopeList();

    List<CatalogTaskScopeDO> getCatalogTaskScopeListBytaskId(Long taskId);

    /**
     * 获得全部采集范围 Map
     *
     * @return 采集范围 Map
     */
    Map<Long, CatalogTaskScopeDO> getCatalogTaskScopeMap();


    /**
     * 导入采集范围数据
     *
     * @param importExcelList 采集范围数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importCatalogTaskScope(List<CatalogTaskScopeRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void removeCatalogTaskScopeBytaskId(Long taskId);
}
