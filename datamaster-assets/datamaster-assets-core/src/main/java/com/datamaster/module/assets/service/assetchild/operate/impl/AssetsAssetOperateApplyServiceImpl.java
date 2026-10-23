package com.datamaster.module.assets.service.assetchild.operate.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.security.AccessPolicy;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplySaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateApplyDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateLogDO;
import com.datamaster.module.assets.dal.mapper.assetchild.operate.AssetsAssetOperateApplyMapper;
import com.datamaster.module.assets.service.assetchild.operate.IAssetsAssetOperateApplyService;
import com.datamaster.module.assets.service.assetchild.operate.IAssetsAssetOperateLogService;
import com.datamaster.module.assets.service.assetchild.operate.enums.AssetOperateApplyStatus;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class AssetsAssetOperateApplyServiceImpl  extends ServiceImpl<AssetsAssetOperateApplyMapper,AssetsAssetOperateApplyDO> implements IAssetsAssetOperateApplyService {
    @Resource
    private AssetsAssetOperateApplyMapper AssetsAssetOperateApplyMapper;

    @Resource
    private IAssetsAssetOperateLogService AssetsAssetOperateLogService;

    @Override
    public PageResult<AssetsAssetOperateApplyDO> getAssetOperateApplyPage(AssetsAssetOperateApplyPageReqVO pageReqVO) {
        if (!isPrivilegedOperator()) {
            pageReqVO.setCreatorId(SecurityUtils.getUserId());
        }
        return AssetsAssetOperateApplyMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAssetOperateApply(AssetsAssetOperateApplySaveReqVO createReqVO) {
        validateRequest(createReqVO);
        AssetsAssetOperateApplyDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetOperateApplyDO.class);
        dictType.setCreatorId(SecurityUtils.getUserId());
        dictType.setApprovalStatus(AssetOperateApplyStatus.DRAFT);
        dictType.setExecuteFlag("0");
        dictType.setExecuteTime(null);
        AssetsAssetOperateApplyMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetOperateApply(AssetsAssetOperateApplySaveReqVO updateReqVO) {
        validateRequest(updateReqVO);
        AssetsAssetOperateApplyDO current = requireApply(updateReqVO.getId());
        assertOwnerOrPrivileged(current, "修改");
        String status = normalizeStatus(current);
        if (!AssetOperateApplyStatus.DRAFT.equals(status)
                && !AssetOperateApplyStatus.REJECTED.equals(status)
                && !AssetOperateApplyStatus.FAILED.equals(status)) {
            throw new ServiceException("当前状态不允许修改：" + status);
        }
        AssetsAssetOperateApplyDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetOperateApplyDO.class);
        updateObj.setCreatorId(current.getCreatorId());
        updateObj.setCreateBy(current.getCreateBy());
        updateObj.setCreateTime(current.getCreateTime());
        updateObj.setApprovalStatus(status);
        updateObj.setExecuteFlag("0");
        updateObj.setExecuteTime(null);
        return AssetsAssetOperateApplyMapper.updateById(updateObj);
    }
    @Override
    public int submitAssetOperateApply(Long id, Long userId, String userName) {
        AssetsAssetOperateApplyDO current = requireApply(id);
        assertOwnerOrPrivileged(current, "提交");
        String status = normalizeStatus(current);
        if (!AssetOperateApplyStatus.DRAFT.equals(status)
                && !AssetOperateApplyStatus.REJECTED.equals(status)
                && !AssetOperateApplyStatus.FAILED.equals(status)) {
            throw new ServiceException("当前状态不能提交申请：" + status);
        }
        return transition(id, status, AssetOperateApplyStatus.PENDING_APPROVAL,
                null, null, null, null, userId, userName);
    }

    @Override
    public int approveAssetOperateApply(Long id, String comment, Long userId, String userName) {
        return review(id, comment, userId, userName, AssetOperateApplyStatus.APPROVED);
    }

    @Override
    public int rejectAssetOperateApply(Long id, String comment, Long userId, String userName) {
        if (StringUtils.isBlank(comment)) {
            throw new ServiceException("驳回申请必须填写审批意见");
        }
        return review(id, comment, userId, userName, AssetOperateApplyStatus.REJECTED);
    }

    private int review(Long id, String comment, Long userId, String userName, String targetStatus) {
        AssetsAssetOperateApplyDO current = requireApply(id);
        assertPrivilegedOperator("审批");
        if (!AssetOperateApplyStatus.PENDING_APPROVAL.equals(normalizeStatus(current))) {
            throw new ServiceException("只有待审批申请才能审批，当前状态：" + normalizeStatus(current));
        }
        if (userId != null && Objects.equals(current.getCreatorId(), userId)) {
            throw new ServiceException("申请人不能审批自己的资产操作申请");
        }
        return transition(id, AssetOperateApplyStatus.PENDING_APPROVAL, targetStatus,
                comment, userName, userId, new Date(), userId, userName);
    }

    @Override
    public int executeAssetOperateApply(Long id, Long userId, String userName) {
        AssetsAssetOperateApplyDO current = requireApply(id);
        assertOwnerOrPrivileged(current, "执行");
        if (!AssetOperateApplyStatus.APPROVED.equals(normalizeStatus(current))) {
            throw new ServiceException("只有审批通过的申请才能执行，当前状态：" + normalizeStatus(current));
        }
        int claimed = transition(id, AssetOperateApplyStatus.APPROVED, AssetOperateApplyStatus.EXECUTING,
                null, null, null, null, userId, userName);
        try {
            AssetsAssetOperateLogSaveReqVO logRequest = toOperateLogRequest(current);
            if ("4".equals(logRequest.getOperateType())) {
                throw new ServiceException("导入类型申请暂不支持自动执行，请使用文件导入任务");
            }
            Long logId = AssetsAssetOperateLogService.createAssetOperateLog(logRequest);
            AssetsAssetOperateLogDO operationLog = AssetsAssetOperateLogService.getAssetOperateLogById(logId);
            boolean success = operationLog != null && "3".equals(operationLog.getStatus());
            String message = success ? "执行成功，操作记录ID=" + logId : "执行失败，操作记录ID=" + logId;
            finishExecution(id, success ? AssetOperateApplyStatus.EXECUTED : AssetOperateApplyStatus.FAILED,
                    success ? "1" : "0", message, userId, userName);
            if (!success) {
                return 0;
            }
            return claimed;
        } catch (RuntimeException ex) {
            finishExecution(id, AssetOperateApplyStatus.FAILED, "0",
                    StringUtils.defaultIfBlank(ex.getMessage(), "执行失败"), userId, userName);
            log.warn("资产操作申请执行失败，applyId={}", id, ex);
            return 0;
        }
    }

    private void validateRequest(AssetsAssetOperateApplySaveReqVO request) {
        if (request == null || request.getAssetId() == null || request.getDatasourceId() == null
                || StringUtils.isBlank(request.getTableName()) || StringUtils.isBlank(request.getOperateType())
                || StringUtils.isBlank(request.getOperateJson())) {
            throw new ServiceException("资产操作申请缺少必要参数");
        }
        if (!Arrays.asList("1", "2", "3", "4").contains(request.getOperateType())) {
            throw new ServiceException("不支持的资产操作类型：" + request.getOperateType());
        }
    }

    private AssetsAssetOperateApplyDO requireApply(Long id) {
        if (id == null) {
            throw new ServiceException("申请 ID 不能为空");
        }
        AssetsAssetOperateApplyDO current = AssetsAssetOperateApplyMapper.selectById(id);
        if (current == null || Boolean.TRUE.equals(current.getDelFlag())) {
            throw new ServiceException("资产操作申请不存在：" + id);
        }
        return current;
    }

    private String normalizeStatus(AssetsAssetOperateApplyDO current) {
        if (StringUtils.isNotBlank(current.getApprovalStatus())) {
            return current.getApprovalStatus();
        }
        return "1".equals(current.getExecuteFlag()) && current.getExecuteTime() != null
                ? AssetOperateApplyStatus.EXECUTED : AssetOperateApplyStatus.DRAFT;
    }

    private int transition(Long id, String expected, String target, String comment, String approvedBy,
                           Long approvedId, Date approvedTime, Long updaterId, String updaterName) {
        LambdaUpdateWrapper<AssetsAssetOperateApplyDO> wrapper = new LambdaUpdateWrapper<AssetsAssetOperateApplyDO>()
                .eq(AssetsAssetOperateApplyDO::getId, id);
        if (AssetOperateApplyStatus.DRAFT.equals(expected)) {
            wrapper.and(w -> w.eq(AssetsAssetOperateApplyDO::getApprovalStatus, expected)
                    .or().isNull(AssetsAssetOperateApplyDO::getApprovalStatus));
        } else {
            wrapper.eq(AssetsAssetOperateApplyDO::getApprovalStatus, expected);
        }
        wrapper.set(AssetsAssetOperateApplyDO::getApprovalStatus, target)
                .set(AssetsAssetOperateApplyDO::getUpdaterId, updaterId)
                .set(AssetsAssetOperateApplyDO::getUpdateBy, updaterName)
                .set(AssetsAssetOperateApplyDO::getUpdateTime, new Date());
        if (comment != null) wrapper.set(AssetsAssetOperateApplyDO::getApprovalComment, comment);
        if (approvedBy != null) wrapper.set(AssetsAssetOperateApplyDO::getApprovedBy, approvedBy);
        if (approvedId != null) wrapper.set(AssetsAssetOperateApplyDO::getApprovedId, approvedId);
        if (approvedTime != null) wrapper.set(AssetsAssetOperateApplyDO::getApprovedTime, approvedTime);
        int updated = AssetsAssetOperateApplyMapper.update(null, wrapper);
        if (updated == 0) {
            throw new ServiceException("申请状态已被其他人修改，请刷新后重试");
        }
        return updated;
    }

    private void finishExecution(Long id, String status, String executeFlag, String message,
                                 Long userId, String userName) {
        AssetsAssetOperateApplyDO update = new AssetsAssetOperateApplyDO();
        update.setId(id);
        update.setApprovalStatus(status);
        update.setExecuteFlag(executeFlag);
        update.setExecuteTime(new Date());
        update.setExecutionMessage(message);
        update.setUpdaterId(userId);
        update.setUpdateBy(userName);
        update.setUpdateTime(new Date());
        AssetsAssetOperateApplyMapper.updateById(update);
    }

    private AssetsAssetOperateLogSaveReqVO toOperateLogRequest(AssetsAssetOperateApplyDO apply) {
        JSONObject json;
        try {
            json = JSON.parseObject(apply.getOperateJson());
        } catch (Exception ex) {
            throw new ServiceException("操作 JSON 格式错误");
        }
        AssetsAssetOperateLogSaveReqVO request = new AssetsAssetOperateLogSaveReqVO();
        request.setAssetId(apply.getAssetId());
        request.setDatasourceId(apply.getDatasourceId());
        request.setTableName(apply.getTableName());
        request.setTableComment(apply.getTableComment());
        request.setOperateType(apply.getOperateType());
        request.setOperateTime(apply.getOperateTime());
        request.setUpdateBefore(text(json, "updateBefore", "before"));
        request.setUpdateAfter(text(json, "updateAfter", "after", "data"));
        request.setFieldNames(text(json, "fieldNames", "fields", "keys"));
        request.setFileUrl(text(json, "fileUrl"));
        request.setFileName(text(json, "fileName"));
        request.setDescription(text(json, "description"));
        request.setStatus("1");
        return request;
    }

    private String text(JSONObject json, String... keys) {
        for (String key : keys) {
            Object value = json.get(key);
            if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                return (String) value;
            }
            if (value != null) {
                return JSON.toJSONString(value);
            }
        }
        return null;
    }

    @Override
    public int removeAssetOperateApply(Collection<Long> idList) {
        // 批量删除数据资产操作申请
        if (idList == null || idList.isEmpty()) return 0;
        for (Long id : idList) {
            assertOwnerOrPrivileged(requireApply(id), "删除");
        }
        return AssetsAssetOperateApplyMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetOperateApplyDO getAssetOperateApplyById(Long id) {
        AssetsAssetOperateApplyDO apply = requireApply(id);
        assertOwnerOrPrivileged(apply, "查看");
        return apply;
    }

    @Override
    public List<AssetsAssetOperateApplyDO> getAssetOperateApplyList() {
        if (isPrivilegedOperator()) return AssetsAssetOperateApplyMapper.selectList();
        return AssetsAssetOperateApplyMapper.selectList(new LambdaQueryWrapper<AssetsAssetOperateApplyDO>()
                .eq(AssetsAssetOperateApplyDO::getCreatorId, SecurityUtils.getUserId()));
    }

    @Override
    public Map<Long, AssetsAssetOperateApplyDO> getAssetOperateApplyMap() {
        List<AssetsAssetOperateApplyDO> AssetsAssetOperateApplyList = isPrivilegedOperator()
                ? AssetsAssetOperateApplyMapper.selectList()
                : AssetsAssetOperateApplyMapper.selectList(new LambdaQueryWrapper<AssetsAssetOperateApplyDO>()
                .eq(AssetsAssetOperateApplyDO::getCreatorId, SecurityUtils.getUserId()));
        return AssetsAssetOperateApplyList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetOperateApplyDO::getId,
                        AssetsAssetOperateApplyDO -> AssetsAssetOperateApplyDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    private void assertOwnerOrPrivileged(AssetsAssetOperateApplyDO apply, String operation) {
        if (apply == null) throw new ServiceException("资产操作申请不存在");
        Long userId = SecurityUtils.getUserId();
        if (!isPrivilegedOperator() && !Objects.equals(apply.getCreatorId(), userId)) {
            throw new ServiceException("无权" + operation + "其他用户创建的资产操作申请");
        }
    }

    private void assertPrivilegedOperator(String operation) {
        if (!isPrivilegedOperator()) {
            throw new ServiceException("只有平台管理员、空间管理员或运维人员才能" + operation + "资产操作申请");
        }
    }

    private boolean isPrivilegedOperator() {
        Long userId = SecurityUtils.getUserId();
        if (SecurityUtils.isAdmin(userId)) return true;
        com.datamaster.common.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();
        java.util.List<com.datamaster.common.core.domain.entity.SysRole> roles =
                loginUser == null || loginUser.getUser() == null ? java.util.Collections.emptyList()
                        : loginUser.getUser().getRoles();
        return AccessPolicy.isPlatformAdmin(userId, roles)
                || AccessPolicy.hasSpaceAdminRole(roles)
                || AccessPolicy.hasOpsRole(roles);
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
        public String importAssetOperateApply(List<AssetsAssetOperateApplyRespVO> importExcelList, boolean isUpdateSupport,
                                              String operName, Long operatorId) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AssetsAssetOperateApplyRespVO respVO : importExcelList) {
                try {
                    AssetsAssetOperateApplyDO AssetsAssetOperateApplyDO = BeanUtils.toBean(respVO, AssetsAssetOperateApplyDO.class);
                    AssetsAssetOperateApplyDO.setCreatorId(operatorId);
                    // Excel 导入只能生成/更新草稿，不能绕过审批状态机写入已批准或已执行状态。
                    AssetsAssetOperateApplyDO.setApprovalStatus(AssetOperateApplyStatus.DRAFT);
                    AssetsAssetOperateApplyDO.setExecuteFlag("0");
                    AssetsAssetOperateApplyDO.setExecuteTime(null);
                    Long AssetsAssetOperateApplyId = respVO.getId();
                    if (isUpdateSupport) {
                        if (AssetsAssetOperateApplyId != null) {
                            AssetsAssetOperateApplyDO existingAssetOperateApply = AssetsAssetOperateApplyMapper.selectById(AssetsAssetOperateApplyId);
                            if (existingAssetOperateApply != null) {
                                assertOwnerOrPrivileged(existingAssetOperateApply, "导入修改");
                                String existingStatus = normalizeStatus(existingAssetOperateApply);
                                if (!AssetOperateApplyStatus.DRAFT.equals(existingStatus)
                                        && !AssetOperateApplyStatus.REJECTED.equals(existingStatus)
                                        && !AssetOperateApplyStatus.FAILED.equals(existingStatus)) {
                                    throw new ServiceException("当前状态不允许通过导入修改：" + existingStatus);
                                }
                                AssetsAssetOperateApplyMapper.updateById(AssetsAssetOperateApplyDO);
                                successNum++;
                                successMessages.add("ID " + AssetsAssetOperateApplyId + " ");
                            } else {
                                failureNum++;
                                failureMessages.add("ID " + AssetsAssetOperateApplyId + " ");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("ID");
                        }
                    } else {
                        QueryWrapper<AssetsAssetOperateApplyDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", AssetsAssetOperateApplyId);
                        AssetsAssetOperateApplyDO existingAssetOperateApply = AssetsAssetOperateApplyMapper.selectOne(queryWrapper);
                        if (existingAssetOperateApply == null) {
                            AssetsAssetOperateApplyMapper.insert(AssetsAssetOperateApplyDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetOperateApplyId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetOperateApplyId + " ");
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
