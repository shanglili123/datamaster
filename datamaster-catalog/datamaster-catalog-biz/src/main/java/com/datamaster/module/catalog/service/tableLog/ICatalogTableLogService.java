package com.datamaster.module.catalog.service.tableLog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableSaveReqVO;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.tableLog.CatalogTableLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 元数据信息 - 日志Service接口
 *
 * @author qdata
 * @date 2026-03-10
 */
public interface ICatalogTableLogService extends IService<CatalogTableLogDO> {

    /**
     * 获得元数据信息 - 日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 元数据信息 - 日志分页列表
     */
    PageResult<CatalogTableLogDO> getCatalogTableLogPage(CatalogTableLogPageReqVO pageReqVO);

    /**
     * 创建元数据信息 - 日志
     *
     * @param createReqVO 元数据信息 - 日志信息
     * @return 元数据信息 - 日志编号
     */
    Long createCatalogTableLog(CatalogTableLogSaveReqVO createReqVO);

    /**
     * 更新元数据信息 - 日志
     *
     * @param updateReqVO 元数据信息 - 日志信息
     */
    int updateCatalogTableLog(CatalogTableLogSaveReqVO updateReqVO);

    /**
     * 删除元数据信息 - 日志
     *
     * @param idList 元数据信息 - 日志编号
     */
    int removeCatalogTableLog(Collection<Long> idList);

    /**
     * 获得元数据信息 - 日志详情
     *
     * @param id 元数据信息 - 日志编号
     * @return 元数据信息 - 日志
     */
    CatalogTableLogDO getCatalogTableLogById(Long id);

    /**
     * 获得全部元数据信息 - 日志列表
     *
     * @return 元数据信息 - 日志列表
     */
    List<CatalogTableLogDO> getCatalogTableLogList();

    /**
     * 获得全部元数据信息 - 日志 Map
     *
     * @return 元数据信息 - 日志 Map
     */
    Map<Long, CatalogTableLogDO> getCatalogTableLogMap();

    /**
     * 根据元数据表信息添加元数据版本变更日志
     * @param table 元数据表信息
     * @return 元数据版本变更日志id
     */
    Long createCatalogTableLog(CatalogTableSaveReqVO table);
}
