package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ontology.controller.admin.action.vo.ApprovalChainRespVO;
import com.datamaster.module.ontology.dal.dataobject.ActionApprovalRequestDO;
import com.datamaster.module.ontology.dal.dataobject.ActionApprovalReviewerDO;
import com.datamaster.module.ontology.dal.dataobject.ActionApprovalTaskDO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.mapper.ActionApprovalRequestMapper;
import com.datamaster.module.ontology.dal.mapper.ActionApprovalReviewerMapper;
import com.datamaster.module.ontology.dal.mapper.ActionApprovalTaskMapper;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.service.IActionApprovalService;
import com.datamaster.module.system.api.message.dto.MessageSaveReqDTO;
import com.datamaster.module.system.service.ISysMessageService;
import com.datamaster.module.system.service.ISysUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.ValidationException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对象绑定动作审批服务实现（Palantir Approvals+Checkpoint 折叠进对象执行记录）
 *
 * <p>① 提交时判定（Submission Criteria）：对 Action.submissionCriteria 表达式求值，
 * 输入 = 提交参数 + 目标对象主键 + 内置上下文（@currentUser/@now），输出判定 JSON。
 * <p>人工确认：请求(ONT_ACTION_REQUEST) → 唯一确认任务(ONT_ACTION_TASK) → 确认结论审计(ONT_ACTION_REVIEWER)。
 * 确认意见可选，确认结论只追加并永久留痕。
 */
