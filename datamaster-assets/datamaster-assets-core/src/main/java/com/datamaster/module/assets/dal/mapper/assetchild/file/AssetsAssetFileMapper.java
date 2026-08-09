package com.datamaster.module.assets.dal.mapper.assetchild.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datamaster.module.assets.dal.dataobject.assetchild.file.AssetsAssetFileDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

public interface AssetsAssetFileMapper extends BaseMapperX<AssetsAssetFileDO> {

    default AssetsAssetFileDO selectByAssetId(Long assetId) {
        return selectOne(new LambdaQueryWrapper<AssetsAssetFileDO>().eq(AssetsAssetFileDO::getAssetId, assetId).last("limit 1"));
    }

}
