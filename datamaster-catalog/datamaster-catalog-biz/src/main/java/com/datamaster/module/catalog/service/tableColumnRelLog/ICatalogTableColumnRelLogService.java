package com.datamaster.module.catalog.service.tableColumnRelLog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.tableColumnRelLog.CatalogTableColumnRelLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 元数据数据库与信息及字段信息关系-日志Service接口
 *
 * @author qdata
 * @date 2026-03-10
 */
public interface ICatalogTableColumnRelLogService extends IService<CatalogTableColumnRelLogDO> {

    /**
     * 获得元数据数据库与信息及字段信息关系-日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return 元数据数据库与信息及字段信息关系-日志分页列表
     */
    PageResult<CatalogTableColumnRelLogDO> getCatalogTableColumnRelLogPage(CatalogTableColumnRelLogPageReqVO pageReqVO);

    /**
     * 创建元数据数据库与信息及字段信息关系-日志
     *
     * @param createReqVO 元数据数据库与信息及字段信息关系-日志信息
     * @return 元数据数据库与信息及字段信息关系-日志编号
     */
    Long createCatalogTableColumnRelLog(CatalogTableColumnRelLogSaveReqVO createReqVO);

    /**
     * 更新元数据数据库与信息及字段信息关系-日志
     *
     * @param updateReqVO 元数据数据库与信息及字段信息关系-日志信息
     */
    int updateCatalogTableColumnRelLog(CatalogTableColumnRelLogSaveReqVO updateReqVO);

    /**
     * 删除元数据数据库与信息及字段信息关系-日志
     *
     * @param idList 元数据数据库与信息及字段信息关系-日志编号
     */
    int removeCatalogTableColumnRelLog(Collection<Long> idList);

    /**
     * 获得元数据数据库与信息及字段信息关系-日志详情
     *
     * @param id 元数据数据库与信息及字段信息关系-日志编号
     * @return 元数据数据库与信息及字段信息关系-日志
     */
    CatalogTableColumnRelLogDO getCatalogTableColumnRelLogById(Long id);

    /**
     * 获得全部元数据数据库与信息及字段信息关系-日志列表
     *
     * @return 元数据数据库与信息及字段信息关系-日志列表
     */
    List<CatalogTableColumnRelLogDO> getCatalogTableColumnRelLogList();

    /**
     * 获得全部元数据数据库与信息及字段信息关系-日志 Map
     *
     * @return 元数据数据库与信息及字段信息关系-日志 Map
     */
    Map<Long, CatalogTableColumnRelLogDO> getCatalogTableColumnRelLogMap();


}
