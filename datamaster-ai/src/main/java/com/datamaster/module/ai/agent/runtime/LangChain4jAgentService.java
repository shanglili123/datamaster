package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolCatalog;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.config.AiSkillModelProperties;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskMessageRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskMessageWindowRespVO;
import com.datamaster.module.ai.controller.agent.tool.GenerateActionsTool;
import com.datamaster.module.ai.controller.agent.tool.GetDatasourceTool;
import com.datamaster.module.ai.controller.agent.tool.AgentToolResourceGuard;
import com.datamaster.module.ai.controller.agent.tool.ListAssetsTool;
import com.datamaster.module.ai.controller.agent.tool.CreateDataServiceTool;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.ai.controller.agent.tool.SearchOntologiesTool;
import com.datamaster.module.ai.controller.agent.tool.CreateActionTool;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import com.datamaster.module.ai.service.skill.IAiAskSessionService;
import com.datamaster.common.utils.AesEncryptUtil;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.exception.ServiceException;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LangChain4j Agent 最小运行时：模型负责决定下一步 Tool，平台负责执行和安全边界。
 */
@Service
@Slf4j
public class LangChain4jAgentService implements IAutonomousAgentService {

    private static final String SYSTEM_PROMPT =
            "你是 DataMaster 平台数据智能体。你可以调用平台 Tool 完成数据源、元数据探查、资产、本体和 Skill 工作。"
                    + "所有业务 ID 和 ID 列表元素必须按工具 Schema 使用带引号的完整字符串；原样复制 Tool 返回的 ID，禁止转换数字、截断、四舍五入或科学计数法。"
                    + "严禁自行编造 ds-xxx、table-xxx、ontology-xxx 等示例或占位符；如果没有真实 ID，必须先调用查询 Tool，或使用查询 Tool 支持的名称字段。"
                    + "创建成功但后续发布/执行失败时，应使用创建结果中的真实 ID 继续，不得宣称未创建或重复创建。"
                    + "必须先查询和确认上下文，再调用写入 Tool；不能执行任意 SQL，不能绕过权限、审批和动作执行引擎。"
                    + "如果 Tool 返回 NEED_INPUT 或 NEED_CONFIRMATION，立即停止并向用户说明需要补充或确认的内容。"
                    + "当用户说“与某已有数据源连接信息相同”时，不要把密码放进 datasourceConfig，也不要只传空对象；"
                    + "必须先调用 get_datasource(datasourceName=源数据源名称)，向用户展示类型、IP、端口和数据库名并询问是否一致；"
                    + "只有用户明确回复一致/确认后，才调用 create_datasource，并把源名称填入 sourceDatasourceName，由平台后台复制受控配置。"
                    + "复制数据源时必须明确提供目标 databaseName；不能只改 datasourceName 冒充修改连接。PostgreSQL 目标 schema 默认使用 public。"
                    + "update_datasource 返回 NEED_INPUT、FAILED 或校验失败时必须停止重试并向用户说明，不得循环重复调用；只有返回 verified=true 才能说更新成功。"
                    + "任何 Tool 返回“无效资源 ID”或“数据源不存在”时，停止重复调用该 ID，改为按名称查询并向用户展示可选资源。"
                    + "当用户用名称、编码或自然语言指代本体、动作或流程而当前上下文没有真实 ID 时，可自主选择对应的查询 Tool（例如 search_ontologies、list_actions、list_action_workflows）；查询结果唯一时继续使用真实 ID，多条时向用户展示候选并请求选择，不能编造 ID，也不能把查询步骤写死成固定链路。"
                    + "search_ontologies 返回 total=0 不能直接断言当前空间没有本体，也不能因此擅自创建或生成新本体；应先检查名称/编码条件、当前空间上下文，必要时去掉名称条件重新查询。"
                    + "create_*、update_*、delete_* Tool 只在用户明确要求新增、修改或删除时调用；查询、生成预览、提交执行请求和真正执行分别使用各自 Tool，不能用一个 Tool 冒充另一个阶段。"
                    + "如果上一轮同一 Agent 会话上下文已经有 datasourceName 或 datasourceId，用户随后说“开始探查元数据”或“继续探查”，必须把该数据源视为当前目标；先用 get_datasource 回读，再使用真实 datasourceId 和 databaseName 查询或复用探查任务。不要再次列出全部数据源要求用户选择，也不要因为任务存在就自动发布或执行。"
                    + "如果当前操作已经记录 datasourceId/datasourceName，用户说“决策数据源”“决策数据库”“决策库”或“当前数据源”时，这些是当前目标数据源的语义别名，不是新数据源名称；必须直接使用当前真实数据源，不得要求用户确认是否另有 decision_db/ai_decision，也不得因为名称不含 decision 而暂停。"
                    + "查询或根据实际数据生成内容时，不要从历史消息猜测 datasourceId；优先使用当前操作记录中的实际目标数据源，或先按名称调用 get_datasource。"
                    + "日常数据问答、指标分析和报告请提示用户进入AI问数；此入口只建设平台流程，不调用DB-GPT或指标分析工具。"
                    + "用户要求查询已发布的数据服务时，先用 list_data_services 或 get_data_service 确认当前空间内的真实 serviceId 和请求字段，再调用 invoke_data_service；不得把数据服务 ID、SQL、连接配置或密钥交给模型自行编造。invoke_data_service 只支持表单查询型已发布服务，返回结果后再进行总结。"
                    + "用户要求创建订单、客户订单或其他数据接口服务时，不要因为 list_data_services 为空就结束；先用当前真实 datasourceId 调用 list_assets 搜索订单相关表。候选唯一且表结构可用时，调用 create_data_service 创建只读表单查询服务草稿；如果候选为空或有多个，才向用户说明并请求选择。create_data_service 成功后明确告知草稿 serviceId 和接口路径，不要自动发布，等待用户明确要求发布。"
                    + "用户明确要求发布某个数据服务草稿时，先用 get_data_service 回读真实 serviceId 和契约，再调用 publish_data_service；发布是独立写操作，必须要求用户确认同一个 serviceId。"
                    + "用户要求查看数据开发任务时，使用 list_data_development_tasks 或 get_data_development_task；需要设计或修改节点编排时，先使用 preview_data_development_plan 做只读校验，用户确认后才使用 apply_data_development_plan 保存草稿；只有用户明确确认启动时，才调用 start_data_development_task。数据开发任务的节点编排、发布参数和调度策略必须以后续专用 Tool 返回的真实信息为准，不能编造 taskId、节点 ID 或 DolphinScheduler 参数。"
                    + "流程必须由当前事实和用户目标驱动，而不是机械套用固定顺序。只有用户明确要求生成或刷新本体模型时，才需要考虑探查任务；生成动作、生成动作草稿、根据已有本体和已采集数据生成动作时，严禁调用 get_probe_task、create_probe_task、publish_probe_task 或 run_probe_task，直接使用当前 operation 的 ontologyId 调用 generate_actions。涉及本体生成时，先查询或复用探查任务，并查看 get_probe_task 返回的最近一次执行状态、时间和统计；仅在没有成功执行、最近执行失败/仍在执行、用户明确要求刷新，或有证据表明元数据可能过期时，才向用户说明原因并请求确认后调用 publish_probe_task/run_probe_task。已有成功且满足当前目标的探查结果时，跳过探查，直接进入下一步。"
                    + "严格区分本体与动作：用户说生成本体、概念、属性或关系时才调用 generate_ontology；用户说生成动作、生成动作草稿、查询动作定义、分析动作定义、更新动作定义或动作预览时，必须调用 generate_actions，它需要 ontologyId 和 prompt，但不需要 actionId；只有用户明确要求提交、试运行或执行某个已经存在的动作，并且已有真实 actionId 时，才调用 submit_action。不得用 submit_action 处理‘生成动作’。用户说生成动作编排、动作流程或工作流时才调用 generate_action_workflow。generate_ontology 的确认文案必须称为‘生成本体模型’，不能称为‘生成本体动作’。页面上下文中若包含 ontologyId，它代表用户所说的‘当前本体’，优先级高于历史会话中的 ontologyId。"
                    + "generate_actions 返回动作预览后，保存必须分阶段进行：用户确认后对每个预览逐个调用 create_action，传入 ontologyId、预览中的 name/actionType 和从 0 开始的 previewIndex；不要在确认阶段回显 functionBody 或整段 executionSteps，也不要一次调用保存全部动作。create_action 会从最近一次预览解析真实概念/关系 ID，并为 FUNCTION 预览创建共享函数后再绑定动作。"
                    + "若用户明确要求完整标准接入，优先调用 run_standard_data_onboarding，但仍须根据各步骤返回的实际状态跳过已完成步骤，不得重复创建或重复执行。"
                    + "指标建设属于流程能力：用preview_metrics基于真实概念和字段生成候选预览，业务口径不明确时要求补充；再用save_metric_draft请求用户确认并保存单个草稿。"
                    + "默认不发布指标、不执行指标查询、不创建ADS物理表。预览不是经过确认的正式口径，不能把候选聚合结果当作已验证指标。";

    @Resource
    private AgentToolCatalog toolCatalog;

    @Resource
    private AiSkillModelProperties modelProperties;
    @Resource
    private com.datamaster.module.ai.service.gateway.ActiveModelResolver activeModelResolver;
    @Resource
    private com.datamaster.module.ai.service.gateway.AiInvocationGateway invocationGateway;

    @Resource
    private IAiAgentOperationService operationService;

    @Resource
    private PersistentAgentChatMemoryStore memoryStore;
    @Resource
    private IAiAskSessionService askSessionService;
    @Resource
    private GenerateActionsTool generateActionsTool;
    @Resource
    private GetDatasourceTool getDatasourceTool;
    @Resource
    private IAssetsDatasourceService datasourceService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Resource
    private ListAssetsTool listAssetsTool;
    @Resource
    private CreateDataServiceTool createDataServiceTool;
    @Resource
    private SearchOntologiesTool searchOntologiesTool;
    @Resource
    private CreateActionTool createActionTool;
    @Autowired(required = false)
    private ISpaceApi spaceApi;
    private final Map<String, ChatMemory> memories = new ConcurrentHashMap<>();

