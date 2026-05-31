package com.datamaster.module.catalog.service.metadata;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbPageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbSaveReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogMetaSearchRespDTO;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据库Service接口
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
public interface ICatalogDbService extends IService<CatalogDbDO> {

    /**
     * 获得数据库分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据库分页列表
     */
    PageResult<CatalogDbDO> getCatalogDbPage(CatalogDbPageReqVO pageReqVO);

    /**
     * 创建数据库
     *
     * @param createReqVO 数据库信息
     * @return 数据库编号
     */
    Long createCatalogDb(CatalogDbSaveReqVO createReqVO);

    /**
     * 更新数据库
     *
     * @param updateReqVO 数据库信息
     */
    int updateCatalogDb(CatalogDbSaveReqVO updateReqVO);

    /**
     * 删除数据库
     *
     * @param idList 数据库编号
     */
    int removeCatalogDb(Collection<Long> idList);

    /**
     * 获得数据库详情
     *
     * @param id 数据库编号
     * @return 数据库
     */
    CatalogDbRespVO getCatalogDbById(Long id);

    /**
     * 获得全部数据库列表
     *
     * @return 数据库列表
     */
    List<CatalogDbDO> getCatalogDbList(CatalogDbPageReqVO CatalogDb);

    /**
     * 获得全部数据库 Map
     *
     * @return 数据库 Map
     */
    Map<Long, CatalogDbDO> getCatalogDbMap();


    Integer toggle(Long id, String status);

    Integer editPortalVisible(Long id, String portalVisible);
    List<CatalogDbRespVO> getCatalogDbByTaskId(Long taskId);

    BatchDeleteCheck<Long> batchDeleteCheck(List<Long> list);

    PageResult<CatalogMetaSearchRespDTO> selectMetaSearchPage(CatalogMetaSearchRespDTO mdMetaSearchRespDTO);
}
