package com.datamaster.module.assets.service.assetColumnSpaceRel;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumnSpaceRel.AssetsAssetColumnSpaceRelDO;

import java.util.Collection;
import java.util.List;

/**
 * 数据资产字段与空间关联关系 Service
 *
 * @author DATAMASTER
 */
public interface IAssetsAssetColumnSpaceRelService extends IService<AssetsAssetColumnSpaceRelDO> {

    PageResult<AssetsAssetColumnSpaceRelDO> getAssetColumnSpaceRelPage(AssetsAssetColumnSpaceRelPageReqVO pageReqVO);

    Long createAssetColumnSpaceRel(AssetsAssetColumnSpaceRelSaveReqVO createReqVO);

    int updateAssetColumnSpaceRel(AssetsAssetColumnSpaceRelSaveReqVO updateReqVO);

    int removeAssetColumnSpaceRel(Collection<Long> idList);

    int removeSpaceRelByColumnId(Long columnId);

    AssetsAssetColumnSpaceRelDO getAssetColumnSpaceRelById(Long id);

    List<AssetsAssetColumnSpaceRelDO> getAssetColumnSpaceRelList();
}