    @Override
    public AgentRunResponse run(AgentRunRequest request, AgentToolContext context) {
        boolean resumed = request != null && StringUtils.isBlank(request.getGoal())
                && StringUtils.isNotBlank(request.getOperationId());
        resumed = resumed || isContinuation(context) || Boolean.TRUE.equals(request == null ? null : request.getContinuation());
        // 前端聊天框里的“继续啊/下一步”等自然语言续接不会带 continuation 标记，
        // 但会复用当前 operationId。若不在这里识别，它会被误当成新问题再次交给
        // 模型，部分供应商会在这条无关请求上生成非法 function.arguments。
        boolean naturalContinuation = request != null
                && StringUtils.isNotBlank(request.getOperationId())
                && isNaturalContinuationRequest(request.getGoal());
        resumed = resumed || naturalContinuation;
        // 前端确认按钮会保留用户可读的确认文本（例如“保存该排产动作定义”），
        // 因此 goal 并不为空；confirmed=true + operationId 仍然必须走确认续跑分支。
        resumed = resumed || (request != null && Boolean.TRUE.equals(request.getConfirmed())
                && StringUtils.isNotBlank(request.getOperationId()));
        request = resolveSavedRequest(request, context);
        if (request == null || StringUtils.isBlank(request.getGoal())) {
            return AgentRunResponse.failure("缺少 Agent 目标");
        }
        String conversationId = StringUtils.isBlank(request.getConversationId())
                ? (request.getSessionId() == null ? UUID.randomUUID().toString()
                : "agent-session-" + request.getSessionId()) : request.getConversationId();
        String operationId = context == null ? request.getOperationId() : context.getOperationId();
        if (StringUtils.isBlank(operationId)) operationId = UUID.randomUUID().toString();
        request.setOperationId(operationId);
        request.setConversationId(conversationId);
        ensureOperation(operationId, request, context);
        AgentToolContext executionContext = context == null ? null : context.withConversationId(conversationId);
        AgentRunResponse metricDraft = confirmMetricDraft(request, executionContext, operationId, conversationId);
        if (metricDraft != null) return metricDraft;
        // Data Agent 仅建设平台资源和指标草稿；问数与指标分析在独立问数入口运行。
        final java.util.Set<String> routedTools = toolCatalog.managementToolNames();
        ChatLanguageModel model;
        try (com.datamaster.module.ai.service.gateway.AiInvocationContext.Scope ignored =
                     com.datamaster.module.ai.service.gateway.AiInvocationContext.scope(
                             new com.datamaster.module.ai.service.gateway.AiInvocationGateway.Context(
                                     executionContext == null ? null : executionContext.getUserId(),
                                     executionContext == null ? null : executionContext.getSpaceId(),
                                     executionContext == null ? null : executionContext.getSpaceCode(), operationId))) {
            model = createModel();
        }
        if (model == null) {
            AgentRunResponse failure = AgentRunResponse.failure("决策智能体模型未启用或未配置 API 地址");
            failure.setOperationId(operationId);
            failure.setConversationId(conversationId);
            persistOperation(operationId, request, failure);
            return failure;
        }

        boolean explicitlyConfirmed = context != null && context.getAttributes() != null
                && (Boolean.TRUE.equals(context.getAttributes().get("confirmed"))
                || "true".equalsIgnoreCase(String.valueOf(context.getAttributes().get("confirmed"))));
        // 自主 Agent 的页面 sessionId 是会话记忆的主键；conversationId 仍保留给
        // API 响应和 DB-GPT 子 Tool，避免页面 localStorage 丢失时产生另一份记忆。
        String memoryId = buildAgentMemoryId(request, context, conversationId);
        ChatMemory memory = memories.computeIfAbsent(memoryId,
                id -> MessageWindowChatMemory.builder().id(id).maxMessages(24).chatMemoryStore(memoryStore).build());
        // MessageWindowChatMemory replaces the existing system message, preserving
        // conversation history while applying the latest Tool contract to old sessions.
        memory.add(SystemMessage.from(SYSTEM_PROMPT));
        synchronizePageSession(memory, request, context);
        AgentRunResponse result = new AgentRunResponse();
        if (resumed) {
            AiAgentOperationDO savedOperation = operationService.get(operationId);
            if (savedOperation != null && StringUtils.isNotBlank(savedOperation.getResultJson())) {
                AgentRunResponse savedResult = JSON.parseObject(savedOperation.getResultJson(), AgentRunResponse.class);
                if (savedResult != null && savedResult.getToolResults() != null) result.setToolResults(savedResult.getToolResults());
            }
        }
        result.setConversationId(conversationId);
        result.setSessionId(request.getSessionId());
        result.setOperationId(operationId);
        result.setSuccess(true);
        result.setStatus(AgentToolResult.RUNNING);
        result.setMessage("Agent 正在自主调用平台 Tool");

        // 用户把“决策数据库/决策数据源”作为业务语义引用时，先从当前空间
        // 的真实数据源目录解析唯一的决策库，避免模型把它误判成待新建的数据源。
        bindDecisionDatasourceAlias(request, operationId, executionContext);

        AgentRunResponse deterministicOrderService = runDecisionOrderServiceFlow(
                request, operationId, executionContext, result, explicitlyConfirmed);
        if (deterministicOrderService != null) return deterministicOrderService;

        AgentRunResponse currentDatasource = resolveCurrentDatasourceAlias(request, operationId,
                executionContext, result);
        if (currentDatasource != null) return currentDatasource;

        if (naturalContinuation) {
            AgentRunResponse continuedAction = continueActionPreview(operationId, request, executionContext, result, memory);
            if (continuedAction != null) return continuedAction;
        }

        String prompt;
        if (!resumed) {
            prompt = request.getGoal().trim();
        } else if (explicitlyConfirmed) {
            // 确认按钮必须保留用户刚刚给出的写入参数（例如 ontologyId、previewIndex、name、actionType）。
            // 旧逻辑只发送泛化的“继续执行”文本，模型拿不到 create_action 参数时会错误地再次索要参数。
            String confirmationGoal = request.getGoal();
            if (StringUtils.isNotBlank(confirmationGoal)
                    && !confirmationGoal.trim().startsWith("用户已明确确认上一轮待确认操作")) {
                prompt = "用户已经明确确认执行上一轮待确认的写操作。请严格按下面这条确认请求立即调用对应写 Tool，"
                        + "不要再次询问确认、不要重新查询资源、不要只回复说明；平台治理层会校验本次确认。\n"
                        + confirmationGoal.trim();
            } else {
                prompt = "用户已经明确确认执行上一轮待确认的写操作。请立即重新调用上一轮被拦截的同一个写 Tool，"
                        + "不要再次询问确认，也不要只回复说明；平台治理层会校验本次确认。";
            }
            prompt = appendPendingConfirmationArguments(operationId, prompt);
        } else {
            prompt = "请继续处理上一条任务；如果仍缺少信息或需要确认，请立即停止并明确告知用户。";
        }
        prompt = appendOperationContext(operationId, prompt, context);
        prompt = "本轮是平台流程建设入口，不承接日常数据查询、指标分析或报告。此类请求请提示进入AI问数。\n" + prompt;
        if (isDecisionOrderServiceIntent(request.getGoal()) && hasBoundDatasource(operationId)) {
            if (explicitlyConfirmed) {
                prompt = "当前流程已解析到真实决策数据源，用户已确认创建订单只读数据服务。"
                        + "请立即原样重放上一轮被拦截的 create_data_service，不要再次调用 create_datasource、list_datasources 或 get_datasource，也不要重新询问数据源。\n"
                        + prompt;
            } else {
                prompt = "当前流程已自动解析真实决策数据源 datamaster_ai_decision。"
                        + "用户明确要求创建订单数据接口；请立即调用 list_assets 搜索该 datasourceId 下的订单表，"
                        + "若销售订单表唯一且有字段，再调用 create_data_service 创建只读表单查询服务草稿。"
                        + "禁止调用 create_datasource，禁止停留在仅介绍数据源。\n" + prompt;
            }
        }
        // 保存/确认动作时，完整的函数体会让模型再次序列化超长 Tool 参数，
        // 容易触发供应商返回 EOF。预览已持久化，给模型只提供索引、名称和类型即可，
        // CreateActionTool 会按 operationId + previewIndex 恢复完整函数体。
        boolean actionWriteFollowup = isActionWriteFollowup(request.getGoal(), operationId);
        if (actionWriteFollowup) {
            prompt = appendActionPreviewSummary(operationId, prompt);
            if (resumed || explicitlyConfirmed) {
                memory = MessageWindowChatMemory.builder()
                        .id("agent-confirm-" + operationId + "-" + UUID.randomUUID())
                        .maxMessages(8)
                        .build();
                memory.add(SystemMessage.from(SYSTEM_PROMPT));
            }
        }

        // “生成动作预览”是平台内固定且无写入副作用的业务入口。不要让模型再次
        // 序列化一份复杂 Tool 参数：部分模型供应商会返回非法 function.arguments，
        // 导致普通用户即使只说“生成排产动作”也无法进入 generate_actions。
        // 页面本体 ID 已由 Controller 放入可信上下文，工具内部仍会执行空间权限校验。
        if (isDeterministicActionSaveRequest(request.getGoal(), operationId)) {
            AgentToolContextHolder.set(executionContext);
            try {
                appendPersistedToolResults(operationId, result);
                Map<String, Object> pendingArguments = pendingConfirmationArguments(operationId, "create_action");
                String ontologyId = ontologyIdFromContext(executionContext, operationId);
                if (StringUtils.isBlank(ontologyId) && pendingArguments.get("ontologyId") != null) {
                    ontologyId = String.valueOf(pendingArguments.get("ontologyId"));
                }
                String previewIndex = previewIndexFromGoalOrResult(request.getGoal(), operationId);
                if (StringUtils.isBlank(previewIndex) && pendingArguments.get("previewIndex") != null) {
                    previewIndex = String.valueOf(pendingArguments.get("previewIndex"));
                }
                if (StringUtils.isBlank(ontologyId) || StringUtils.isBlank(previewIndex)) {
                    AgentToolResult needInput = AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                            StringUtils.isBlank(ontologyId)
                                    ? "保存动作需要当前本体上下文；请先在本体工作台中打开目标本体"
                                    : "当前动作预览不唯一，请明确要保存的 previewIndex",
                            operationId);
                    result.getToolResults().add(needInput);
                    result.setSuccess(false);
                    result.setStatus(needInput.getStatus());
                    result.setMessage(needInput.getMessage());
                    persistOperation(operationId, request, result);
                    return result;
                }
                AgentToolRequest.CreateAction input = new AgentToolRequest.CreateAction();
                input.setOntologyId(ontologyId);
                input.setPreviewIndex(previewIndex);
                if (pendingArguments.get("name") != null) input.setName(String.valueOf(pendingArguments.get("name")));
                if (pendingArguments.get("actionType") != null) input.setActionType(String.valueOf(pendingArguments.get("actionType")));
                if (pendingArguments.get("needsApproval") != null) {
                    input.setNeedsApproval(Boolean.parseBoolean(String.valueOf(pendingArguments.get("needsApproval"))));
                }
                AgentToolResult toolResult = createActionTool.execute(input);
                result.getToolResults().add(toolResult);
                result.setSuccess(AgentToolResult.SUCCEEDED.equals(toolResult.getStatus()));
                result.setStatus(toolResult.getStatus());
                result.setMessage(toolResult.getMessage());
                result.setText(toolResult.getMessage());
                rememberDeterministicResult(memory, request.getGoal(), result.getToolResults());
                persistOperation(operationId, request, result);
                return result;
            } finally {
                AgentToolContextHolder.clear();
            }
        }
        if (!resumed && isDeterministicActionPreviewRequest(request.getGoal())) {
            AgentToolContextHolder.set(executionContext);
            try {
                List<AgentToolResult> deterministicResults = new ArrayList<>();
                String ontologyId = ontologyIdFromContext(executionContext, operationId);
                if (StringUtils.isBlank(ontologyId)) {
                    String ontologyName = extractOntologyName(request.getGoal());
                    if (StringUtils.isBlank(ontologyName)) {
                        AgentToolResult needInput = AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                                "动作预览需要本体上下文；请提供本体名称（例如“AI决策模拟库”）。物料库存是概念名称，不能单独唯一定位本体",
                                operationId);
                        result.getToolResults().add(needInput);
                        result.setSuccess(false);
                        result.setStatus(needInput.getStatus());
                        result.setMessage(needInput.getMessage());
                        result.setText(needInput.getMessage());
                        rememberDeterministicResult(memory, request.getGoal(), result.getToolResults());
                        persistOperation(operationId, request, result);
                        return result;
                    }
                    AgentToolRequest.SearchOntologies lookup = new AgentToolRequest.SearchOntologies();
                    lookup.setName(ontologyName);
                    AgentToolResult lookupResult = searchOntologiesTool.execute(lookup);
                    deterministicResults.add(lookupResult);
                    ontologyId = ontologyIdFromLookup(lookupResult);
                    if (StringUtils.isBlank(ontologyId)) {
                        result.getToolResults().addAll(deterministicResults);
                        result.setSuccess(AgentToolResult.SUCCEEDED.equals(lookupResult.getStatus()));
                        result.setStatus(lookupResult.getStatus());
                        result.setMessage(lookupResult.getMessage());
                        result.setText(lookupResult.getMessage());
                        rememberDeterministicResult(memory, request.getGoal(), deterministicResults);
                        persistOperation(operationId, request, result);
                        return result;
                    }
                }
                AgentToolRequest.GenerateActions input = new AgentToolRequest.GenerateActions();
                input.setOntologyId(ontologyId);
                input.setPrompt(request.getGoal().trim());
                AgentToolResult toolResult = generateActionsTool.execute(input);
                result.getToolResults().addAll(deterministicResults);
                result.getToolResults().add(toolResult);
                result.setSuccess(AgentToolResult.SUCCEEDED.equals(toolResult.getStatus()));
                result.setStatus(toolResult.getStatus());
                result.setMessage(toolResult.getMessage());
                result.setText(toolResult.getMessage());
                setOperationOntologyId(operationId, ontologyId);
                rememberDeterministicResult(memory, request.getGoal(), result.getToolResults());
                persistOperation(operationId, request, result);
                return result;
            } finally {
                AgentToolContextHolder.clear();
            }
        }
        AgentToolContextHolder.set(executionContext);
        String executionStage = "prepare";
        try {
            executionStage = "build_assistant";
            AutonomousAssistant assistant = AiServices.builder(AutonomousAssistant.class)
                    .chatLanguageModel(model)
                    .chatMemory(memory)
                    // AiServices 负责模型 Tool Calling、参数转换和调度；Catalog 只做
                    // Spring AOP 代理到框架 ToolExecutor 的兼容适配，不自行循环调度。
                    .tools(toolCatalog.frameworkTools(routedTools))
                    .build();
            executionStage = "model_chat";
            String text;
            try {
                text = assistant.chat(prompt);
            } catch (Exception first) {
                // 模型偶发返回截断的 Tool JSON 时，用不带历史长消息的确认上下文重试一次。
                // 仅限动作保存/确认，避免改变普通查询的错误语义。
                if (!actionWriteFollowup || !isEndOfInputFailure(first)) throw first;
                ChatMemory retryMemory = MessageWindowChatMemory.builder()
                        .id("agent-action-retry-" + operationId + "-" + UUID.randomUUID())
                        .maxMessages(8)
                        .build();
                retryMemory.add(SystemMessage.from(SYSTEM_PROMPT));
                AutonomousAssistant retryAssistant = AiServices.builder(AutonomousAssistant.class)
                        .chatLanguageModel(model)
                        .chatMemory(retryMemory)
                        .tools(toolCatalog.frameworkTools(routedTools))
                        .build();
                text = retryAssistant.chat(prompt);
            }
            // 续跑时 result 里会先带上上一轮的 Tool 结果。终态只能根据本轮
            // 新产生的结果判断，否则上一轮 NEED_CONFIRMATION 会污染本轮状态，
            // 导致用户已经确认后仍被前端反复要求确认。
            List<AgentToolResult> currentToolResults = AgentToolContextHolder.results();
            result.getToolResults().addAll(currentToolResults);
            AgentToolResult terminal = lastTerminalResult(currentToolResults);
            if (terminal != null) {
                result.setSuccess(false);
                result.setStatus(terminal.getStatus());
                result.setMessage(terminal.getMessage());
            } else {
                result.setSuccess(true);
                result.setStatus(AgentToolResult.SUCCEEDED);
                result.setText(text);
                result.setMessage("Agent 已完成本轮调度");
            }
            persistOperation(operationId, request, result);
            return result;
        } catch (AgentToolFlowHaltException ex) {
            result.getToolResults().addAll(AgentToolContextHolder.results());
            AgentToolResult halted = ex.getResult() == null ? lastTerminalResult(result.getToolResults()) : ex.getResult();
            result.setSuccess(false);
            result.setStatus(halted == null ? AgentToolResult.FAILED : halted.getStatus());
            result.setMessage(halted == null ? ex.getMessage() : halted.getMessage());
            completeHaltedToolMemory(memory, halted);
            persistOperation(operationId, request, result);
            return result;
        } catch (Exception ex) {
            List<AgentToolResult> currentToolResults = AgentToolContextHolder.results();
            result.getToolResults().addAll(currentToolResults);
            AgentToolResult halted = lastTerminalResult(currentToolResults);
            result.setSuccess(false);
            result.setStatus(halted == null ? AgentToolResult.FAILED : halted.getStatus());
            String failure = describeException(ex);
            log.error("Agent 模型/Tool 调用失败，stage={}，operationId={}，conversationId={}，异常={}",
                    executionStage, operationId, conversationId, failure, ex);
            result.setMessage(halted == null ? "Agent 模型调用失败（" + executionStage + "）: " + failure : halted.getMessage());
            completeHaltedToolMemory(memory, halted);
            persistOperation(operationId, request, result);
            return result;
        } finally {
            AgentToolContextHolder.clear();
        }
    }

    private AgentRunResponse resolveCurrentDatasourceAlias(AgentRunRequest request, String operationId,
                                                            AgentToolContext context, AgentRunResponse result) {
        if (!isCurrentDatasourceAlias(request == null ? null : request.getGoal())) return null;
        AiAgentOperationDO operation = operationService.get(operationId);
        if (operation == null || operation.getDatasourceId() == null || context == null) return null;
        AgentToolRequest.GetDatasource input = new AgentToolRequest.GetDatasource();
        input.setDatasourceId(String.valueOf(operation.getDatasourceId()));
        AgentToolContextHolder.set(context);
        try {
            AgentToolResult toolResult = getDatasourceTool.execute(input);
            result.getToolResults().add(toolResult);
            result.setStatus(toolResult.getStatus());
            result.setSuccess(AgentToolResult.SUCCEEDED.equals(toolResult.getStatus()));
            result.setMessage(toolResult.getMessage());
            result.setText(toolResult.getMessage());
            persistOperation(operationId, request, result);
            return result;
        } finally {
            AgentToolContextHolder.clear();
        }
    }

    private boolean isCurrentDatasourceAlias(String goal) {
        if (StringUtils.isBlank(goal)) return false;
        String normalized = goal.trim().replace(" ", "").replace("　", "");
        return "决策数据源".equals(normalized)
                || "当前决策数据源".equals(normalized)
                || "决策数据库".equals(normalized)
                || "当前决策库".equals(normalized)
                || "决策库".equals(normalized);
    }

    private void bindDecisionDatasourceAlias(AgentRunRequest request, String operationId,
                                             AgentToolContext context) {
        if (request == null || context == null || !containsDecisionDatasourceAlias(request.getGoal())) return;
        AiAgentOperationDO operation = operationService.get(operationId);
        if (operation == null || datasourceService == null) return;
        // 用户在同一轮明确给出 datasourceId 时，以该受控资源为准。旧会话可能已经
        // 误绑定了测试库 id=9；继续沿用旧绑定会让“决策数据库”永远无法切回真实目标。
        Long explicitDatasourceId = datasourceIdFromGoal(request.getGoal());
        if (explicitDatasourceId != null) {
            AssetsDatasourceDO explicit = datasourceService.getDatasourceDOById(explicitDatasourceId);
            if (explicit != null && resourceGuard.datasourceVisible(explicitDatasourceId, context)) {
                bindDecisionDatasource(operation, explicit, context.getUserId());
                return;
            }
        }
        // 历史会话可能已经误绑定到普通测试库（例如 datamaster_test）。
        // “决策数据源/决策数据库”是语义别名，遇到这种旧绑定时必须重新解析
        // 当前空间唯一的决策库，不能把旧数据源当成用户已经确认的目标。
        if (operation.getDatasourceId() != null) {
            AssetsDatasourceDO bound = datasourceService.getDatasourceDOById(operation.getDatasourceId());
            if (isDecisionDatasource(bound)) return;
        }
        List<AssetsDatasourceDO> candidates = new ArrayList<>();
        List<AssetsDatasourceDO> datasources = datasourceService.getDatasourceList();
        if (datasources != null) {
            for (AssetsDatasourceDO datasource : datasources) {
                if (datasource == null || datasource.getId() == null) continue;
                if (!resourceGuard.datasourceVisible(datasource.getId(), context)) continue;
                String name = datasource.getDatasourceName() == null ? ""
                        : datasource.getDatasourceName().toLowerCase(java.util.Locale.ROOT);
                if (isDecisionDatasource(datasource)) candidates.add(datasource);
            }
        }
        if (candidates.size() != 1) return;
        bindDecisionDatasource(operation, candidates.get(0), context.getUserId());
    }

    private void bindDecisionDatasource(AiAgentOperationDO operation, AssetsDatasourceDO datasource, Long userId) {
        if (operation == null || datasource == null) return;
        operation.setDatasourceId(datasource.getId());
        operation.setDatasourceName(datasource.getDatasourceName());
        operation.setDatabaseName(databaseName(datasource));
        setDatasourceFlowKeySafely(operation, datasource.getId(), userId);
        operation.setUpdateTime(new java.util.Date());
        operationService.update(operation);
    }

    private void setDatasourceFlowKeySafely(AiAgentOperationDO operation, Long datasourceId, Long userId) {
        if (operation == null || datasourceId == null) return;
        String space = operation.getSpaceId() == null
                ? (operation.getSpaceCode() == null ? "" : operation.getSpaceCode().trim())
                : String.valueOf(operation.getSpaceId());
        String flowKey = "DATASOURCE:" + space + ":" + datasourceId;
        AiAgentOperationDO existing = operationService.findByFlowKey(flowKey, userId);
        // flowKey 标识可恢复的主数据源流程，同一用户和空间只能有一条。
        // 临时聊天操作可以复用该数据源，但不能抢占或重复写入唯一键。
        operation.setFlowKey(existing == null || operation.getId().equals(existing.getId()) ? flowKey : null);
    }

    private boolean isDecisionDatasource(AssetsDatasourceDO datasource) {
        if (datasource == null) return false;
        String name = datasource.getDatasourceName() == null ? ""
                : datasource.getDatasourceName().toLowerCase(java.util.Locale.ROOT);
        if (name.contains("decision") || name.contains("决策")) return true;
        String database = databaseName(datasource);
        if (database == null) return false;
        String normalized = database.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("decision") || normalized.contains("决策");
    }

    private Long datasourceIdFromGoal(String goal) {
        if (StringUtils.isBlank(goal)) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)datasource(?:Id|_id)?\\s*[=:：]?\\s*[\\\"']?(\\d{1,25})")
                .matcher(goal);
        if (!matcher.find()) return null;
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean containsDecisionDatasourceAlias(String goal) {
        if (StringUtils.isBlank(goal)) return false;
        String normalized = goal.replace(" ", "").replace("　", "");
        return normalized.contains("决策数据源") || normalized.contains("决策数据库")
                || normalized.contains("当前决策库") || normalized.contains("决策库")
                || normalized.contains("决策数据");
    }

    private boolean isDecisionOrderServiceIntent(String goal) {
        if (!containsDecisionDatasourceAlias(goal) || StringUtils.isBlank(goal)) return false;
        String normalized = goal.replace(" ", "").replace("　", "");
        return normalized.contains("订单") && (normalized.contains("接口") || normalized.contains("服务"));
    }

    private boolean hasBoundDatasource(String operationId) {
        if (StringUtils.isBlank(operationId)) return false;
        AiAgentOperationDO operation = operationService.get(operationId);
        return operation != null && operation.getDatasourceId() != null;
    }

    private AgentRunResponse runDecisionOrderServiceFlow(AgentRunRequest request, String operationId,
                                                         AgentToolContext context, AgentRunResponse result,
                                                         boolean confirmed) {
        if (!isDecisionOrderServiceIntent(request == null ? null : request.getGoal())
                || !hasBoundDatasource(operationId) || listAssetsTool == null || createDataServiceTool == null) {
            return null;
        }
        AiAgentOperationDO operation = operationService.get(operationId);
        if (operation == null || operation.getDatasourceId() == null) return null;
        AgentToolContextHolder.set(context);
        try {
            AgentToolRequest.ListAssets assetsRequest = new AgentToolRequest.ListAssets();
            assetsRequest.setDatasourceId(String.valueOf(operation.getDatasourceId()));
            AgentToolResult assets = listAssetsTool.execute(assetsRequest);
            result.getToolResults().add(assets);
            if (!AgentToolResult.SUCCEEDED.equals(assets.getStatus())) {
                result.setStatus(assets.getStatus());
                result.setSuccess(false);
                result.setMessage(assets.getMessage());
                result.setText(assets.getMessage());
                persistOperation(operationId, request, result);
                return result;
            }
            String orderTable = resolveOrderTable(assets.getData());
            if (orderTable == null) {
                result.setStatus(AgentToolResult.NEED_INPUT);
                result.setSuccess(false);
                result.setMessage("决策数据源中未找到唯一订单表");
                result.setText(result.getMessage());
                persistOperation(operationId, request, result);
                return result;
            }
            AgentToolRequest.CreateDataService create = new AgentToolRequest.CreateDataService();
            create.setDatasourceId(String.valueOf(operation.getDatasourceId()));
            create.setTableName(orderTable);
            create.setName("销售订单查询服务");
            create.setApiUrl("/api/v1/data-service/sales-order");
            create.setDescription("决策数据库销售订单只读查询服务");
            AgentToolResult created = createDataServiceTool.execute(create);
            result.getToolResults().add(created);
            result.setStatus(created.getStatus());
            result.setSuccess(AgentToolResult.SUCCEEDED.equals(created.getStatus()));
            String createdMessage = created.getMessage();
            if (AgentToolResult.SUCCEEDED.equals(created.getStatus()) && created.getData() instanceof Map) {
                Map<?, ?> service = (Map<?, ?>) created.getData();
                Object serviceId = service.get("serviceId");
                Object apiUrl = service.get("apiUrl");
                if (serviceId != null) {
                    createdMessage = createdMessage + "（serviceId=" + serviceId
                            + (apiUrl == null ? "" : "，接口路径=" + apiUrl) + "）";
                }
            }
            result.setMessage(createdMessage);
            result.setText(createdMessage);
            persistOperation(operationId, request, result);
            return result;
        } finally {
            AgentToolContextHolder.clear();
        }
    }

    private String resolveOrderTable(Object data) {
        if (!(data instanceof List)) return null;
        String fallback = null;
        for (Object item : (List<?>) data) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> row = (Map<?, ?>) item;
            String tableName = row.get("tableName") == null ? null : String.valueOf(row.get("tableName"));
            String name = row.get("name") == null ? "" : String.valueOf(row.get("name"));
            if ("sales_order".equalsIgnoreCase(tableName)) return tableName;
            if (tableName != null && (tableName.toLowerCase(java.util.Locale.ROOT).contains("order")
                    || name.contains("订单")) && fallback == null) fallback = tableName;
        }
        return fallback;
    }

    private String databaseName(AssetsDatasourceDO datasource) {
        if (datasource == null || StringUtils.isBlank(datasource.getDatasourceConfig())) return null;
        try {
            return JSON.parseObject(datasource.getDatasourceConfig()).getString("dbname");
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isEndOfInputFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof java.io.EOFException
                    || current.getClass().getSimpleName().contains("EOFException")
                    || (current.getMessage() != null && current.getMessage().contains("End of input"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String buildAgentMemoryId(AgentRunRequest request, AgentToolContext context,
                                      String conversationId) {
        String user = context == null ? "anonymous" : String.valueOf(context.getUserId());
        String space = context == null ? String.valueOf(request.getSpaceId()) : String.valueOf(context.getSpaceId());
        if (request != null && request.getSessionId() != null) {
            return user + ":" + space + ":agent-session:" + request.getSessionId();
        }
        return user + ":" + space + ":" + conversationId;
    }

    private AgentRunResponse confirmMetricDraft(AgentRunRequest request, AgentToolContext context,
                                                String operationId, String conversationId) {
        if (context == null || !Boolean.TRUE.equals(request.getConfirmed())
                || !Boolean.TRUE.equals(context.getAttributes().get("confirmed"))) return null;
        AiAgentOperationDO operation = operationService.get(operationId);
        if (operation == null) return null;
        requireAccess(operation, context);
        AgentToolResult pending = com.datamaster.module.ai.controller.agent.tool.metricdraft.MetricDraftConfirmation.pending(
                operation.getStatus(), operation.getResultJson());
        if (pending == null) return null;
        Object arguments = com.datamaster.module.ai.controller.agent.tool.metricdraft.MetricDraftConfirmation.arguments(pending);
        Map<String, Object> values = JSON.parseObject(JSON.toJSONString(arguments), Map.class);
        AgentToolResult saved = toolCatalog.invoke("save_metric_draft", context, values);
        AgentRunResponse response = new AgentRunResponse();
        response.setOperationId(operationId); response.setConversationId(conversationId); response.setSessionId(request.getSessionId());
        response.setSuccess(AgentToolResult.SUCCEEDED.equals(saved.getStatus())); response.setStatus(saved.getStatus());
        response.setMessage(saved.getMessage()); response.setText(saved.getMessage()); response.getToolResults().add(saved);
        persistOperation(operationId, request, response);
        return response;
    }

    private boolean isActionWriteFollowup(String goal, String operationId) {
        if (StringUtils.isBlank(goal) || StringUtils.isBlank(operationId)) return false;
        String text = goal.toLowerCase(java.util.Locale.ROOT);
        return text.contains("保存") || text.contains("确认") || text.contains("写入")
                || text.contains("创建动作") || text.contains("保存动作");
    }

    private boolean isNaturalContinuationRequest(String goal) {
        if (StringUtils.isBlank(goal)) return false;
        String text = goal.toLowerCase(java.util.Locale.ROOT);
        boolean continuation = text.contains("继续") || text.contains("下一步") || text.contains("接着")
                || text.contains("然后呢") || text.contains("继续啊");
        // “保存并继续/确认继续执行”属于写操作确认，必须保留原有治理链路，
        // 不能被动作预览恢复快捷路径提前返回。
        return continuation && !text.contains("保存") && !text.contains("确认")
                && !text.contains("提交") && !text.contains("执行") && !text.contains("生成");
    }

    /**
     * 直接调用只读 Tool 时没有经过 AiServices，模型聊天记忆不会自动收到用户消息和
     * Tool 结果。补一条精简的可读摘要，保证下一轮普通对话也能基于上轮事实推理，
     * 同时避免把完整 functionBody/执行步骤塞回模型上下文。
     */
    private void rememberDeterministicResult(ChatMemory memory, String goal,
                                             List<AgentToolResult> toolResults) {
        if (memory == null || StringUtils.isBlank(goal)) return;
        StringBuilder summary = new StringBuilder("平台已完成上一轮只读 Tool 调度：");
        if (toolResults != null) {
            for (AgentToolResult item : toolResults) {
                if (item == null) continue;
                summary.append("\n[").append(item.getToolName()).append("] ")
                        .append(item.getStatus()).append("：").append(item.getMessage());
            }
        }
        memory.add(UserMessage.from(goal));
        memory.add(AiMessage.from(summary.toString()));
    }

    /**
     * 页面 Agent 会话是用户可见的事实来源。问数页面以前由 DB-GPT 自己维护上下文，
     * 但自主 Agent 必须在本地把最近页面消息同步给 LangChain4j；否则页面能看到历史，
     * 模型却只收到当前一句话。这里只同步最近 20 条消息（约 10 轮），Tool 的完整
     * 参数和结果仍由 AI_AGENT_OPERATION 保存，不把敏感/超长内容塞进聊天上下文。
     */
    private void synchronizePageSession(ChatMemory memory, AgentRunRequest request,
                                        AgentToolContext context) {
        if (memory == null || request == null || request.getSessionId() == null
                || askSessionService == null || context == null || context.getUserId() == null) return;
        try {
            AiAskMessageWindowRespVO window = askSessionService.listMessages(
                    context.getUserId(), context.getSpaceId(), request.getSessionId(), null, null, 20);
            if (window == null || window.getRows() == null) return;
            List<dev.langchain4j.data.message.ChatMessage> existing = memory.messages();
            for (int i = 0; i < window.getRows().size(); i++) {
                AiAskMessageRespVO row = window.getRows().get(i);
                if (row == null || StringUtils.isBlank(row.getContent())) continue;
                String text = StringUtils.isNotBlank(row.getDisplayContent())
                        ? row.getDisplayContent() : row.getContent();
                // 前端在调用 Agent 前已经先落了一条当前用户消息；模型调用本身还会
                // 添加带页面上下文的当前 prompt，因此跳过这条尾部用户消息，避免重复。
                if (i == window.getRows().size() - 1 && "user".equalsIgnoreCase(row.getRole())
                        && StringUtils.isNotBlank(request.getGoal())
                        && (request.getGoal().contains("用户目标：" + text)
                        || request.getGoal().trim().endsWith(text.trim()))) continue;
                if (containsEquivalentMessage(existing, row.getRole(), text)) continue;
                if ("user".equalsIgnoreCase(row.getRole())) memory.add(UserMessage.from(text));
                else if ("assistant".equalsIgnoreCase(row.getRole())
                        || "ai".equalsIgnoreCase(row.getRole())) memory.add(AiMessage.from(text));
                existing = memory.messages();
            }
        } catch (Exception ex) {
            // 页面会话读取失败不能阻断 Agent；operationId 和本地 ChatMemory 仍可继续工作。
            log.warn("同步 Agent 页面会话到模型上下文失败，sessionId={}", request.getSessionId(), ex);
        }
    }

    private boolean containsEquivalentMessage(List<dev.langchain4j.data.message.ChatMessage> existing,
                                              String role, String text) {
        if (existing == null || StringUtils.isBlank(text)) return false;
        String candidate = text.trim();
        for (dev.langchain4j.data.message.ChatMessage message : existing) {
            if (message == null || message.text() == null) continue;
            boolean sameRole = ("user".equalsIgnoreCase(role) && message instanceof UserMessage)
                    || (("assistant".equalsIgnoreCase(role) || "ai".equalsIgnoreCase(role))
                    && message instanceof AiMessage);
            if (!sameRole) continue;
            String current = message.text().trim();
            if (current.equals(candidate)
                    || current.contains("用户目标：" + candidate)
                    || candidate.contains(current) && current.length() > 20) return true;
        }
        return false;
    }

    /**
     * 恢复动作生成链路时不再把“继续”交给模型：已有动作预览直接返回，
     * 只有上一轮停在 search_ontologies 时才用持久化的原始目标补调 generate_actions。
     */
    private AgentRunResponse continueActionPreview(String operationId, AgentRunRequest request,
                                                    AgentToolContext context, AgentRunResponse result,
                                                    ChatMemory memory) {
        if (StringUtils.isBlank(operationId)) return null;
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null || StringUtils.isBlank(operation.getResultJson())) return null;
            AgentRunResponse saved = JSON.parseObject(operation.getResultJson(), AgentRunResponse.class);
            if (saved == null || saved.getToolResults() == null || saved.getToolResults().isEmpty()) return null;

            AgentToolResult generated = null;
            AgentToolResult searched = null;
            for (AgentToolResult item : saved.getToolResults()) {
                if (item == null) continue;
                if ("generate_actions".equals(item.getToolName())
                        && AgentToolResult.SUCCEEDED.equals(item.getStatus())) generated = item;
                if ("search_ontologies".equals(item.getToolName())
                        && AgentToolResult.SUCCEEDED.equals(item.getStatus())) searched = item;
            }

            if (generated != null) {
                result.setToolResults(new ArrayList<>(saved.getToolResults()));
                result.setSuccess(true);
                result.setStatus(AgentToolResult.SUCCEEDED);
                result.setMessage("已恢复上一轮动作预览；如需保存，请明确说“保存这个动作”");
                result.setText(result.getMessage());
                rememberDeterministicResult(memory, request.getGoal(), result.getToolResults());
                // 仅恢复结果，不改写原始 requestJson，保留最初的动作目标供后续续接使用。
                return result;
            }

            String ontologyId = ontologyIdFromSearchResult(searched);
            if (StringUtils.isBlank(ontologyId)) return null;
            String originalGoal = originalGoal(operation);
            if (StringUtils.isBlank(originalGoal)) originalGoal = request.getGoal();
            AgentToolRequest.GenerateActions input = new AgentToolRequest.GenerateActions();
            input.setOntologyId(ontologyId);
            input.setPrompt(originalGoal);
            AgentToolContextHolder.set(context);
            AgentToolResult regenerated;
            try {
                regenerated = generateActionsTool.execute(input);
            } finally {
                AgentToolContextHolder.clear();
            }
            result.setToolResults(new ArrayList<>(saved.getToolResults()));
            result.getToolResults().add(regenerated);
            result.setSuccess(AgentToolResult.SUCCEEDED.equals(regenerated.getStatus()));
            result.setStatus(regenerated.getStatus());
            result.setMessage(regenerated.getMessage());
            result.setText(regenerated.getMessage());
            setOperationOntologyId(operationId, ontologyId);
            rememberDeterministicResult(memory, request.getGoal(), result.getToolResults());
            persistOperation(operationId, request, result);
            return result;
        } catch (Exception ignored) {
            // 非动作任务继续走正常模型链路；不能因恢复尝试失败吞掉原始请求。
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String ontologyIdFromSearchResult(AgentToolResult searched) {
        if (searched == null || !(searched.getData() instanceof Map)) return null;
        Map<String, Object> data = (Map<String, Object>) searched.getData();
        if (!Boolean.TRUE.equals(data.get("uniqueMatch"))) return null;
        Object id = data.get("ontologyId");
        return id == null ? null : String.valueOf(id);
    }

    private String originalGoal(AiAgentOperationDO operation) {
        if (operation == null || StringUtils.isBlank(operation.getRequestJson())) return null;
        try {
            AgentRunRequest saved = JSON.parseObject(operation.getRequestJson(), AgentRunRequest.class);
            return saved == null ? null : saved.getGoal();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isDeterministicActionPreviewRequest(String goal) {
        if (StringUtils.isBlank(goal)) return false;
        String text = goal.toLowerCase(java.util.Locale.ROOT);
        boolean actionIntent = text.contains("生成动作") || text.matches(".*生成[^\\n，。；;]{0,30}动作.*")
                || text.contains("动作预览")
                || text.contains("动作草稿") || text.contains("生成排产")
                || text.contains("排产动作")
                // 普通用户更常说“做一个动作……先生成预览”，此时“生成”位于
                // “动作”之后，不能只依赖“生成xx动作”的词序。
                || (text.contains("动作") && (text.contains("预览") || text.contains("草稿")));
        if (!actionIntent) return false;
        // 保存、提交和执行必须继续走确认/治理流程，不能被预览快捷路径吞掉。
        return !hasAffirmativeSaveIntent(text) && !hasAffirmativeExecutionIntent(text)
                && !hasAffirmativeSubmitIntent(text)
                && !text.contains("试运行");
    }

    private boolean isDeterministicActionSaveRequest(String goal, String operationId) {
        if (StringUtils.isBlank(goal)) return false;
        String text = goal.toLowerCase(java.util.Locale.ROOT);
        if (hasAffirmativeSaveIntent(text)
                && text.contains("动作") && !text.contains("生成动作")) return true;
        return hasPendingConfirmation(operationId, "create_action");
    }

    /**
     * 中文用户经常用“只生成预览，不保存”描述动作草稿。
     * 不能把否定短语中的“保存”当成写入确认，否则预览请求会直接误走 create_action。
     */
    private boolean hasAffirmativeSaveIntent(String text) {
        if (StringUtils.isBlank(text) || !text.contains("保存")) return false;
        return !text.contains("不保存") && !text.contains("不要保存")
                && !text.contains("无需保存") && !text.contains("无需持久化")
                && !text.contains("不需要保存") && !text.contains("仅预览")
                && !text.contains("只生成预览");
    }

    private boolean hasAffirmativeExecutionIntent(String text) {
        if (StringUtils.isBlank(text) || !text.contains("执行")) return false;
        return !text.contains("不执行") && !text.contains("不要执行")
                && !text.contains("不运行") && !text.contains("不要运行")
                && !text.contains("仅预览") && !text.contains("只生成预览");
    }

    private boolean hasAffirmativeSubmitIntent(String text) {
        if (StringUtils.isBlank(text) || !text.contains("提交")) return false;
        return !text.contains("不提交") && !text.contains("不要提交")
                && !text.contains("仅预览") && !text.contains("只生成预览");
    }

    private boolean hasPendingConfirmation(String operationId, String toolName) {
        return !pendingConfirmationArguments(operationId, toolName).isEmpty();
    }

    private void appendPersistedToolResults(String operationId, AgentRunResponse result) {
        if (result == null || result.getToolResults() == null || !result.getToolResults().isEmpty()
                || StringUtils.isBlank(operationId)) return;
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null || StringUtils.isBlank(operation.getResultJson())) return;
            AgentRunResponse saved = JSON.parseObject(operation.getResultJson(), AgentRunResponse.class);
            if (saved != null && saved.getToolResults() != null) result.getToolResults().addAll(saved.getToolResults());
        } catch (Exception ignored) {
            // 历史结果不可解析时仍让 create_action 给出明确的上下文错误。
        }
    }

    private String previewIndexFromGoalOrResult(String goal, String operationId) {
        if (StringUtils.isNotBlank(goal)) {
            Matcher matcher = Pattern.compile("(?i)previewIndex\\s*[=:：]\\s*(\\d+)").matcher(goal);
            if (matcher.find()) return matcher.group(1);
        }
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null || StringUtils.isBlank(operation.getResultJson())) return null;
            JSONObject root = JSON.parseObject(operation.getResultJson());
            JSONArray toolResults = root == null ? null : root.getJSONArray("toolResults");
            if (toolResults == null) return null;
            for (int i = toolResults.size() - 1; i >= 0; i--) {
                JSONObject toolResult = toolResults.getJSONObject(i);
                if (toolResult == null || !"generate_actions".equals(toolResult.getString("toolName"))) continue;
                JSONObject data = toolResult.getJSONObject("data");
                JSONArray actions = data == null ? null : data.getJSONArray("actions");
                return actions != null && actions.size() == 1 ? "0" : null;
            }
        } catch (Exception ignored) {
            // 让上层返回“预览不唯一/已失效”，而不是猜测动作。
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pendingConfirmationArguments(String operationId, String toolName) {
        Map<String, Object> empty = new LinkedHashMap<>();
        if (StringUtils.isBlank(operationId) || StringUtils.isBlank(toolName)) return empty;
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null || StringUtils.isBlank(operation.getResultJson())) return empty;
            JSONObject root = JSON.parseObject(operation.getResultJson());
            JSONArray toolResults = root == null ? null : root.getJSONArray("toolResults");
            if (toolResults == null) return empty;
            for (int i = toolResults.size() - 1; i >= 0; i--) {
                JSONObject toolResult = toolResults.getJSONObject(i);
                if (toolResult == null || !toolName.equals(toolResult.getString("toolName"))
                        || !AgentToolResult.NEED_CONFIRMATION.equals(toolResult.getString("status"))) continue;
                JSONObject metadata = toolResult.getJSONObject("metadata");
                Object arguments = metadata == null ? null : metadata.get("arguments");
                if (arguments instanceof Map) return new LinkedHashMap<>((Map<String, Object>) arguments);
                if (arguments != null) {
                    Map<String, Object> parsed = JSON.parseObject(JSON.toJSONString(arguments), Map.class);
                    return parsed == null ? empty : new LinkedHashMap<>(parsed);
                }
            }
        } catch (Exception ignored) {
            // 确认参数恢复失败时让上层返回 NEED_INPUT，而不是猜测写入参数。
        }
        return empty;
    }

    private void setOperationOntologyId(String operationId, String ontologyId) {
        if (StringUtils.isBlank(operationId) || StringUtils.isBlank(ontologyId)) return;
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null || operation.getOntologyId() != null) return;
            operation.setOntologyId(Long.valueOf(ontologyId));
            operation.setUpdateTime(new java.util.Date());
            operationService.update(operation);
        } catch (Exception ignored) {
            // 结果本身仍可返回；CreateActionTool 还会从页面上下文读取 ontologyId。
        }
    }

    private String ontologyIdFromContext(AgentToolContext context, String operationId) {
        if (context != null && context.getAttributes() != null) {
            Object value = context.getAttributes().get("ontologyId");
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) return String.valueOf(value);
        }
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation != null && operation.getOntologyId() != null) return String.valueOf(operation.getOntologyId());
        } catch (Exception ignored) {
            // 没有持久化上下文时让 GenerateActionsTool 返回明确的查询提示。
        }
        return null;
    }

    private String extractOntologyName(String goal) {
        if (StringUtils.isBlank(goal)) return null;
        // 先取用户明确标出的本体名称。模型经常会保留中文引号，若直接把引号
        // 传给 LIKE 查询，会导致“AI决策模拟库”匹配不到 AI决策模拟库。
        Matcher quoted = Pattern.compile("[\\\"“”‘’']([^\\\"“”‘’']{2,60})[\\\"“”‘’']").matcher(goal);
        if (quoted.find() && (goal.contains("本体") || goal.toLowerCase(java.util.Locale.ROOT).contains("ontology"))) {
            return quoted.group(1).trim();
        }
        // 只有明确带“本体”后缀时才把无引号名称当作 ontology name；
        // “给物料库存做一个动作”中的物料库存是概念名，不能误当成本体名。
        Matcher matcher = Pattern.compile("(?:基于|根据|使用|在)\\s*(?:当前\\s*)?([\\p{L}\\p{N}_-]{2,60}?)(?:本体)[\\s,，、]*(?:再\\s*)?(?:生成|创建|设计|做|下)(?:[^\\n，。；;]{0,80})动作").matcher(goal);
        if (matcher.find()) return matcher.group(1).trim();
        matcher = Pattern.compile("(?:本体(?:名称)?|ontology(?:Name)?)\\s*(?:为|是|叫|[=:：])\\s*[\\\"“‘']?([^，。；;\\n\\\"”’']+)[\\\"”’']?" , Pattern.CASE_INSENSITIVE).matcher(goal);
        if (!matcher.find()) return null;
        return matcher.group(1).trim();
    }

    @SuppressWarnings("unchecked")
    private String ontologyIdFromLookup(AgentToolResult lookupResult) {
        if (lookupResult == null || !(lookupResult.getData() instanceof Map)) return null;
        Map<String, Object> data = (Map<String, Object>) lookupResult.getData();
        Object unique = data.get("uniqueMatch");
        Object id = data.get("ontologyId");
        return Boolean.TRUE.equals(unique) && id != null ? String.valueOf(id) : null;
    }

    /** 只向模型暴露预览元数据，完整函数体由 CreateActionTool 从持久化预览恢复。 */
    private String appendActionPreviewSummary(String operationId, String prompt) {
        if (StringUtils.isBlank(operationId) || StringUtils.isBlank(prompt)) return prompt;
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null || StringUtils.isBlank(operation.getResultJson())) return prompt;
            JSONObject root = JSON.parseObject(operation.getResultJson());
            JSONArray toolResults = root == null ? null : root.getJSONArray("toolResults");
            if (toolResults == null) return prompt;
            for (int i = toolResults.size() - 1; i >= 0; i--) {
                JSONObject toolResult = toolResults.getJSONObject(i);
                if (toolResult == null || !"generate_actions".equals(toolResult.getString("toolName"))) continue;
                JSONObject data = toolResult.getJSONObject("data");
                if (data == null) continue;
                JSONArray actions = data.getJSONArray("actions");
                if (actions == null || actions.isEmpty()) continue;
                StringBuilder summary = new StringBuilder("\n可保存的最近动作预览（只使用 previewIndex，不要回显 functionBody）：");
                for (int j = 0; j < actions.size(); j++) {
                    JSONObject action = actions.getJSONObject(j);
                    if (action == null) continue;
                    summary.append("\npreviewIndex=").append(j)
                            .append(", name=").append(action.getString("actionName"))
                            .append(", actionType=").append(action.getString("actionType"));
                }
                return prompt + summary;
            }
        } catch (Exception ignored) {
            // 预览摘要失败不阻断正常对话，CreateActionTool 仍会给出明确 NEED_INPUT。
        }
        return prompt;
    }

    /** 平台确认中断会跳过框架写入 Tool 结果的步骤，补齐它以便下一轮合法续聊。 */
    private void completeHaltedToolMemory(ChatMemory memory, AgentToolResult halted) {
        if (memory == null || halted == null) return;
        List<dev.langchain4j.agent.tool.ToolExecutionRequest> pending = new ArrayList<>();
        java.util.Set<String> completed = new java.util.HashSet<>();
        for (dev.langchain4j.data.message.ChatMessage message : memory.messages()) {
            if (message instanceof dev.langchain4j.data.message.AiMessage) {
                List<dev.langchain4j.agent.tool.ToolExecutionRequest> requests =
                        ((dev.langchain4j.data.message.AiMessage) message).toolExecutionRequests();
                if (requests != null) pending.addAll(requests);
            } else if (message instanceof dev.langchain4j.data.message.ToolExecutionResultMessage) {
                completed.add(((dev.langchain4j.data.message.ToolExecutionResultMessage) message).id());
            }
        }
        for (dev.langchain4j.agent.tool.ToolExecutionRequest request : pending) {
            if (!completed.contains(request.id())) {
                memory.add(dev.langchain4j.data.message.ToolExecutionResultMessage.from(
                        request.id(), request.name(), JSON.toJSONString(halted)));
            }
        }
    }

    /**
     * continue 接口只需要提交 operationId 和确认/补充字段，目标从持久化主记录恢复。
     * 这样页面刷新、服务重启后仍可继续原自主会话，而不会因为 goal 为空直接失败。
     */
    private AgentRunRequest resolveSavedRequest(AgentRunRequest request, AgentToolContext context) {
        if (request == null) request = new AgentRunRequest();
        if (StringUtils.isBlank(request.getOperationId())) return request;
        try {
            AiAgentOperationDO operation = operationService.get(request.getOperationId());
            if (operation == null || StringUtils.isBlank(operation.getRequestJson())) return request;
            AgentRunRequest saved = JSON.parseObject(operation.getRequestJson(), AgentRunRequest.class);
            if (saved == null) return request;
            if (StringUtils.isBlank(request.getGoal())) request.setGoal(saved.getGoal());
            if (StringUtils.isBlank(request.getConversationId())) request.setConversationId(saved.getConversationId());
            if (request.getSessionId() == null) request.setSessionId(saved.getSessionId());
            if (request.getSpaceId() == null) request.setSpaceId(saved.getSpaceId());
            if (StringUtils.isBlank(request.getSpaceCode())) request.setSpaceCode(saved.getSpaceCode());
            if (request.getMaxTurns() == null) request.setMaxTurns(saved.getMaxTurns());
            return request;
        } catch (Exception ex) {
            return request;
        }
    }

    private String appendOperationContext(String operationId, String prompt, AgentToolContext context) {
        if (StringUtils.isBlank(operationId) || StringUtils.isBlank(prompt)) return prompt;
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null) return prompt;
            StringBuilder previous = new StringBuilder();
            if (operation.getDatasourceId() != null) previous.append("datasourceId=").append(operation.getDatasourceId()).append(", ");
            if (StringUtils.isNotBlank(operation.getDatasourceName())) previous.append("datasourceName=").append(operation.getDatasourceName()).append(", ");
            if (StringUtils.isNotBlank(operation.getDatabaseName())) previous.append("databaseName=").append(operation.getDatabaseName()).append(", ");
            if (operation.getOntologyId() != null) previous.append("ontologyId=").append(operation.getOntologyId()).append(", ");
            if (operation.getActionCount() != null) previous.append("actionCount=").append(operation.getActionCount()).append(", ");
            if (StringUtils.isNotBlank(operation.getOutputJson())) {
                List<String> output = JSON.parseArray(operation.getOutputJson(), String.class);
                if (output != null && !output.isEmpty()) previous.append("上一轮结果=").append(String.join("；", output));
            }
            if (previous.length() == 0) return prompt;
            String instruction = "上一轮同一 Agent 会话已产生以下平台上下文（不要重新询问用户已明确提供的信息）："
                    + previous;
            if (operation.getDatasourceId() != null || StringUtils.isNotBlank(operation.getDatasourceName())) {
                instruction += "\n当前会话已有明确目标数据源；若本轮目标是探查元数据，直接围绕该数据源继续，不能退回让用户从全部数据源中重新选择。";
            }
            if (operation.getOntologyId() != null) {
                instruction += "\n当前会话已有 ontologyId=" + operation.getOntologyId()
                        + "；若用户要求生成动作定义，应将它作为 generate_actions 的 ontologyId，不能调用 submit_action；本轮不得为了生成动作再次探查或运行历史 catalogTaskId。若本轮页面上下文给出另一个 ontologyId，以页面为准。";
            }
            return instruction + "\n本轮用户继续请求：" + prompt;
        } catch (Exception ignored) {
            return prompt;
        }
    }

    /**
     * 确认续跑时把上一次被治理层拦截的 Tool 参数显式带回 Prompt。
     * 仅依赖聊天记忆会在页面刷新、服务重启或模型裁剪上下文时丢失 previewIndex 等关键字段。
     */
    private String appendPendingConfirmationArguments(String operationId, String prompt) {
        if (StringUtils.isBlank(operationId) || StringUtils.isBlank(prompt)) return prompt;
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null || StringUtils.isBlank(operation.getResultJson())) return prompt;
            JSONObject root = JSON.parseObject(operation.getResultJson());
            JSONArray toolResults = root == null ? null : root.getJSONArray("toolResults");
            if (toolResults == null) return prompt;
            for (int i = toolResults.size() - 1; i >= 0; i--) {
                JSONObject toolResult = toolResults.getJSONObject(i);
                if (toolResult == null || !AgentToolResult.NEED_CONFIRMATION.equals(toolResult.getString("status"))) continue;
                JSONObject metadata = toolResult.getJSONObject("metadata");
                Object arguments = metadata == null ? null : metadata.get("arguments");
                if (arguments == null) continue;
                return prompt + "\n上一轮被拦截的写 Tool 原始参数如下，请在确认后原样重放，不要改名、改类型或丢失 previewIndex："
                        + JSON.toJSONString(arguments);
            }
        } catch (Exception ignored) {
            // Prompt 增强失败不能阻断正常的确认续跑。
        }
        return prompt;
    }

    private boolean isContinuation(AgentToolContext context) {
        if (context == null || context.getAttributes() == null) return false;
        Object value = context.getAttributes().get("continuation");
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    @Override
    public AgentRunResponse start(AgentRunRequest request, AgentToolContext context) {
        boolean continuation = request != null && StringUtils.isBlank(request.getGoal())
                && StringUtils.isNotBlank(request.getOperationId());
        if (continuation) request.setContinuation(Boolean.TRUE);
        request = resolveSavedRequest(request, context);
        if (request == null || StringUtils.isBlank(request.getGoal())) {
            return AgentRunResponse.failure("缺少 Agent 目标");
        }
        String operationId = context == null ? request.getOperationId() : context.getOperationId();
        if (StringUtils.isBlank(operationId)) operationId = UUID.randomUUID().toString();
        request.setOperationId(operationId);
        if (StringUtils.isBlank(request.getConversationId())) {
            try {
                AiAgentOperationDO existing = operationService.get(operationId);
                if (existing != null && StringUtils.isNotBlank(existing.getRequestJson())) {
                    AgentRunRequest saved = JSON.parseObject(existing.getRequestJson(), AgentRunRequest.class);
                    request.setConversationId(saved.getConversationId());
                }
            } catch (Exception ignored) {
                // 使用新的会话 ID 作为最后兜底。
            }
            if (StringUtils.isBlank(request.getConversationId())) request.setConversationId(UUID.randomUUID().toString());
        }
        final String finalOperationId = operationId;
        if (continuation && context != null && context.getAttributes() != null) {
            try {
                context.getAttributes().put("continuation", Boolean.TRUE);
            } catch (Exception ignored) {
                // 只读属性 Map 不影响继续执行。
            }
        }
        AgentToolContext runContext = context == null
                ? new AgentToolContext(finalOperationId, null, null, request.getSpaceId(), request.getSpaceCode(), Collections.emptyMap())
                : context;
        ensureOperation(operationId, request, runContext);
        if (continuation) markOperationRunning(operationId);
        final AgentToolContext finalContext = runContext;
        final AgentRunRequest executionRequest = request;
        CompletableFuture.runAsync(() -> {
            try {
                run(executionRequest, finalContext);
            } catch (Exception ex) {
                String detail = describeException(ex);
                log.error("Agent 后台执行异常，operationId={}", finalOperationId, ex);
                AgentRunResponse failure = AgentRunResponse.failure("Agent 执行异常: " + detail);
                failure.setOperationId(finalOperationId);
                persistOperation(finalOperationId, executionRequest, failure);
            }
        });
        AgentRunResponse accepted = new AgentRunResponse();
        accepted.setSuccess(true);
        accepted.setStatus(AgentToolResult.RUNNING);
        accepted.setMessage("Agent 已提交后台执行");
        accepted.setOperationId(finalOperationId);
        accepted.setConversationId(request.getConversationId());
        accepted.setSessionId(request.getSessionId());
        return accepted;
    }

    private void markOperationRunning(String operationId) {
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null) return;
            if (com.datamaster.module.ai.controller.agent.tool.metricdraft.MetricDraftConfirmation.pending(
                    operation.getStatus(), operation.getResultJson()) != null) return;
            operation.setStatus(AgentToolResult.RUNNING);
            operation.setMessage("已确认，Agent 正在继续调用平台 Tool");
            operation.setUpdateTime(new java.util.Date());
            operationService.update(operation);
        } catch (Exception ignored) {
            // 后台 run 仍会在下一次 Tool 调用时写入最终状态。
        }
    }

    private void ensureOperation(String operationId, AgentRunRequest request, AgentToolContext context) {
            AiAgentOperationDO existing = operationService.get(operationId);
            if (existing != null) {
                requireAccess(existing, context);
                return;
            }
            AiAgentOperationDO operation = new AiAgentOperationDO();
            operation.setId(operationId);
            operation.setGoal(request.getGoal());
            operation.setDatabaseName(inferDatabaseName(request.getGoal()));
            operation.setSpaceId(context == null ? request.getSpaceId() : context.getSpaceId());
            operation.setSpaceCode(context == null ? request.getSpaceCode() : context.getSpaceCode());
            operation.setStatus(AgentToolResult.RUNNING);
            operation.setProgress(0);
            operation.setCurrentStep("自主 Tool 调度");
            operation.setMessage("Agent 已开始自主规划");
            operation.setRequestJson(JSON.toJSONString(request));
            operation.setCreatorId(context == null ? null : context.getUserId());
            operation.setCreateBy(context == null ? null : context.getUserName());
            operation.setCreateTime(new java.util.Date());
            operation.setUpdateTime(new java.util.Date());
            operation.setDelFlag(false);
            operationService.create(operation);
    }

    private void requireAccess(AiAgentOperationDO operation, AgentToolContext context) {
        boolean spaceMatch = context != null
                && (context.getSpaceId() == null || Objects.equals(operation.getSpaceId(), context.getSpaceId()))
                && (context.getSpaceCode() == null || Objects.equals(operation.getSpaceCode(), context.getSpaceCode()));
        boolean membership = operation.getSpaceId() == null && StringUtils.isBlank(operation.getSpaceCode())
                ? true
                : spaceApi != null && context != null
                && spaceApi.hasUserSpace(context.getUserId(), operation.getSpaceId(), operation.getSpaceCode());
        if (context == null || context.getUserId() == null
                || !membership
                || !Objects.equals(operation.getCreatorId(), context.getUserId()) || !spaceMatch) {
            throw new ServiceException("流程不属于当前用户和空间");
        }
    }

    @Override
    public AgentRunResponse get(String operationId, AgentToolContext context) {
        AiAgentOperationDO operation = operationService.get(operationId);
        if (operation == null) throw new ServiceException("流程不存在");
        requireAccess(operation, context);
        AgentRunResponse response = operation.getResultJson() == null
                ? new AgentRunResponse() : JSON.parseObject(operation.getResultJson(), AgentRunResponse.class);
        response.setOperationId(operationId);
        response.setStatus(operation.getStatus());
        response.setMessage(operation.getMessage());
        response.setProgress(operation.getProgress());
        response.setCurrentStep(operation.getCurrentStep());
        response.setOutput(operation.getOutputJson() == null ? new ArrayList<>()
                : JSON.parseArray(operation.getOutputJson(), String.class));
        if (StringUtils.isNotBlank(response.getDelegateOperationId())) {
            AiAgentOperationDO delegate = operationService.get(response.getDelegateOperationId());
            if (delegate != null) {
                requireAccess(delegate, context);
                response.setStatus(delegate.getStatus());
                response.setMessage(delegate.getMessage());
                response.setProgress(delegate.getProgress());
                response.setCurrentStep(delegate.getCurrentStep());
                response.setConfirmationType(delegate.getConfirmationType());
                response.setConfirmationMessage(delegate.getConfirmationMessage());
                response.setRequiredFields(delegate.getRequiredFieldsJson() == null ? new ArrayList<>()
                        : JSON.parseArray(delegate.getRequiredFieldsJson(), String.class));
                response.setOutput(delegate.getOutputJson() == null ? new ArrayList<>()
                        : JSON.parseArray(delegate.getOutputJson(), String.class));
                operation.setStatus(delegate.getStatus());
                operation.setProgress(delegate.getProgress());
                operation.setCurrentStep(delegate.getCurrentStep());
                operation.setMessage(delegate.getMessage());
                operation.setResultJson(JSON.toJSONString(response));
                operationService.update(operation);
            }
        }
        response.setSuccess(AgentToolResult.SUCCEEDED.equals(response.getStatus()) || AgentToolResult.RUNNING.equals(response.getStatus()));
        return response;
    }

    private void persistOperation(String operationId, AgentRunRequest request, AgentRunResponse response) {
        try {
            AiAgentOperationDO operation = operationService.get(operationId);
            if (operation == null) return;
            mergePersistedToolResults(operation, response);
            operation.setStatus(response.getStatus());
            if (AgentToolResult.SUCCEEDED.equals(response.getStatus())) {
                operation.setProgress(100);
            } else if (response.getToolResults() != null && !response.getToolResults().isEmpty()) {
                operation.setProgress(Math.min(95, 5 + response.getToolResults().size() * 5));
            }
            operation.setCurrentStep("自主 Tool 调度");
            operation.setMessage(response.getMessage() == null ? response.getText() : response.getMessage());
            operation.setOutputJson(JSON.toJSONString(buildOutputMessages(response)));
            operation.setStepsJson(JSON.toJSONString(buildOperationSteps(response)));
            operation.setResultJson(JSON.toJSONString(response));
            operation.setRequestJson(JSON.toJSONString(request));
            copyResourceSummary(operation, response);
            operation.setUpdateTime(new java.util.Date());
            operationService.update(operation);
        } catch (Exception ignored) {
            // 流程记录失败不能覆盖 Agent 返回结果。
        }
    }

    /**
     * 保留同一 operation 的历史只读事实（尤其是 generate_actions 预览）。
     * 确认拦截结果不能覆盖预览，否则用户点击确认后 create_action 无法恢复原始动作。
     */
    private void mergePersistedToolResults(AiAgentOperationDO operation, AgentRunResponse response) {
        if (operation == null || response == null) return;
        List<AgentToolResult> existing = new ArrayList<>();
        if (StringUtils.isNotBlank(operation.getResultJson())) {
            try {
                AgentRunResponse saved = JSON.parseObject(operation.getResultJson(), AgentRunResponse.class);
                if (saved != null && saved.getToolResults() != null) existing.addAll(saved.getToolResults());
            } catch (Exception ignored) {
                // 历史结果损坏时仍保存当前结果。
            }
        }
        List<AgentToolResult> current = response.getToolResults();
        if (current == null) current = new ArrayList<>();
        List<AgentToolResult> merged = new ArrayList<>();
        for (AgentToolResult item : existing) addDistinctToolResult(merged, item);
        for (AgentToolResult item : current) addDistinctToolResult(merged, item);
        response.setToolResults(merged);
    }

    private void addDistinctToolResult(List<AgentToolResult> target, AgentToolResult candidate) {
        if (candidate == null) return;
        String candidateJson = JSON.toJSONString(candidate);
        for (AgentToolResult existing : target) {
            if (JSON.toJSONString(existing).equals(candidateJson)) return;
        }
        target.add(candidate);
    }

    private List<String> buildOutputMessages(AgentRunResponse response) {
        List<String> output = new ArrayList<>();
        if (response == null) return output;
        if (response.getToolResults() != null) {
            for (AgentToolResult toolResult : response.getToolResults()) {
                if (toolResult == null) continue;
                String message = toolResult.getMessage();
                if (message == null || message.trim().isEmpty()) message = toolResult.getStatus();
                output.add(message);
            }
        }
        if (response.getText() != null && !response.getText().trim().isEmpty()) output.add(response.getText());
        if (output.isEmpty() && response.getMessage() != null) output.add(response.getMessage());
        return output;
    }

    private List<Map<String, String>> buildOperationSteps(AgentRunResponse response) {
        List<Map<String, String>> steps = new ArrayList<>();
        if (response == null || response.getToolResults() == null) return steps;
        for (AgentToolResult toolResult : response.getToolResults()) {
            if (toolResult == null) continue;
            Map<String, String> step = new LinkedHashMap<>();
            step.put("title", StringUtils.isBlank(toolResult.getToolName()) ? "Agent Tool" : toolResult.getToolName());
            step.put("status", toolResult.getStatus());
            steps.add(step);
        }
        return steps;
    }

    @SuppressWarnings("unchecked")
    private void copyResourceSummary(AiAgentOperationDO operation, AgentRunResponse response) {
        if (operation == null || response == null || response.getToolResults() == null) return;
        for (AgentToolResult toolResult : response.getToolResults()) {
            if (toolResult == null || !(toolResult.getData() instanceof Map)) continue;
            Map<String, Object> data = (Map<String, Object>) toolResult.getData();
            String toolName = toolResult.getToolName();
            boolean datasourceTool = "get_datasource".equals(toolName)
                    || "create_datasource".equals(toolName)
                    || "update_datasource".equals(toolName)
                    || "bind_datasource_space".equals(toolName);
            Object datasourceId = data.get("datasourceId");
            if (datasourceId == null && datasourceTool) datasourceId = data.get("id");
            Long resolvedDatasourceId = toLong(datasourceId);
            // 查询其他数据源只是读取，不能改变当前会话目标；创建、更新、绑定以及
            // 已通过一致性守卫的探查结果才可以建立或刷新目标数据源上下文。
            boolean authoritativeDatasourceResult = "create_datasource".equals(toolName)
                    || "update_datasource".equals(toolName)
                    || "bind_datasource_space".equals(toolName)
                    || "create_probe_task".equals(toolName)
                    || "publish_probe_task".equals(toolName)
                    || "run_probe_task".equals(toolName);
            String resultDatasourceName = data.get("datasourceName") == null ? null
                    : String.valueOf(data.get("datasourceName"));
            String resultDatabaseName = data.get("databaseName") == null ? null
                    : String.valueOf(data.get("databaseName"));
            boolean matchesExistingName = StringUtils.isBlank(operation.getDatasourceName())
                    && StringUtils.isBlank(operation.getDatabaseName())
                    || StringUtils.isNotBlank(resultDatasourceName)
                    && resultDatasourceName.equalsIgnoreCase(operation.getDatasourceName())
                    || StringUtils.isNotBlank(resultDatabaseName)
                    && resultDatabaseName.equalsIgnoreCase(operation.getDatabaseName());
            boolean sameDatasource = resolvedDatasourceId != null
                    && (operation.getDatasourceId() != null
                    ? operation.getDatasourceId().equals(resolvedDatasourceId) : matchesExistingName);
            boolean mayRefreshDatasource = authoritativeDatasourceResult || sameDatasource;
            if (resolvedDatasourceId != null && mayRefreshDatasource) operation.setDatasourceId(resolvedDatasourceId);
            if (operation.getDatasourceId() != null) {
                setDatasourceFlowKeySafely(operation, operation.getDatasourceId(), operation.getCreatorId());
            }
            // Tool 回读或更新后的值是当前事实，必须覆盖旧上下文。否则连接从
            // datamaster_movie 改为 datamaster_ai_decision 后仍会探查旧数据库。
            if (mayRefreshDatasource && data.get("datasourceName") != null
                    && StringUtils.isNotBlank(String.valueOf(data.get("datasourceName")))) {
                operation.setDatasourceName(String.valueOf(data.get("datasourceName")));
            }
            if (mayRefreshDatasource && data.get("databaseName") != null
                    && StringUtils.isNotBlank(String.valueOf(data.get("databaseName")))) {
                operation.setDatabaseName(String.valueOf(data.get("databaseName")));
            }
            if (operation.getCatalogTaskId() == null) operation.setCatalogTaskId(toLong(data.get("catalogTaskId")));
            if (operation.getOntologyId() == null) operation.setOntologyId(toLong(data.get("ontologyId")));
            if (operation.getActionCount() == null) operation.setActionCount(toInteger(data.get("actionCount")));
        }
    }

    private String inferDatabaseName(String goal) {
        if (StringUtils.isBlank(goal)) return null;
        Matcher matcher = Pattern.compile("(?i)(?:databaseName|数据库名称|数据库|dbname)\\s*[:=：]?\\s*[\\\"']?([A-Za-z0-9_.-]+)").matcher(goal);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return value == null ? null : Integer.valueOf(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private AgentToolResult lastTerminalResult(List<AgentToolResult> results) {
        if (results == null) return null;
        for (int i = results.size() - 1; i >= 0; i--) {
            AgentToolResult result = results.get(i);
            if (result == null) continue;
            if (AgentToolResult.NEED_INPUT.equals(result.getStatus())
                    || AgentToolResult.NEED_CONFIRMATION.equals(result.getStatus())
                    || AgentToolResult.FAILED.equals(result.getStatus())) return result;
        }
        return null;
    }

    private ChatLanguageModel createModel() {
        AiSkillModelProperties modelProperties = activeModelResolver.resolve(this.modelProperties);
        if (!Boolean.TRUE.equals(modelProperties.getEnabled()) || StringUtils.isBlank(modelProperties.getApiUrl())) {
            return null;
        }
        String apiKey = modelProperties.getApiKey();
        if (StringUtils.isNotBlank(apiKey) && looksLikeEncryptedApiKey(apiKey)) {
            try {
                String decrypted = AesEncryptUtil.desEncrypt(apiKey);
                if (StringUtils.isNotBlank(decrypted)) apiKey = decrypted.trim();
            } catch (Exception ignored) {
                // 未加密的 API Key 直接使用。
            }
        }
        Integer timeout = modelProperties.getTimeout() == null ? 180000 : modelProperties.getTimeout();
        Integer retries = modelProperties.getRetryCount() == null ? 2 : modelProperties.getRetryCount();
        return new com.datamaster.module.ai.service.gateway.GovernedChatLanguageModel(OpenAiChatModel.builder()
                .baseUrl(normalizeBaseUrl(modelProperties.getApiUrl()))
                .apiKey(StringUtils.isBlank(apiKey) ? "none" : apiKey)
                .modelName(StringUtils.isBlank(modelProperties.getModel()) ? "qwen-plus" : modelProperties.getModel())
                .temperature(modelProperties.getTemperature() == null ? 0.2D : modelProperties.getTemperature())
                .maxTokens(modelProperties.getMaxTokens() == null ? 8192 : modelProperties.getMaxTokens())
                .timeout(Duration.ofMillis(Math.max(1000, timeout)))
                .maxRetries(Math.max(0, retries))
                .parallelToolCalls(false)
                .strictTools(false)
                .build(), invocationGateway, "DATA_AGENT", modelProperties.getProvider(),
                StringUtils.isBlank(modelProperties.getModel()) ? "qwen-plus" : modelProperties.getModel());
    }

    /**
     * 只有看起来像 DataMaster AES 密文时才尝试解密。
     * 明文 API Key、截断密文或旧配置中的普通字符串不能交给 AES/CBC/NoPadding，
     * 否则 AesEncryptUtil 会打印 IllegalBlockSizeException，干扰 Agent 后台执行日志。
     */
    private boolean looksLikeEncryptedApiKey(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0 || text.length() % 4 != 0 || !text.matches("^[A-Za-z0-9+/=]+$")) {
            return false;
        }
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(text);
            return decoded.length > 0 && decoded.length % 16 == 0;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String normalizeBaseUrl(String apiUrl) {
        String url = apiUrl.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) {
            url = url.substring(0, url.length() - "/chat/completions".length());
        }
        return url;
    }

    /**
     * LangChain4j/HTTP 客户端的一些异常 message 为空，只返回“未知错误”会让
     * 用户和运维无法判断是网络、模型配置还是 Tool Schema 问题。只输出异常
     * 类型和非敏感 message，不输出请求体、API Key 或完整响应内容。
     */
    private String describeException(Throwable error) {
        if (error == null) return "未知错误";
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String message = root.getMessage();
        if (StringUtils.isBlank(message)) message = error.getMessage();
        String type = root.getClass().getSimpleName();
        return StringUtils.isBlank(message) ? type : type + ": " + message.trim();
    }

    private interface AutonomousAssistant {
        String chat(String message);
    }
}
