package com.datamaster.module.assets.service.sensitiveLevel.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelPageReqVO;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelRespVO;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.sensitiveLevel.AssetsSensitiveLevelDO;
import com.datamaster.module.assets.dal.mapper.sensitiveLevel.AssetsSensitiveLevelMapper;
import com.datamaster.module.assets.service.assetColumn.IAssetsAssetColumnService;
import com.datamaster.module.assets.service.sensitiveLevel.IAssetsSensitiveLevelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsSensitiveLevelServiceImpl extends ServiceImpl<AssetsSensitiveLevelMapper, AssetsSensitiveLevelDO> implements IAssetsSensitiveLevelService {
    @Resource
    private AssetsSensitiveLevelMapper AssetsSensitiveLevelMapper;
    @Resource
    private IAssetsAssetColumnService AssetsAssetColumnService;

    @Override
    public PageResult<AssetsSensitiveLevelDO> getDaSensitiveLevelPage(AssetsSensitiveLevelPageReqVO pageReqVO) {
        return AssetsSensitiveLevelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDaSensitiveLevel(AssetsSensitiveLevelSaveReqVO createReqVO) {
        AssetsSensitiveLevelDO dictType = BeanUtils.toBean(createReqVO, AssetsSensitiveLevelDO.class);
        AssetsSensitiveLevelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDaSensitiveLevel(AssetsSensitiveLevelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新敏感等级
        AssetsSensitiveLevelDO updateObj = BeanUtils.toBean(updateReqVO, AssetsSensitiveLevelDO.class);
        return AssetsSensitiveLevelMapper.updateById(updateObj);
    }

    @Override
    public int removeDaSensitiveLevel(Collection<Long> idList) {
        // 批量删除敏感等级
        return AssetsSensitiveLevelMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsSensitiveLevelDO getDaSensitiveLevelById(Long id) {
        return AssetsSensitiveLevelMapper.selectById(id);
    }

    @Override
    public List<AssetsSensitiveLevelDO> getDaSensitiveLevelList() {
        return AssetsSensitiveLevelMapper.selectList();
    }

    @Override
    public Map<Long, AssetsSensitiveLevelDO> getDaSensitiveLevelMap() {
        List<AssetsSensitiveLevelDO> AssetsSensitiveLevelList = AssetsSensitiveLevelMapper.selectList();
        return AssetsSensitiveLevelList.stream()
                .collect(Collectors.toMap(
                        AssetsSensitiveLevelDO::getId,
                        AssetsSensitiveLevelDO -> AssetsSensitiveLevelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importDaSensitiveLevel(List<AssetsSensitiveLevelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsSensitiveLevelRespVO respVO : importExcelList) {
            try {
                AssetsSensitiveLevelDO AssetsSensitiveLevelDO = BeanUtils.toBean(respVO, AssetsSensitiveLevelDO.class);
                Long AssetsSensitiveLevelId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsSensitiveLevelId != null) {
                        AssetsSensitiveLevelDO existingDaSensitiveLevel = AssetsSensitiveLevelMapper.selectById(AssetsSensitiveLevelId);
                        if (existingDaSensitiveLevel != null) {
                            AssetsSensitiveLevelMapper.updateById(AssetsSensitiveLevelDO);
                            successNum++;
                            successMessages.add("ID " + AssetsSensitiveLevelId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsSensitiveLevelId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsSensitiveLevelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsSensitiveLevelId);
                    AssetsSensitiveLevelDO existingDaSensitiveLevel = AssetsSensitiveLevelMapper.selectOne(queryWrapper);
                    if (existingDaSensitiveLevel == null) {
                        AssetsSensitiveLevelMapper.insert(AssetsSensitiveLevelDO);
                        successNum++;
                        successMessages.add("ID " + AssetsSensitiveLevelId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsSensitiveLevelId + " ");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append(" ").append(failureNum).append(" ");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append(" ").append(successNum).append(" ");
        }
        return resultMsg.toString();
    }

    @Override
    public Boolean updateStatus(Long id, Long status) {
        AssetsAssetColumnPageReqVO AssetsAssetColumnPageReqVO = new AssetsAssetColumnPageReqVO();
        AssetsAssetColumnPageReqVO.setSensitiveLevelId(id.toString());
        List<AssetsAssetColumnDO> AssetsAssetColumnList = AssetsAssetColumnService.getDaAssetColumnList(AssetsAssetColumnPageReqVO);
        if (!AssetsAssetColumnList.isEmpty()) {
            return false;
        }
        return this.update(Wrappers.lambdaUpdate(AssetsSensitiveLevelDO.class)
                .eq(AssetsSensitiveLevelDO::getId, id)
                .set(AssetsSensitiveLevelDO::getOnlineFlag, status));
    }
}
