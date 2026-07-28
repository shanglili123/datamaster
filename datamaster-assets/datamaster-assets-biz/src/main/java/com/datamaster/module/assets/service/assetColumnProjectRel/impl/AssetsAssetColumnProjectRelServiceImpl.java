package com.datamaster.module.assets.service.assetColumnProjectRel.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo.AssetsAssetColumnProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo.AssetsAssetColumnProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumnProjectRel.AssetsAssetColumnProjectRelDO;
import com.datamaster.module.assets.dal.mapper.assetColumnProjectRel.AssetsAssetColumnProjectRelMapper;
import com.datamaster.module.assets.service.assetColumnProjectRel.IAssetsAssetColumnProjectRelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 数据资产字段与空间关联关系 Service 实现
 *
 * @author DATAMASTER
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetColumnProjectRelServiceImpl extends ServiceImpl<AssetsAssetColumnProjectRelMapper, AssetsAssetColumnProjectRelDO> implements IAssetsAssetColumnProjectRelService {
    @Resource
    private AssetsAssetColumnProjectRelMapper assetsAssetColumnProjectRelMapper;

    @Override
    public PageResult<AssetsAssetColumnProjectRelDO> getAssetColumnProjectRelPage(AssetsAssetColumnProjectRelPageReqVO pageReqVO) {
        return assetsAssetColumnProjectRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAssetColumnProjectRel(AssetsAssetColumnProjectRelSaveReqVO createReqVO) {
        this.removeProjectRelByColumnId(createReqVO.getColumnId());
        AssetsAssetColumnProjectRelDO rel = BeanUtils.toBean(createReqVO, AssetsAssetColumnProjectRelDO.class);
        assetsAssetColumnProjectRelMapper.insert(rel);
        return rel.getId();
    }

    @Override
    public int updateAssetColumnProjectRel(AssetsAssetColumnProjectRelSaveReqVO updateReqVO) {
        AssetsAssetColumnProjectRelDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetColumnProjectRelDO.class);
        return assetsAssetColumnProjectRelMapper.updateById(updateObj);
    }

    @Override
    public int removeAssetColumnProjectRel(Collection<Long> idList) {
        return assetsAssetColumnProjectRelMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeProjectRelByColumnId(Long columnId) {
        assetsAssetColumnProjectRelMapper.removeProjectRelByColumnId(columnId);
        return 1;
    }

    @Override
    public AssetsAssetColumnProjectRelDO getAssetColumnProjectRelById(Long id) {
        return assetsAssetColumnProjectRelMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetColumnProjectRelDO> getAssetColumnProjectRelList() {
        return assetsAssetColumnProjectRelMapper.selectList();
    }
}
