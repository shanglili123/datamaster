package com.datamaster.module.assets.service.asset;

import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSyncReqVO;

/**
 * 资产元数据同步 Service 接口
 *
 * 同步定位：将目录元数据（CAT_TABLE / CAT_COLUMN）同步到资产（AST_ASSET / AST_ASSET_COLUMN），
 * 用于生成缺失资产 + 更新已有资产，不自行实时取数。
 */
public interface IAssetsAssetSyncService {

    /**
     * 按元数据库或单个资产同步。
     *
     * @param reqVO 同步条件
     * @return 同步结果摘要
     */
    AjaxResult sync(AssetsAssetSyncReqVO reqVO);

}
