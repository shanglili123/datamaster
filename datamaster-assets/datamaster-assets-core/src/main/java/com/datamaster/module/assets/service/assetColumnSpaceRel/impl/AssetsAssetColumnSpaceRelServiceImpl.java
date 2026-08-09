package com.datamaster.module.assets.service.assetColumnSpaceRel.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumnSpaceRel.AssetsAssetColumnSpaceRelDO;
import com.datamaster.module.assets.dal.mapper.assetColumnSpaceRel.AssetsAssetColumnSpaceRelMapper;
import com.datamaster.module.assets.service.assetColumnSpaceRel.IAssetsAssetColumnSpaceRelService;
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
public class AssetsAssetColumnSpaceRelServiceImpl extends ServiceImpl<AssetsAssetColumnSpaceRelMapper, AssetsAssetColumnSpaceRelDO> implements IAssetsAssetColumnSpaceRelService {
    @Resource
    private AssetsAssetColumnSpaceRelMapper assetsAssetColumnSpaceRelMapper;

    @Override
    public PageResult<AssetsAssetColumnSpaceRelDO> getAssetColumnSpaceRelPage(AssetsAssetColumnSpaceRelPageReqVO pageReqVO) {
        return assetsAssetColumnSpaceRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAssetColumnSpaceRel(AssetsAssetColumnSpaceRelSaveReqVO createReqVO) {
        AssetsAssetColumnSpaceRelDO existingRel = lambdaQuery()
                .eq(AssetsAssetColumnSpaceRelDO::getColumnId, createReqVO.getColumnId())
                .one();
        if (existingRel != null) {
            AssetsAssetColumnSpaceRelDO updateRel = BeanUtils.toBean(createReqVO, AssetsAssetColumnSpaceRelDO.class);
            updateRel.setId(existingRel.getId());
            assetsAssetColumnSpaceRelMapper.updateById(updateRel);
            return existingRel.getId();
        }
        AssetsAssetColumnSpaceRelDO rel = BeanUtils.toBean(createReqVO, AssetsAssetColumnSpaceRelDO.class);
        assetsAssetColumnSpaceRelMapper.insert(rel);
        return rel.getId();
    }

    @Override
    public int updateAssetColumnSpaceRel(AssetsAssetColumnSpaceRelSaveReqVO updateReqVO) {
        AssetsAssetColumnSpaceRelDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetColumnSpaceRelDO.class);
        return assetsAssetColumnSpaceRelMapper.updateById(updateObj);
    }

    @Override
    public int removeAssetColumnSpaceRel(Collection<Long> idList) {
        return assetsAssetColumnSpaceRelMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeSpaceRelByColumnId(Long columnId) {
        assetsAssetColumnSpaceRelMapper.removeSpaceRelByColumnId(columnId);
        return 1;
    }

    @Override
    public AssetsAssetColumnSpaceRelDO getAssetColumnSpaceRelById(Long id) {
        return assetsAssetColumnSpaceRelMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetColumnSpaceRelDO> getAssetColumnSpaceRelList() {
        return assetsAssetColumnSpaceRelMapper.selectList();
    }
}
