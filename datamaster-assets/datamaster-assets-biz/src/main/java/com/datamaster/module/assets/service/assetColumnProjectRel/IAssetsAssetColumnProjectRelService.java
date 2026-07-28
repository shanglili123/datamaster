package com.datamaster.module.assets.service.assetColumnProjectRel;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo.AssetsAssetColumnProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo.AssetsAssetColumnProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumnProjectRel.AssetsAssetColumnProjectRelDO;

import java.util.Collection;
import java.util.List;

/**
 * 数据资产字段与空间关联关系 Service
 *
 * @author DATAMASTER
 */
public interface IAssetsAssetColumnProjectRelService extends IService<AssetsAssetColumnProjectRelDO> {

    PageResult<AssetsAssetColumnProjectRelDO> getAssetColumnProjectRelPage(AssetsAssetColumnProjectRelPageReqVO pageReqVO);

    Long createAssetColumnProjectRel(AssetsAssetColumnProjectRelSaveReqVO createReqVO);

    int updateAssetColumnProjectRel(AssetsAssetColumnProjectRelSaveReqVO updateReqVO);

    int removeAssetColumnProjectRel(Collection<Long> idList);

    int removeProjectRelByColumnId(Long columnId);

    AssetsAssetColumnProjectRelDO getAssetColumnProjectRelById(Long id);

    List<AssetsAssetColumnProjectRelDO> getAssetColumnProjectRelList();
}
