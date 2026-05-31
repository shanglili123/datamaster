package com.datamaster.module.assets.service.assetchild.theme.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.theme.AssetsAssetThemeRelDO;
import com.datamaster.module.assets.dal.mapper.assetchild.theme.AssetsAssetThemeRelMapper;
import com.datamaster.module.assets.service.assetchild.theme.IAssetsAssetThemeRelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetThemeRelServiceImpl extends ServiceImpl<AssetsAssetThemeRelMapper, AssetsAssetThemeRelDO> implements IAssetsAssetThemeRelService {
    @Resource
    private AssetsAssetThemeRelMapper AssetsAssetThemeRelMapper;

    @Override
    public PageResult<AssetsAssetThemeRelDO> getDaAssetThemeRelPage(AssetsAssetThemeRelPageReqVO pageReqVO) {
        return AssetsAssetThemeRelMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AssetsAssetThemeRelRespVO> getDaAssetThemeRelList(AssetsAssetThemeRelPageReqVO pageReqVO) {
        MPJLambdaWrapper<AssetsAssetThemeRelDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsAssetThemeRelDO.class)
                .select("t2.NAME as themeName")
                .leftJoin("TAX_THEME t2 on t.THEME_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .likeIfExists("t2.NAME", pageReqVO.getThemeName())
                .eq("t2.DEL_FLAG","0")
                .eq(pageReqVO.getAssetId() != null, AssetsAssetThemeRelDO::getAssetId, pageReqVO.getAssetId())
                .in(CollectionUtils.isNotEmpty(pageReqVO.getThemeIdList()), AssetsAssetThemeRelDO::getThemeId, pageReqVO.getThemeIdList());
        List<AssetsAssetThemeRelDO> AssetsAssetThemeRelDOList = AssetsAssetThemeRelMapper.selectJoinList(AssetsAssetThemeRelDO.class, lambdaWrapper);
        return BeanUtils.toBean(AssetsAssetThemeRelDOList, AssetsAssetThemeRelRespVO.class);
    }

    @Override
    public List<Long> getDaAssetIdList(List<Long> themeIdList) {
        return AssetsAssetThemeRelMapper.getDaAssetIdList(themeIdList);
    }

    @Override
    public Long createDaAssetThemeRel(AssetsAssetThemeRelSaveReqVO createReqVO) {
        AssetsAssetThemeRelDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetThemeRelDO.class);
        AssetsAssetThemeRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public void createDaAssetThemeRelList(List<String> themeIdList, Long assetId) {
        this.removeThemeRelByAssetId(assetId);
        for (String themeId : themeIdList) {
            AssetsAssetThemeRelSaveReqVO createReqVO = new AssetsAssetThemeRelSaveReqVO();
            createReqVO.setThemeId(JSONUtils.convertToLong(themeId));
            createReqVO.setAssetId(assetId);
            this.createDaAssetThemeRel(createReqVO);
        }
    }

    @Override
    public int updateDaAssetThemeRel(AssetsAssetThemeRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产-主题关联关系
        AssetsAssetThemeRelDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetThemeRelDO.class);
        return AssetsAssetThemeRelMapper.updateById(updateObj);
    }

    @Override
    public int removeDaAssetThemeRel(Collection<Long> idList) {
        // 批量删除数据资产-主题关联关系
        return AssetsAssetThemeRelMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeThemeRelByAssetId(Long assetId) {
        AssetsAssetThemeRelMapper.deleteDaAssetThemeRelByAssetId(assetId);
        return 1;
    }

    @Override
    public AssetsAssetThemeRelDO getDaAssetThemeRelById(Long id) {
        return AssetsAssetThemeRelMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetThemeRelDO> getDaAssetThemeRelList() {
        return AssetsAssetThemeRelMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetThemeRelDO> getDaAssetThemeRelMap() {
        List<AssetsAssetThemeRelDO> AssetsAssetThemeRelList = AssetsAssetThemeRelMapper.selectList();
        return AssetsAssetThemeRelList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetThemeRelDO::getId,
                        AssetsAssetThemeRelDO -> AssetsAssetThemeRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     * -
     *
     * @param importExcelList -
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importDaAssetThemeRel(List<AssetsAssetThemeRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetThemeRelRespVO respVO : importExcelList) {
            try {
                AssetsAssetThemeRelDO AssetsAssetThemeRelDO = BeanUtils.toBean(respVO, AssetsAssetThemeRelDO.class);
                Long AssetsAssetThemeRelId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetThemeRelId != null) {
                        AssetsAssetThemeRelDO existingDaAssetThemeRel = AssetsAssetThemeRelMapper.selectById(AssetsAssetThemeRelId);
                        if (existingDaAssetThemeRel != null) {
                            AssetsAssetThemeRelMapper.updateById(AssetsAssetThemeRelDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetThemeRelId + " -");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetThemeRelId + " -");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetThemeRelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetThemeRelId);
                    AssetsAssetThemeRelDO existingDaAssetThemeRel = AssetsAssetThemeRelMapper.selectOne(queryWrapper);
                    if (existingDaAssetThemeRel == null) {
                        AssetsAssetThemeRelMapper.insert(AssetsAssetThemeRelDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetThemeRelId + " -");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetThemeRelId + " -");
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
