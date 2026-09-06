package com.datamaster.module.ontology.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionSubmitReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RowOperateReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RowOperateRespVO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.datamaster.module.ontology.service.IObjectInstanceOperateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

/**
 * 对象实例行操作服务实现
 *
 * <p>内置动作按 (ontologyId + conceptId + actionType) 惰性创建并持久化到 ONT_ACTION（命名前缀「内置-」），
 * 与动作面板配置的动作一致：可被查看/调整审批/绑定审批人/挂 Webhook；已创建的动作被后续行操作复用。
 * 行操作执行记录（ONT_ACTION_EXECUTION）与提交/审批/快照/血缘/回调链路完全复用普通动作。</p>
 */
@Service
@Validated
public class ObjectInstanceOperateServiceImpl implements IObjectInstanceOperateService {

    private static final Logger log = LoggerFactory.getLogger(ObjectInstanceOperateServiceImpl.class);

    /** 内置动作命名前缀（识别用，避免与用户自建动作混淆） */
    private static final String BUILTIN_PREFIX = "内置-";

    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_EXECUTED = "EXECUTED";

    @Resource private ActionMapper actionMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private IActionExecutionService executionService;
    @Resource private ObjectMapper objectMapper;

    @Override
    @Transactional
    public RowOperateRespVO preview(RowOperateReqVO reqVO) {
        ActionDO action = getOrCreateBuiltinAction(reqVO);
        String inputParams = toJson(reqVO.getData());
        ExecutionSubmitReqVO submitReq = new ExecutionSubmitReqVO();
        submitReq.setActionId(action.getId());
        submitReq.setInputParams(inputParams);
        // 行操作有独立的“预览 → 用户确认预览内容”两步交互；PREVIEW_ONLY 只阻止 Worker 抢跑，不代表人工审批。
        submitReq.setTriggerType("PREVIEW_ONLY");
        // 不再整行塞入 objectKey（派生自概念主键属性 + 提交参数，由服务端 deriveObjectKey 统一推导）
        submitReq.setSpaceId(reqVO.getSpaceId());
        submitReq.setSpaceCode(reqVO.getSpaceCode());
        ExecutionRespVO exec = executionService.submitExecution(submitReq);
        return buildPreviewResp(exec);
    }

    @Override
    @Transactional
    public RowOperateRespVO confirm(RowOperateReqVO reqVO) {
        if (reqVO.getExecutionId() == null) {
            throw new RuntimeException("缺少执行记录ID（请先提交预览）");
        }
        ExecutionRespVO exec = executionService.getExecutionById(reqVO.getExecutionId());
        if (exec == null) {
            throw new RuntimeException("执行记录不存在: " + reqVO.getExecutionId());
        }
        String status = exec.getStatus();
        if (STATUS_PENDING.equals(status)) {
            // 行操作不内嵌审批：审批是独立权限层，一律移交审批中心处理（行操作弹框不再代为审批通过）
            RowOperateRespVO resp = new RowOperateRespVO();
            resp.setExecutionId(exec.getId());
            resp.setStatus(status);
            resp.setCanApprove(false);
            resp.setMessage("已提交待审批，请前往审批中心处理本单（行操作弹框不再内嵌审批）");
            return resp;
        }
        if (!STATUS_APPROVED.equals(status)) {
            throw new RuntimeException("当前状态不可确认执行: " + status);
        }
        boolean triggerWebhook = reqVO.getTriggerWebhook() == null || reqVO.getTriggerWebhook();
        ExecutionRespVO executed = executionService.executeExecution(exec.getId(), triggerWebhook);
        return buildExecResp(executed, triggerWebhook);
    }

    /* ---------------- 内置动作惰性创建 ---------------- */

