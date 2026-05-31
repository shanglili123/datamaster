

package com.datamaster.quality.service.asset.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetRespDTO;
import com.datamaster.quality.dal.dataobject.asset.DaAssetDO;
import com.datamaster.quality.dal.mapper.asset.DaAssetMapper;
import com.datamaster.quality.service.asset.IDaAssetService;

/**
 * 数据资产Service业务层处理
 *
 * @author lhs
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class DaAssetServiceImpl extends ServiceImpl<DaAssetMapper, DaAssetDO> implements IDaAssetService {

    @Override
    public AssetsAssetRespDTO insertDaAsset(AssetsAssetReqDTO daAssetReqDTO) {
        return null;
    }

    @Override
    public Long getCountByCatCode(String catCode) {
        return null;
    }

    @Override
    public PageResult<AssetsAssetRespDTO> daAssetListPage(AssetsAssetReqDTO daAssetReqDTO) {
        return null;
    }
}
