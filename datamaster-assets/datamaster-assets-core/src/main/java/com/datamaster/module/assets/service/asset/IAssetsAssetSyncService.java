package com.datamaster.module.assets.service.asset;

import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSyncReqVO;

/**
 * 资产元数据同步 Service 接口
 *
 * 同步定位：将探查结果（AST_DISCOVERY_TABLE / AST_DISCOVERY_COLUMN）同步到资产（AST_ASSET / AST_ASSET_COLUMN），
 * 用于生成缺失资产 + 更新已有资产，不自行实时取数。
 */
public interface IAssetsAssetSyncService {

    /**
     * 按条件同步。支持 datasourceId / taskId / assetId 任一维度，全部为空则全量同步。
     *
     * @param reqVO 同步条件
     * @return 同步结果摘要
     */
    AjaxResult sync(AssetsAssetSyncReqVO reqVO);

    /**
     * 定时同步任务：扫描全部探查任务，对目录编码（catCode）非空的任务执行同步。
     */
    void syncByScheduled();

}