    /**
     * 按 (ontologyId + conceptId + actionType) 查找本概念的内置动作；不存在则创建（免审批默认）。
     * 查询用名称前缀「内置-」限定，避免命中用户自建的同类型动作。
     */
    private ActionDO getOrCreateBuiltinAction(RowOperateReqVO reqVO) {
        String typeLabel = typeLabel(reqVO.getActionType());
        List<ActionDO> existed = actionMapper.selectList(new LambdaQueryWrapper<ActionDO>()
                .eq(ActionDO::getOntologyId, reqVO.getOntologyId())
                .eq(ActionDO::getConceptId, reqVO.getConceptId())
                .eq(ActionDO::getActionType, reqVO.getActionType())
                .likeRight(ActionDO::getName, BUILTIN_PREFIX + typeLabel)
                .last("limit 1"));
        if (existed != null && !existed.isEmpty()) {
            return existed.get(0);
        }
        ConceptDO concept = conceptMapper.selectById(reqVO.getConceptId());
        String conceptName = concept == null ? String.valueOf(reqVO.getConceptId()) : concept.getName();
        ActionDO built = new ActionDO();
        built.setOntologyId(reqVO.getOntologyId());
        built.setName(BUILTIN_PREFIX + typeLabel + "-" + conceptName);
        built.setActionType(reqVO.getActionType());
        built.setConceptId(reqVO.getConceptId());
        built.setNeedsApproval(false);
        built.setApprovalLevels(0);
        built.setDescription("对象管理列表内自动生成的行操作动作（" + typeLabel + "）；可在动作面板调整直接执行/人工确认、确认人和 Webhook 回调");
        actionMapper.insert(built);
        log.info("已创建对象行操作内置动作: actionId={}, type={}, conceptId={}", built.getId(), reqVO.getActionType(), reqVO.getConceptId());
        return built;
    }

    private String typeLabel(String actionType) {
        if ("CREATE".equals(actionType)) {
            return "新增";
        }
        if ("UPDATE".equals(actionType)) {
            return "修改";
        }
        if ("DELETE".equals(actionType)) {
            return "删除";
        }
        throw new RuntimeException("不支持的批量行操作类型: " + actionType);
    }

    /* ---------------- 响应组装 ---------------- */

    private RowOperateRespVO buildPreviewResp(ExecutionRespVO exec) {
        RowOperateRespVO resp = new RowOperateRespVO();
        resp.setExecutionId(exec.getId());
        resp.setStatus(exec.getStatus());
        resp.setCanApprove(false);
        resp.setGeneratedSql(exec.getGeneratedSql());
        resp.setPreviewResult(exec.getPreviewResult());
        if (STATUS_PENDING.equals(exec.getStatus())) {
            // 行操作不内嵌审批：审批移交审批中心；弹框仅提示已提交
            resp.setMessage("已提交待审批，请前往审批中心处理本单");
        } else if (STATUS_APPROVED.equals(exec.getStatus())) {
            resp.setCanApprove(true);
            resp.setMessage("前置检查已通过，请确认当前预览内容后执行（此确认不是审批）");
        } else {
            resp.setMessage("提交结果: " + exec.getStatus());
        }
        return resp;
    }

    private RowOperateRespVO buildExecResp(ExecutionRespVO exec, boolean triggerWebhook) {
        RowOperateRespVO resp = new RowOperateRespVO();
        resp.setExecutionId(exec.getId());
        resp.setStatus(exec.getStatus());
        resp.setCanApprove(false);
        resp.setGeneratedSql(exec.getGeneratedSql());
        resp.setPreviewResult(exec.getPreviewResult());
        resp.setBeforeData(exec.getBeforeData());
        resp.setAfterData(exec.getAfterData());
        resp.setErrorMessage(exec.getErrorMessage());
        resp.setWebhookTriggered(triggerWebhook);
        if (STATUS_EXECUTED.equals(exec.getStatus())) {
            resp.setMessage("执行成功（" + summarize(exec.getPreviewResult()) + "）；回调" + (triggerWebhook ? "已" : "未") + "触发");
        } else {
            resp.setMessage("执行失败: " + exec.getErrorMessage());
        }
        return resp;
    }

    private String summarize(String previewResult) {
        if (StringUtils.isBlank(previewResult)) {
            return "无结果";
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(previewResult);
            if (node != null && node.has("affectedRows")) {
                return "影响 " + node.get("affectedRows").asInt() + " 行";
            }
            if (node != null && node.has("rows")) {
                return "返回 " + node.get("rows").size() + " 行";
            }
        } catch (Exception ignore) {
            // 非结构化结果原样展示
        }
        return previewResult.length() > 120 ? previewResult.substring(0, 120) + "…" : previewResult;
    }

    private String toJson(Object v) {
        if (v == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(v);
        } catch (Exception e) {
            log.warn("行操作数据序列化失败，按空参数处理: {}", e.getMessage());
            return "{}";
        }
    }
}
