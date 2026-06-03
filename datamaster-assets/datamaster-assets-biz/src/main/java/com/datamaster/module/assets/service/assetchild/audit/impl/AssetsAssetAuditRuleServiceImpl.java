package com.datamaster.module.assets.service.assetchild.audit.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRulePageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRuleRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRuleSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditRuleDO;
import com.datamaster.module.assets.dal.mapper.assetchild.audit.AssetsAssetAuditRuleMapper;
import com.datamaster.module.assets.service.assetchild.audit.IAssetsAssetAuditRuleService;

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
 * @date 2025-05-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetAuditRuleServiceImpl  extends ServiceImpl<AssetsAssetAuditRuleMapper,AssetsAssetAuditRuleDO> implements IAssetsAssetAuditRuleService {
    @Resource
    private AssetsAssetAuditRuleMapper AssetsAssetAuditRuleMapper;

    @Override
    public PageResult<AssetsAssetAuditRuleDO> getAssetAuditRulePage(AssetsAssetAuditRulePageReqVO pageReqVO) {
        return AssetsAssetAuditRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAssetAuditRule(AssetsAssetAuditRuleSaveReqVO createReqVO) {
        AssetsAssetAuditRuleDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetAuditRuleDO.class);
        AssetsAssetAuditRuleMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetAuditRule(AssetsAssetAuditRuleSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产质量结果记录
        AssetsAssetAuditRuleDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetAuditRuleDO.class);
        return AssetsAssetAuditRuleMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetAuditRule(Collection<Long> idList) {
        // 批量删除数据资产质量结果记录
        return AssetsAssetAuditRuleMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetAuditRuleDO getAssetAuditRuleById(Long id) {
        return AssetsAssetAuditRuleMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetAuditRuleDO> getAssetAuditRuleList() {
        return AssetsAssetAuditRuleMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetAuditRuleDO> getAssetAuditRuleMap() {
        List<AssetsAssetAuditRuleDO> AssetsAssetAuditRuleList = AssetsAssetAuditRuleMapper.selectList();
        return AssetsAssetAuditRuleList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetAuditRuleDO::getId,
                        AssetsAssetAuditRuleDO -> AssetsAssetAuditRuleDO,
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
    public String importAssetAuditRule(List<AssetsAssetAuditRuleRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetAuditRuleRespVO respVO : importExcelList) {
            try {
                AssetsAssetAuditRuleDO AssetsAssetAuditRuleDO = BeanUtils.toBean(respVO, AssetsAssetAuditRuleDO.class);
                Long AssetsAssetAuditRuleId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetAuditRuleId != null) {
                        AssetsAssetAuditRuleDO existingAssetAuditRule = AssetsAssetAuditRuleMapper.selectById(AssetsAssetAuditRuleId);
                        if (existingAssetAuditRule != null) {
                            AssetsAssetAuditRuleMapper.updateById(AssetsAssetAuditRuleDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetAuditRuleId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetAuditRuleId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetAuditRuleDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetAuditRuleId);
                    AssetsAssetAuditRuleDO existingAssetAuditRule = AssetsAssetAuditRuleMapper.selectOne(queryWrapper);
                    if (existingAssetAuditRule == null) {
                        AssetsAssetAuditRuleMapper.insert(AssetsAssetAuditRuleDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetAuditRuleId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetAuditRuleId + " ");
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
