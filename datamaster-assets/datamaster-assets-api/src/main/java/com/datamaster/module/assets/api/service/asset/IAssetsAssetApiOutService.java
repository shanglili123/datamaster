package com.datamaster.module.assets.api.service.asset;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetRespDTO;
import com.datamaster.module.catalog.api.column.dto.CatalogColumnRespDTO;

import java.util.List;
import java.util.Map;

/**
 * 数据资产Service接口 * * @author lhs * @date 2025-01-21
 */
public interface IAssetsAssetApiOutService {
    public AssetsAssetRespDTO insertAsset(AssetsAssetReqDTO AssetsAssetReqDTO);

    /**
     * 根据类目编码查询数量     *     * @return
     */
    Long getCountByCatCode(String catCode);

    /**
     * 获取资产集合分页
     */
    PageResult<AssetsAssetRespDTO> AssetsAssetListPage(AssetsAssetReqDTO AssetsAssetReqDTO);

    Map<String, Object> getAssetOverviewStatistics();

    /**
     * 将老的 CAT_CODE 批量更新成新的 CAT_CODE     *     * @param oldCatCode 旧分类编码     * @param newCatCode 新分类编码     * @return 受影响行数
     */
    int updateCatCode(String oldCatCode, String newCatCode);

    /**
     * 根据catalog表id列表，获取在资产中存在的catalog表id列表     * @param catalogTableIds     * @return
     */
    List<Long> getCatalogTableInAsset(List<Long> catalogTableIds);

    /**
     * 将mc字段批量更新到到资产中     * @param columnMap
     */
    void catalogTableColumnUpdateToAssetColumn(Map<Long, List<CatalogColumnRespDTO>> columnMap);

    /**
     * 检查是否有资产使用了指定的元数据表ID     *     * @param tableId 元数据表ID     * @return 是否存在使用该表的资产
     */
    boolean existsByTableId(Long tableId);
}