@Service
public class ActionApprovalServiceImpl implements IActionApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ActionApprovalServiceImpl.class);

    public static final String DECISION_PASS = "PASS";
    public static final String DECISION_REJECT = "REJECT";
    /** 评估错误：条件表达式无法解析/求值失败，fail-closed 拒绝且不得当成 REJECT 的普通拒绝（需人工介入修正） */
    public static final String DECISION_EVALUATION_ERROR = "EVALUATION_ERROR";

    public static final String REQ_STATUS_PENDING = "PENDING";
    public static final String REQ_STATUS_APPROVED = "APPROVED";
    public static final String REQ_STATUS_REJECTED = "REJECTED";

    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_APPROVED = "APPROVED";
    public static final String TASK_STATUS_REJECTED = "REJECTED";

    @Resource private ActionApprovalRequestMapper requestMapper;
    @Resource private ActionApprovalTaskMapper taskMapper;
    @Resource private ActionApprovalReviewerMapper reviewerMapper;
    @Resource private ActionMapper actionMapper;
    @Resource private ISysMessageService iSysMessageService;
    @Resource private ISysUserService userService;
    @Resource private ObjectMapper objectMapper;
    @Resource private ObjectStateConditionResolver objectStateResolver;

    /** 动作审批待办站内消息模板 ID（message_template 表，初始化脚本已插入） */
    private static final Long APPROVAL_TODO_TEMPLATE_ID = 8L;

    /* ---------------- ① 提交时判定 ---------------- */

    @Override
    public String evaluateCriteria(ActionDO action, String inputParams, String objectKey) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("decision", DECISION_PASS);
        result.put("passed", true);
        ArrayNode detail = result.putArray("detail");

        String criteriaJson = action == null ? null : action.getSubmissionCriteria();
        if (criteriaJson == null || criteriaJson.trim().isEmpty()) {
            return result.toString(); // 无条件 → 直接通过（是否需审批由 needsApproval/approvalLevels 决定）
        }

        Map<String, Object> params = parseParams(inputParams);
        Map<String, Object> ctx = buildContext();

        try {
            JsonNode criteria = objectMapper.readTree(criteriaJson);
            JsonNode conditions = criteria.has("conditions") ? criteria.get("conditions") : null;
            boolean logicAnd = !criteria.has("logic") || !"OR".equalsIgnoreCase(criteria.path("logic").asText());
            boolean allPassed = true;
            if (conditions != null && conditions.isArray()) {
                List<Boolean> matches = new ArrayList<>();
                for (JsonNode cond : conditions) {
                    boolean m = evaluateCondition(cond, action, params, objectKey, ctx, detail);
                    matches.add(m);
                }
                allPassed = logicAnd
                        ? matches.stream().allMatch(Boolean::booleanValue)
                        : matches.stream().anyMatch(Boolean::booleanValue);
            }
            result.put("passed", allPassed);
            result.put("decision", allPassed ? DECISION_PASS : DECISION_REJECT);
        } catch (Exception e) {
            // fail-closed：条件解析/评估异常 → 决策 EVALUATION_ERROR，绝不当「通过」放行
            // （修复原先 catch 后 passed=true 的 fail-open 缺陷，保证不因配置错误而误执行）
            log.warn("提交判定表达式求值失败，按评估错误拒绝: actionId={}, err={}", action.getId(), e.getMessage());
            result.put("passed", false);
            result.put("decision", DECISION_EVALUATION_ERROR);
            ObjectNode err = detail.addObject();
            err.put("error", e.getMessage());
        }
        return result.toString();
    }

    /** 判定单条条件；将比对结果写入 detail，返回是否匹配。 */
    private boolean evaluateCondition(JsonNode cond, ActionDO action, Map<String, Object> params, String objectKey,
                                       Map<String, Object> ctx, ArrayNode detail) {
        String field = cond.path("field").asText(null);
        String op = extractOp(cond);
        ObjectNode d = detail.addObject();
        if (field == null || field.trim().isEmpty()) {
            d.put("error", "condition.field 缺失");
            return false;
        }
        Object actual = resolveField(field, action, params, objectKey, ctx);
        Object expected = cond.has("value") ? nodeToValue(cond.get("value")) : null;
        boolean match = compare(actual, op, expected);
        d.put("field", field);
        d.put("op", op);
        if (expected != null) d.put("value", expected instanceof BigDecimal ? expected.toString() : String.valueOf(expected));
        if (actual != null) d.put("actual", actual.toString());
        d.put("match", match);
        return match;
    }

    /** 提取条件操作符：operator 优先，回退 op，最后默认 eq。 */
    private String extractOp(JsonNode cond) {
        if (cond.hasNonNull("operator")) {
            return cond.get("operator").asText("eq");
        }
        if (cond.hasNonNull("op")) {
            return cond.get("op").asText("eq");
        }
        return "eq";
    }

    /**
     * 解析判定中的取值来源：objectKey / param.* / @内置上下文 / object.*。
     * object.*（对象当前状态，含 object.&lt;relation&gt;.&lt;prop&gt; 跨对象引用）经物理表回查：
     * 任何解析失败抛异常（fail-closed），由 evaluateCriteria 统一转为 EVALUATION_ERROR 决策。
     */
    private Object resolveField(String field, ActionDO action, Map<String, Object> params,
                                String objectKey, Map<String, Object> ctx) {
        if ("objectKey".equals(field)) {
            return objectKey;
        }
        if (field.startsWith("param.")) {
            Object v = params.get(field.substring("param.".length()));
            return v == null ? null : String.valueOf(v);
        }
        if (field.startsWith("@")) {
            Object v = ctx.get(field);
            return v == null ? null : String.valueOf(v);
        }
        if (objectStateResolver.isObjectField(field)) {
            // object.* 物理表回查；对象/属性/关系/表任一缺失会抛异常 → fail-closed
            Object v = objectStateResolver.resolve(field, action, objectKey);
            return v;
        }
        // 未知字段来源：按 fail-closed 处理（不得静默当 null，避免误判通过）
        throw new IllegalArgumentException("未知的判定字段来源: " + field);
    }

    private boolean compare(Object actual, String op, Object expected) {
        boolean bothNum = isNumeric(actual) && isNumeric(expected);
        try {
            if (bothNum) {
                BigDecimal a = new BigDecimal(String.valueOf(actual));
                BigDecimal b = new BigDecimal(String.valueOf(expected));
                return cmpNumber(op, a.compareTo(b));
            }
        } catch (Exception ignore) {
            // 回退字符串比较
        }
        String a = actual == null ? "" : String.valueOf(actual);
        String b = expected == null ? "" : String.valueOf(expected);
        switch (op) {
            case "eq": return a.equals(b);
            case "ne": return !a.equals(b);
            case "contains": return a.contains(b);
            case "in": return split(b).stream().anyMatch(x -> x.equals(a));
            case "gt": return a.compareTo(b) > 0;
            case "ge": return a.compareTo(b) >= 0;
            case "lt": return a.compareTo(b) < 0;
            case "le": return a.compareTo(b) <= 0;
            default: return false;
        }
    }

    private boolean cmpNumber(String op, int c) {
        switch (op) {
            case "eq": return c == 0;
            case "ne": return c != 0;
            case "gt": return c > 0;
            case "ge": return c >= 0;
            case "lt": return c < 0;
            case "le": return c <= 0;
            default: return false;
        }
    }

    private boolean isNumeric(Object v) {
        if (v == null) return false;
        try { new BigDecimal(String.valueOf(v).trim()); return true; } catch (Exception e) { return false; }
    }

    private List<String> split(String s) {
        List<String> r = new ArrayList<>();
        if (s == null || s.isEmpty()) return r;
        for (String p : s.split(",")) { if (!p.trim().isEmpty()) r.add(p.trim()); }
        return r;
    }

    private Object nodeToValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isNumber()) return node.decimalValue();
        return node.asText();
    }

    private Map<String, Object> parseParams(String inputParams) {
        Map<String, Object> map = new HashMap<>();
        if (inputParams == null || inputParams.trim().isEmpty()) return map;
        try {
            JsonNode node = objectMapper.readTree(inputParams);
            if (node != null && node.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    JsonNode v = e.getValue();
                    map.put(e.getKey(), v.isNumber() ? v.decimalValue() : v.asText());
                }
            }
        } catch (Exception e) {
            log.warn("解析提交参数失败，判定按空参数处理: {}", e.getMessage());
        }
        return map;
    }

    /** 内置上下文：@currentUser / @now。取当前登录用户失败时降级为空白，不阻断提交。 */
    private Map<String, Object> buildContext() {
        Map<String, Object> ctx = new HashMap<>();
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null && loginUser.getUser() != null) {
                SysUser user = loginUser.getUser();
                ctx.put("@currentUser", user.getUserName());
            }
        } catch (Exception e) {
            log.debug("取当前用户失败（非请求上下文？），@currentUser 置空");
        }
        ctx.put("@now", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        return ctx;
    }

    /* ---------------- 单次人工确认 ---------------- */

    @Override
    @Transactional
    public void initChain(ActionExecutionDO exec, ActionDO action) {
        // 当前产品语义固定为一次人工确认；历史多级定义也只创建一个审批任务。
        int levels = 1;
        LoginUser loginUser = currentLogin();
        Long userId = loginUser == null || loginUser.getUser() == null ? null : loginUser.getUser().getUserId();
        String submitterName = loginUser == null || loginUser.getUser() == null ? "" : loginUser.getUser().getUserName();

        ActionApprovalRequestDO request = ActionApprovalRequestDO.builder()
                .actionExecutionId(exec.getId())
                .ontologyId(action.getOntologyId())
                .actionId(action.getId())
                .objectKey(exec.getObjectKey())
                .totalStages(levels)
                .currentStage(1)
                .status(REQ_STATUS_PENDING)
                .requestBy(userId)
                .requestTime(new Date())
                .build();
        requestMapper.insert(request);

        // 唯一确认人来自动作定义 approvalReviewers；未配置时任意登录用户可确认。
        List<Map<String, Object>> reviewers = parseApprovalReviewers(action.getApprovalReviewers());
        for (int i = 1; i <= levels; i++) {
            Long approverId = null;
            String approverName = null;
            for (Map<String, Object> r : reviewers) {
                if (i == toInt(r.get("stage"))) {
                    approverId = toLong(r.get("userId"));
                    approverName = r.get("userName") == null ? null : String.valueOf(r.get("userName"));
                    break;
                }
            }
            ActionApprovalTaskDO task = ActionApprovalTaskDO.builder()
                    .requestId(request.getId())
                    .stageNo(i)
                    .status(TASK_STATUS_PENDING)
                    .requiredApprovals(1)
                    .approvedCount(0)
                    .approverId(approverId)
                    .approverName(approverName)
                    .build();
            taskMapper.insert(task);
            // 给指定确认人发送站内待办消息。
            if (i == 1 && approverId != null) {
                sendApprovalTodo(approverId, action == null ? "" : action.getName(), exec.getObjectKey(),
                        1, submitterName, formatTime(exec.getCreateTime()));
            }
        }
    }

    @Override
    @Transactional
    public String submitReviewerDecision(Long executionId, boolean approve, String reason) {
        ActionApprovalRequestDO request = requestMapper.selectActiveByExecutionId(executionId);
        if (request == null) {
            return null; // 无确认任务（旧数据兼容路径）
        }
        String reqStatus = request.getStatus();
        if (REQ_STATUS_APPROVED.equals(reqStatus) || REQ_STATUS_REJECTED.equals(reqStatus)) {
            throw new RuntimeException("审批流已结束（" + reqStatus + "），不可再审批");
        }
        ActionApprovalTaskDO task = taskMapper.selectCurrentPending(request.getId(), request.getCurrentStage());
        if (task == null) {
            throw new RuntimeException("审批流无待审关卡: stage=" + request.getCurrentStage());
        }
        // 当前审阅人（请求上下文）
        LoginUser loginUser = currentLogin();
        if (loginUser == null || loginUser.getUser() == null) {
            throw new RuntimeException("无法识别当前审批人");
        }
        Long reviewerId = loginUser.getUser().getUserId();
        String reviewerName = loginUser.getUser().getUserName();

        // 关卡绑定审批人校验：配置了审批人的关卡仅该审批人可审批（未绑定=任意登录用户可审，兼容旧数据）
        if (task.getApproverId() != null && !task.getApproverId().equals(reviewerId)) {
            throw new RuntimeException("当前关卡指定审批人为「" + (StringUtils.isBlank(task.getApproverName()) ? task.getApproverId() : task.getApproverName()) + "」，您无权审批");
        }

        if (reviewerMapper.existsReviewerDecision(task.getId(), reviewerId)) {
            throw new RuntimeException("当前审阅人已对本关卡作过决策，不可重复审批");
        }

        // 永久审计：只追加，决策后不改
        ActionApprovalReviewerDO decision = ActionApprovalReviewerDO.builder()
                .taskId(task.getId())
                .requestId(request.getId())
                .reviewerId(reviewerId)
                .reviewerName(reviewerName)
                .decision(approve ? "APPROVE" : "REJECT")
                .reason(reason == null ? "" : reason.trim())
                .decideTime(new Date())
                .build();
        reviewerMapper.insert(decision);

        if (!approve) {
            // 任一级拒绝 → 整个审批流终止
            task.setStatus(TASK_STATUS_REJECTED);
            taskMapper.updateById(task);
            request.setStatus(REQ_STATUS_REJECTED);
            requestMapper.updateById(request);
            return REQ_STATUS_REJECTED;
        }

        int approvedCount = (task.getApprovedCount() == null ? 0 : task.getApprovedCount()) + 1;
        task.setApprovedCount(approvedCount);
        int required = task.getRequiredApprovals() == null ? 1 : task.getRequiredApprovals();
        if (approvedCount >= required) {
            task.setStatus(TASK_STATUS_APPROVED);
            taskMapper.updateById(task);
            int nextStage = request.getCurrentStage() + 1;
            if (nextStage > request.getTotalStages()) {
                request.setStatus(REQ_STATUS_APPROVED);
                requestMapper.updateById(request);
                return REQ_STATUS_APPROVED; // 全部关卡完成 → 可执行
            }
            request.setCurrentStage(nextStage);
            requestMapper.updateById(request);
            notifyNextStageApprover(request, nextStage);
            return REQ_STATUS_PENDING; // 推进到下一关
        }
        taskMapper.updateById(task);
        return REQ_STATUS_PENDING; // 本关仍需更多审阅人同意
    }

    @Override
    public ApprovalChainRespVO getChain(Long executionId) {
        List<ActionApprovalRequestDO> requests = requestMapper.selectByExecutionId(executionId);
        if (requests == null || requests.isEmpty()) {
            return null;
        }
        ActionApprovalRequestDO request = requests.get(requests.size() - 1); // 最近一次审批请求
        ApprovalChainRespVO vo = new ApprovalChainRespVO();
        vo.setRequestId(request.getId());
        vo.setActionExecutionId(request.getActionExecutionId());
        vo.setObjectKey(request.getObjectKey());
        vo.setTotalStages(request.getTotalStages());
        vo.setCurrentStage(request.getCurrentStage());
        vo.setStatus(request.getStatus());
        vo.setRequestTime(request.getRequestTime());

        List<ActionApprovalTaskDO> tasks = taskMapper.selectByRequestId(request.getId());
        if (tasks != null) {
            for (ActionApprovalTaskDO task : tasks) {
                ApprovalChainRespVO.TaskVO tv = new ApprovalChainRespVO.TaskVO();
                tv.setTaskId(task.getId());
                tv.setStageNo(task.getStageNo());
                tv.setStatus(task.getStatus());
                tv.setRequiredApprovals(task.getRequiredApprovals());
                tv.setApprovedCount(task.getApprovedCount());
                tv.setApproverId(task.getApproverId());
                tv.setApproverName(task.getApproverName());
                List<ActionApprovalReviewerDO> reviewers = reviewerMapper.selectByTaskId(task.getId());
                if (reviewers != null) {
                    for (ActionApprovalReviewerDO r : reviewers) {
                        ApprovalChainRespVO.ReviewerVO rv = new ApprovalChainRespVO.ReviewerVO();
                        rv.setReviewerId(r.getReviewerId());
                        rv.setReviewerName(r.getReviewerName());
                        rv.setDecision(r.getDecision());
                        rv.setReason(r.getReason());
                        rv.setDecideTime(r.getDecideTime());
                        tv.getReviewers().add(rv);
                    }
                }
                vo.getTasks().add(tv);
            }
        }
        return vo;
    }

    @Override
    public boolean canApprove(Long executionId) {
        ActionApprovalRequestDO request = requestMapper.selectActiveByExecutionId(executionId);
        if (request == null || !REQ_STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        ActionApprovalTaskDO task = taskMapper.selectCurrentPending(request.getId(), request.getCurrentStage());
        if (task == null) {
            return false;
        }
        if (task.getApproverId() == null) {
            return true; // 关卡未绑定审批人=任意登录用户可审（兼容旧数据）
        }
        LoginUser loginUser = currentLogin();
        if (loginUser == null || loginUser.getUser() == null) {
            return false;
        }
        return task.getApproverId().equals(loginUser.getUser().getUserId());
    }

    /* ---------------- 审批待办站内消息 ---------------- */

    /**
     * 给指定关卡审批人发送「动作审批待办」站内消息（上线红点提示）。
     * 最佳努力：失败仅告警，绝不阻断审批主流程。
     */
    private void sendApprovalTodo(Long receiverId, String actionName, String objectKey, int stage,
                                  String submitter, String submitTime) {
        if (receiverId == null) {
            return;
        }
        try {
            MessageSaveReqDTO dto = new MessageSaveReqDTO();
            dto.setSenderId(1L);
            dto.setCreatorId(1L);
            dto.setCreateBy("系统");
            dto.setReceiverId(receiverId);
            dto.setModule(0);
            dto.setEntityType(0);
            dto.setEntityUrl("/ont/workspace");
            Map<String, Object> map = new HashMap<>();
            map.put("actionName", actionName == null ? "" : actionName);
            map.put("objectKey", objectKey == null ? "" : objectKey);
            map.put("stage", stage);
            map.put("submitter", submitter == null ? "" : submitter);
            map.put("submitTime", submitTime == null ? "" : submitTime);
            iSysMessageService.send(APPROVAL_TODO_TEMPLATE_ID, dto, map);
        } catch (Exception e) {
            log.warn("发送动作审批待办消息失败, receiverId={}", receiverId, e);
        }
    }

    /** 审批流推进到下一关后，给下一关指定审批人发待办消息。 */
    private void notifyNextStageApprover(ActionApprovalRequestDO request, int nextStage) {
        try {
            ActionApprovalTaskDO nextTask = taskMapper.selectCurrentPending(request.getId(), nextStage);
            if (nextTask == null || nextTask.getApproverId() == null) {
                return;
            }
            ActionDO action = request.getActionId() == null ? null : actionMapper.selectById(request.getActionId());
            String submitter = "";
            if (request.getRequestBy() != null) {
                SysUser submitterUser = userService.selectUserById(request.getRequestBy());
                if (submitterUser != null) {
                    submitter = submitterUser.getUserName();
                }
            }
            sendApprovalTodo(nextTask.getApproverId(),
                    action == null ? "" : action.getName(),
                    request.getObjectKey(),
                    nextStage,
                    submitter,
                    formatTime(request.getRequestTime()));
        } catch (Exception e) {
            log.warn("推进审批关卡后通知审批人失败, requestId={}", request.getId(), e);
        }
    }

    /* ---------------- 动作审批人配置解析 ---------------- */

    /** 解析动作 approvalReviewers（JSON数组 [{"stage":1,"userId":1001,"userName":"张三"}]），非法配置静默跳过。 */
    private List<Map<String, Object>> parseApprovalReviewers(String json) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (StringUtils.isBlank(json)) {
            return result;
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr) {
                    if (n == null || !n.isObject()) {
                        continue;
                    }
                    Map<String, Object> m = new HashMap<>();
                    m.put("stage", n.path("stage").asInt(0));
                    m.put("userId", n.path("userId").asLong(0));
                    m.put("userName", n.path("userName").asText(null));
                    result.add(m);
                }
            }
        } catch (Exception e) {
            log.warn("解析动作审批人配置失败: {}", e.getMessage());
        }
        return result;
    }

    private int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        try {
            return ((Number) v).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return ((Number) v).longValue();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private LoginUser currentLogin() {
        try {
            return SecurityUtils.getLoginUser();
        } catch (Exception e) {
            return null;
        }
    }
}
