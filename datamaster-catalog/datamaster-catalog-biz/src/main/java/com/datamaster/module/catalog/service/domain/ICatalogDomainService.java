package com.datamaster.module.catalog.service.domain;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.catalog.controller.admin.domain.vo.CatalogDomainPageReqVO;
import com.datamaster.module.catalog.controller.admin.domain.vo.CatalogDomainSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.domain.CatalogDomainDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务域Service接口
 *
 * @author DATAMASTER
 * @date 2026-02-12
 */
public interface ICatalogDomainService extends IService<CatalogDomainDO> {

    /**
     * 获得业务域分页列表
     *
     * @param pageReqVO 分页请求
     * @return 业务域分页列表
     */
    PageResult<CatalogDomainDO> getCatalogDomainPage(CatalogDomainPageReqVO pageReqVO);

    /**
     * 创建业务域
     *
     * @param createReqVO 业务域信息
     * @return 业务域编号
     */
    Long createCatalogDomain(CatalogDomainSaveReqVO createReqVO);

    /**
     * 更新业务域
     *
     * @param updateReqVO 业务域信息
     */
    int updateCatalogDomain(CatalogDomainSaveReqVO updateReqVO);

    /**
     * 删除业务域
     *
     * @param idList 业务域编号
     */
    int removeCatalogDomain(Collection<Long> idList);

    /**
     * 获得业务域详情
     *
     * @param id 业务域编号
     * @return 业务域
     */
    CatalogDomainDO getCatalogDomainById(Long id);

    /**
     * 获得全部业务域列表
     *
     * @return 业务域列表
     */
    List<CatalogDomainDO> getCatalogDomainList(CatalogDomainPageReqVO CatalogDomain);

    /**
     * 获得全部业务域 Map
     *
     * @return 业务域 Map
     */
    Map<Long, CatalogDomainDO> getCatalogDomainMap();

    /**
     * 批量删除检查,查询可删除数和不可删除数
     * @param ids
     * @return
     */
    BatchDeleteCheck<Long> batchDeleteCheck(List<Long> ids);
}
