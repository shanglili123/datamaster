package com.datamaster.module.catalog.service.metadata;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnPageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 元数据字段信息Service接口
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
public interface ICatalogColumnService extends IService<CatalogColumnDO> {

    /**
     * 获得元数据字段信息分页列表
     *
     * @param pageReqVO 分页请求
     * @return 元数据字段信息分页列表
     */
    PageResult<CatalogColumnDO> getCatalogColumnPage(CatalogColumnPageReqVO pageReqVO);

    /**
     * 创建元数据字段信息
     *
     * @param createReqVO 元数据字段信息信息
     * @return 元数据字段信息编号
     */
    Long createCatalogColumn(CatalogColumnSaveReqVO createReqVO);

    List<CatalogColumnDO>  createCatalogColumnList(List<CatalogColumnSaveReqVO> createReqVO);

    /**
     * 更新元数据字段信息
     *
     * @param updateReqVO 元数据字段信息信息
     */
    int updateCatalogColumn(CatalogColumnSaveReqVO updateReqVO);

    /**
     * 删除元数据字段信息
     *
     * @param idList 元数据字段信息编号
     */
    int removeCatalogColumn(Collection<Long> idList);
    int removeCatalogColumn(CatalogColumnRespVO CatalogColumnRespVO);

    /**
     * 获得元数据字段信息详情
     *
     * @param id 元数据字段信息编号
     * @return 元数据字段信息
     */
    CatalogColumnRespVO getCatalogColumnById(Long id);

    /**
     * 获得全部元数据字段信息列表
     *
     * @return 元数据字段信息列表
     */
    List<CatalogColumnDO> getCatalogColumnList();
    List<CatalogColumnRespVO> getCatalogColumnList(CatalogColumnRespVO CatalogColumnRespVO);

    /**
     * 获得全部元数据字段信息 Map
     *
     * @return 元数据字段信息 Map
     */
    Map<Long, CatalogColumnDO> getCatalogColumnMap();


    List<CatalogColumnDO> getMdColumnList(CatalogColumnPageReqVO mdColumn);

    Integer createMdColumn(List<CatalogColumnSaveReqVO> mdColumn);

    Integer saveDraft(List<CatalogColumnSaveReqVO> saveReqVO);

    Integer toggle(Long id, String status);

    BatchDeleteCheck<Long> batchDeleteCheck(List<Long> list);
}
