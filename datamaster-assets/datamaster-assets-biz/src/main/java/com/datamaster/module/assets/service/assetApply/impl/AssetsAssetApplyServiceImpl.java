package com.datamaster.module.assets.service.assetApply.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplyRespVO;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplySaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetApply.AssetsAssetApplyDO;
import com.datamaster.module.assets.dal.mapper.assetApply.AssetsAssetApplyMapper;
import com.datamaster.module.assets.service.assetchild.theme.IAssetsAssetThemeRelService;
import com.datamaster.module.assets.service.assetApply.IAssetsAssetApplyService;
import com.datamaster.module.system.api.message.dto.MessageSaveReqDTO;
import com.datamaster.module.system.service.ISysMessageService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据资产申请Service业务层处理 * * @author shu * @date 2025-03-19
 */

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetApplyServiceImpl extends ServiceImpl<AssetsAssetApplyMapper, AssetsAssetApplyDO> implements IAssetsAssetApplyService {
    @Resource
    private AssetsAssetApplyMapper AssetsAssetApplyMapper;
    @Resource
    private ISysMessageService iSysMessageService;
    @Resource
    private IAssetsAssetThemeRelService AssetsAssetThemeRelService;

    @Override
    public PageResult<AssetsAssetApplyDO> getAssetApplyPage(AssetsAssetApplyPageReqVO pageReqVO) {
        if (StringUtils.isNotEmpty(pageReqVO.getThemeName())) {
            AssetsAssetThemeRelPageReqVO rel = new AssetsAssetThemeRelPageReqVO();
            rel.setThemeName(pageReqVO.getThemeName());
            List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList = AssetsAssetThemeRelService.getAssetThemeRelList(rel);
            if (AssetsAssetThemeRelList.isEmpty()) {
                return PageResult.empty();
            }
            Set<Long> assetIds = AssetsAssetThemeRelList.stream().map(AssetsAssetThemeRelRespVO::getAssetId).collect(Collectors.toSet());
            pageReqVO.setAssetIds(assetIds);
        }
        PageResult<AssetsAssetApplyDO> AssetsAssetApplyDOPageResult = AssetsAssetApplyMapper.selectPage(pageReqVO);
        List<AssetsAssetApplyDO> AssetsAssetApplyDOList = (List<AssetsAssetApplyDO>) AssetsAssetApplyDOPageResult.getRows();
        for (AssetsAssetApplyDO AssetsAssetApplyDO : AssetsAssetApplyDOList) {
            AssetsAssetThemeRelPageReqVO AssetsAssetThemeRelPageReqVO = new AssetsAssetThemeRelPageReqVO();
            AssetsAssetThemeRelPageReqVO.setAssetId(AssetsAssetApplyDO.getAssetId());
            List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList = AssetsAssetThemeRelService.getAssetThemeRelList(AssetsAssetThemeRelPageReqVO);
            if (!AssetsAssetThemeRelList.isEmpty()) {
                String themeName = AssetsAssetThemeRelList.stream().map(AssetsAssetThemeRelRespVO::getThemeName).collect(Collectors.joining(","));
                AssetsAssetApplyDO.setThemeName(themeName);
                AssetsAssetApplyDO.setAssetsAssetThemeRelList(AssetsAssetThemeRelList);
            }
        }
        return AssetsAssetApplyDOPageResult;
    }

    @Override
    public Long createAssetApply(AssetsAssetApplySaveReqVO createReqVO) {
        AssetsAssetApplyDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetApplyDO.class);
        LambdaQueryWrapperX<AssetsAssetApplyDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eq(true, AssetsAssetApplyDO::getAssetId, dictType.getAssetId()).eq(true, AssetsAssetApplyDO::getProjectId, dictType.getProjectId()).eq(true, AssetsAssetApplyDO::getProjectCode, dictType.getProjectCode());
        AssetsAssetApplyDO AssetsAssetApplyDO = AssetsAssetApplyMapper.selectOne(queryWrapperX);
        if (AssetsAssetApplyDO != null && "2".equals(AssetsAssetApplyDO.getStatus())) {
            AssetsAssetApplyDO.setStatus("1");
            int updateById = AssetsAssetApplyMapper.updateById(AssetsAssetApplyDO);
            return AssetsAssetApplyDO.getId();
        }
        if (AssetsAssetApplyDO != null) {
            throw new ServiceException(createReqVO.getProjectName() + "" + createReqVO.getAssetName() + ",!");
        }
        AssetsAssetApplyMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetApply(AssetsAssetApplySaveReqVO updateReqVO) {
// 相关校验
// 更新数据资产申请
        AssetsAssetApplyDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetApplyDO.class);
        int updateById = AssetsAssetApplyMapper.updateById(updateObj);
        if (updateById > 0 && !"1".equals(updateObj.getStatus())) {
            MessageSaveReqDTO messageSaveReqDTO = new MessageSaveReqDTO();
            messageSaveReqDTO.setSenderId(1L);
            messageSaveReqDTO.setCreatorId(1L);
            messageSaveReqDTO.setCreateBy("超级管理员");
            messageSaveReqDTO.setReceiverId(updateObj.getCreatorId());
            Map<String, Object> map = new HashMap<>();
            map.put("assetName", updateObj.getAssetName());
            map.put("time", DateUtil.date());
            map.put("userName", updateObj.getUpdateBy());
            map.put("statusName", "2".equals(updateObj.getStatus()) ? "" : "");
            map.put("approvalReason", updateObj.getApprovalReason());
            iSysMessageService.send("2".equals(updateObj.getStatus()) ? 6L : 5L, messageSaveReqDTO, map);
        }
        return updateById;
    }

    @Override
    public int removeAssetApply(Collection<Long> idList) {
// 批量删除数据资产申请
        return AssetsAssetApplyMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetApplyDO getAssetApplyById(Long id) {
        MPJLambdaWrapper<AssetsAssetApplyDO> lambdaWrapper = new MPJLambdaWrapper<>();
        lambdaWrapper.selectAll(AssetsAssetApplyDO.class).select("t2.NAME AS assetName,t2.DESCRIPTION AS description, t2.TABLE_NAME AS assetTableName, t3.NAME AS projectName, " + "t5.DATASOURCE_NAME AS datasourceName, t5.IP AS datasourceIp, t5.DATASOURCE_TYPE AS datasourceType, t6.PHONENUMBER AS phonenumber").leftJoin("AST_ASSET t2 on t.ASSET_ID = t2.ID AND t2.DEL_FLAG = '0'").leftJoin("TAX_PROJECT t3 on t.PROJECT_ID = t3.ID AND t3.DEL_FLAG = '0'")
//                .leftJoin("ATT_THEME t4 on t2.THEME_ID = t4.ID AND t4.DEL_FLAG = '0'")
                .leftJoin("AST_DATASOURCE t5 on t2.DATASOURCE_ID = t5.ID AND t5.DEL_FLAG = '0'")
                .leftJoin("SYSTEM_USER t6 on t.CREATOR_ID = t6.USER_ID AND t6.DEL_FLAG = '0'")
                .eq(true, AssetsAssetApplyDO::getId, id);
        AssetsAssetApplyDO AssetsAssetApplyDO = AssetsAssetApplyMapper.selectOne(lambdaWrapper);
        AssetsAssetThemeRelPageReqVO AssetsAssetThemeRelPageReqVO = new AssetsAssetThemeRelPageReqVO();
        AssetsAssetThemeRelPageReqVO.setAssetId(AssetsAssetApplyDO.getId());
        List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList = AssetsAssetThemeRelService.getAssetThemeRelList(AssetsAssetThemeRelPageReqVO);
        AssetsAssetApplyDO.setAssetsAssetThemeRelList(AssetsAssetThemeRelList);
        return AssetsAssetApplyDO;
    }

    @Override
    public List<AssetsAssetApplyDO> getAssetApplyList() {
        return AssetsAssetApplyMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetApplyDO> getAssetApplyMap() {
        List<AssetsAssetApplyDO> AssetsAssetApplyList = AssetsAssetApplyMapper.selectList();
        return AssetsAssetApplyList.stream().collect(Collectors.toMap(AssetsAssetApplyDO::getId, AssetsAssetApplyDO -> AssetsAssetApplyDO,
// 保留已存在的值
                (existing, replacement) -> existing));
    }

    /***     *      *     * @param importExcelList      * @param isUpdateSupport      * @param operName             * @return      */

    @Override
    public String importAssetApply(List<AssetsAssetApplyRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }
        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();
        for (AssetsAssetApplyRespVO respVO : importExcelList) {
            try {
                AssetsAssetApplyDO AssetsAssetApplyDO = BeanUtils.toBean(respVO, AssetsAssetApplyDO.class);
                Long AssetsAssetApplyId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetApplyId != null) {
                        AssetsAssetApplyDO existingAssetApply = AssetsAssetApplyMapper.selectById(AssetsAssetApplyId);
                        if (existingAssetApply != null) {
                            AssetsAssetApplyMapper.updateById(AssetsAssetApplyDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetApplyId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetApplyId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetApplyDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetApplyId);
                    AssetsAssetApplyDO existingAssetApply = AssetsAssetApplyMapper.selectOne(queryWrapper);
                    if (existingAssetApply == null) {
                        AssetsAssetApplyMapper.insert(AssetsAssetApplyDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetApplyId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetApplyId + " ");
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