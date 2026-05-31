package com.datamaster.module.catalog.service.metadata;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTablePageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogTableDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 元数据信息Service接口
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
public interface ICatalogTableService extends IService<CatalogTableDO> {

    /**
     * 获得元数据信息分页列表
     *
     * @param pageReqVO 分页请求
     * @return 元数据信息分页列表
     */
    PageResult<CatalogTableDO> getCatalogTablePage(CatalogTablePageReqVO pageReqVO);

    PageResult<CatalogTableRespVO> getCatalogTablePageAsset(CatalogTablePageReqVO CatalogTable);

    List<CatalogTableRespVO> getCatalogTableListAsset(CatalogTablePageReqVO CatalogTable);

    /**
     * 创建元数据信息
     *
     * @param createReqVO 元数据信息信息
     * @return 元数据信息编号
     */
    Long createCatalogTable(CatalogTableSaveReqVO createReqVO);

    /**
     * 更新元数据信息
     *
     * @param updateReqVO 元数据信息信息
     */
    int updateCatalogTable(CatalogTableSaveReqVO updateReqVO);

    /**
     * 删除元数据信息
     *
     * @param idList 元数据信息编号
     */
    int removeCatalogTable(Collection<Long> idList);

    /**
     * 获得元数据信息详情
     *
     * @param id 元数据信息编号
     * @return 元数据信息
     */
    CatalogTableRespVO getCatalogTableById(Long id);
  //  CatalogTableDO getCatalogTableById(Long id);
   List<CatalogTableRespVO> getCatalogTableById(CatalogTableRespVO CatalogTableRespVO);

    /**
     * 获得全部元数据信息列表
     *
     * @return 元数据信息列表
     */
    List<CatalogTableDO> getCatalogTableList();

    /**
     * 获得全部元数据信息 Map
     *
     * @return 元数据信息 Map
     */
    Map<Long, CatalogTableDO> getCatalogTableMap();


    BatchDeleteCheck<Long> batchDeleteCheck(List<Long> list);
    List<CatalogTableRespVO> getCatalogTableByDbId(Collection<Long> idList);

    Long saveDraft(CatalogTableSaveReqVO saveReqVO);


    /**
     * 停启用
     *
     * @param id
     * @param status
     * @return
     */
    Integer toggle(Long id, String status);
}
