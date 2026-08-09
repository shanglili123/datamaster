package com.datamaster.module.assets.service.assetchild.spaceRel.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel.AssetsAssetSpaceRelDO;
import com.datamaster.module.assets.dal.mapper.assetchild.spaceRel.AssetsAssetSpaceRelMapper;
import com.datamaster.module.assets.service.assetchild.spaceRel.IAssetsAssetSpaceRelService;

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
 * @date 2025-04-18
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetSpaceRelServiceImpl  extends ServiceImpl<AssetsAssetSpaceRelMapper,AssetsAssetSpaceRelDO> implements IAssetsAssetSpaceRelService {
    @Resource
    private AssetsAssetSpaceRelMapper AssetsAssetSpaceRelMapper;

    @Override
    public PageResult<AssetsAssetSpaceRelDO> getAssetSpaceRelPage(AssetsAssetSpaceRelPageReqVO pageReqVO) {
        return AssetsAssetSpaceRelMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AssetsAssetSpaceRelDO> getAssetSpaceRelList(AssetsAssetSpaceRelPageReqVO pageReqVO) {
        return AssetsAssetSpaceRelMapper.selectList(pageReqVO);
    }

    @Override
    public Long createAssetSpaceRel(AssetsAssetSpaceRelSaveReqVO createReqVO) {
        AssetsAssetSpaceRelDO existingRel = lambdaQuery()
                .eq(AssetsAssetSpaceRelDO::getAssetId, createReqVO.getAssetId())
                .one();
        if (existingRel != null) {
            AssetsAssetSpaceRelDO updateRel = BeanUtils.toBean(createReqVO, AssetsAssetSpaceRelDO.class);
            updateRel.setId(existingRel.getId());
            AssetsAssetSpaceRelMapper.updateById(updateRel);
            return existingRel.getId();
        }
        AssetsAssetSpaceRelDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetSpaceRelDO.class);
        AssetsAssetSpaceRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int removeSpaceRelByAssetId(Long assetId) {
        AssetsAssetSpaceRelMapper.removeSpaceRelByAssetId(assetId);
        return 1;
    }

    @Override
    public int updateAssetSpaceRel(AssetsAssetSpaceRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产与空间关联关系
        AssetsAssetSpaceRelDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetSpaceRelDO.class);
        return AssetsAssetSpaceRelMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetSpaceRel(Collection<Long> idList) {
        // 批量删除数据资产与空间关联关系
        return AssetsAssetSpaceRelMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetSpaceRelDO getAssetSpaceRelById(Long id) {
        return AssetsAssetSpaceRelMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetSpaceRelDO> getAssetSpaceRelList() {
        return AssetsAssetSpaceRelMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetSpaceRelDO> getAssetSpaceRelMap() {
        List<AssetsAssetSpaceRelDO> AssetsAssetSpaceRelList = AssetsAssetSpaceRelMapper.selectList();
        return AssetsAssetSpaceRelList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetSpaceRelDO::getId,
                        AssetsAssetSpaceRelDO -> AssetsAssetSpaceRelDO,
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
    public String importAssetSpaceRel(List<AssetsAssetSpaceRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetSpaceRelRespVO respVO : importExcelList) {
            try {
                AssetsAssetSpaceRelDO AssetsAssetSpaceRelDO = BeanUtils.toBean(respVO, AssetsAssetSpaceRelDO.class);
                Long AssetsAssetSpaceRelId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetSpaceRelId != null) {
                        AssetsAssetSpaceRelDO existingAssetSpaceRel = AssetsAssetSpaceRelMapper.selectById(AssetsAssetSpaceRelId);
                        if (existingAssetSpaceRel != null) {
                            AssetsAssetSpaceRelMapper.updateById(AssetsAssetSpaceRelDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetSpaceRelId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetSpaceRelId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetSpaceRelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetSpaceRelId);
                    AssetsAssetSpaceRelDO existingAssetSpaceRel = AssetsAssetSpaceRelMapper.selectOne(queryWrapper);
                    if (existingAssetSpaceRel == null) {
                        AssetsAssetSpaceRelMapper.insert(AssetsAssetSpaceRelDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetSpaceRelId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetSpaceRelId + " ");
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
}
