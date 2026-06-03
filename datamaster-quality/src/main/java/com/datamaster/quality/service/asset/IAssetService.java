

package com.datamaster.quality.service.asset;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetRespDTO;
import com.datamaster.quality.dal.dataobject.asset.AssetDO;

/**
 * 数据资产Service接口
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface IAssetService extends IService<AssetDO> {

    public AssetsAssetRespDTO insertDaAsset(AssetsAssetReqDTO assetReqDTO);

    public Long getCountByCatCode(String catCode);

    public PageResult<AssetsAssetRespDTO> daAssetListPage(AssetsAssetReqDTO assetReqDTO);
}
