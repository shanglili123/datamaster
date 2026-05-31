package com.datamaster.module.catalog.service.columnLog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.columnLog.CatalogColumnLogDO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 元数据字段信息 - 日志Service接口
 *
 * @author qdata
 * @date 2026-03-10
 */
public interface ICatalogColumnLogService extends IService<CatalogColumnLogDO> {

    /**
     * 获得元数据字段信息 - 日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 元数据字段信息 - 日志分页列表
     */
    PageResult<CatalogColumnLogDO> getCatalogColumnLogPage(CatalogColumnLogPageReqVO pageReqVO);

    /**
     * 创建元数据字段信息 - 日志
     *
     * @param createReqVO 元数据字段信息 - 日志信息
     * @return 元数据字段信息 - 日志编号
     */
    Long createCatalogColumnLog(CatalogColumnLogSaveReqVO createReqVO);

    /**
     * 批量创建元数据字段信息 - 日志
     *
     * @param columnDOList 元数据字段信息列表
     * @return 元数据字段信息
     */
    Long createCatalogColumnLog(List<CatalogColumnDO> columnDOList);

    /**
     * 更新元数据字段信息 - 日志
     *
     * @param updateReqVO 元数据字段信息 - 日志信息
     */
    int updateCatalogColumnLog(CatalogColumnLogSaveReqVO updateReqVO);

    /**
     * 删除元数据字段信息 - 日志
     *
     * @param idList 元数据字段信息 - 日志编号
     */
    int removeCatalogColumnLog(Collection<Long> idList);

    /**
     * 获得元数据字段信息 - 日志详情
     *
     * @param id 元数据字段信息 - 日志编号
     * @return 元数据字段信息 - 日志
     */
    CatalogColumnLogDO getCatalogColumnLogById(Long id);

    /**
     * 获得全部元数据字段信息 - 日志列表
     *
     * @return 元数据字段信息 - 日志列表
     */
    List<CatalogColumnLogDO> getCatalogColumnLogList();

    /**
     * 获得全部元数据字段信息 - 日志 Map
     *
     * @return 元数据字段信息 - 日志 Map
     */
    Map<Long, CatalogColumnLogDO> getCatalogColumnLogMap();


}
