package com.datamaster.module.ai.agent.tool;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Agent Tool 的输入 DTO；业务 ID 必须使用字符串，使框架生成 string Schema，
 * 避免模型/提供方对雪花 ID 做浮点化。业务 Service 的 Long 类型由 Tool 边界转换。
 */
public final class AgentToolRequest {

    private AgentToolRequest() {
    }

    /**
     * LangChain4j 0.35.0 的 Tool Schema 生成器无法处理 List<Map<...>>：
     * 它会把 ParameterizedType 强制转换成 Class。使用具体 DTO 既保留结构化
     * 参数，又避免框架在启动生成 Schema 时触发该缺陷。
     */
    @Data
    public static class ConversationHistoryItem {
        @Schema(description = "消息角色，例如 user 或 assistant")
        private String role;
        @Schema(description = "消息内容")
        private String content;
    }

    @Data
    public static class DbGptQuery {
        @NotBlank
        @Schema(description = "用户问题", requiredMode = Schema.RequiredMode.REQUIRED)
        private String question;
        @Schema(description = "聚焦的资产 ID，可选")
        private String assetId;
        @Schema(description = "数据源 ID，可选")
        private String datasourceId;
        @Schema(description = "模型名称，可选")
        private String model;
        @Schema(description = "DB-GPT 对话模式，可选")
        private String chatMode;
        @Schema(description = "回答模式：qa 或 decision")
        private String responseMode;
        @Schema(description = "指定使用的 Skill ID 列表，可选")
        private List<String> skillIds;
        @Schema(description = "是否返回 SQL")
        private Boolean returnSql;
        @Schema(description = "最近几轮会话上下文")
        private List<ConversationHistoryItem> history;
    }

    @Data
    public static class DbGptReport {
        @NotBlank
        @Schema(description = "用户报告需求", requiredMode = Schema.RequiredMode.REQUIRED)
        private String question;
        @NotBlank
        @Schema(description = "数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceId;
        @Schema(description = "Skill ID，可选")
        private String skillId;
        @Schema(description = "报告模板 ID，可选")
        private String templateId;
        @Schema(description = "模型名称，可选")
        private String model;
        @Schema(description = "是否返回 SQL")
        private Boolean returnSql;
        @Schema(description = "报告参数")
        private Map<String, Object> params;
        @Schema(description = "最近几轮会话上下文")
        private List<ConversationHistoryItem> history;
    }

    @Data
    public static class GenerateReportTemplate {
        @NotBlank
        @Schema(description = "关联 Skill ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String skillId;
        @NotBlank
        @Schema(description = "模板需求描述", requiredMode = Schema.RequiredMode.REQUIRED)
        private String prompt;
    }

    @Data
    public static class AuthorizeAssets {
        @NotBlank
        @Schema(description = "数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceId;
        @NotBlank
        @Schema(description = "数据库名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String databaseName;
        @Schema(description = "模式名，可选")
        private String schemaName;
    }

    @Data
    public static class CreateDatasource {
        @NotBlank
        @Schema(description = "数据源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceName;
        @Schema(description = "复制连接配置的已有数据源名称，例如 datamaster_movie；平台在后台复制加密配置，不会把密码返回给模型")
        private String sourceDatasourceName;
        @NotBlank
        @Schema(description = "数据库类型，例如 PostgreSQL", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceType;
        @Schema(description = "连接配置，例如 username、password、dbname")
        private Map<String, Object> datasourceConfig;
        @NotBlank
        @Schema(description = "数据库地址", requiredMode = Schema.RequiredMode.REQUIRED)
        private String ip;
        @Schema(description = "数据库端口", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer port;
        @Schema(description = "数据库名称，可用于连接去重")
        private String databaseName;
        @Schema(description = "数据库模式；PostgreSQL 默认 public")
        private String schemaName;
    }

    @Data
    public static class BindDatasourceSpace {
        @NotBlank
        @Schema(description = "要绑定的数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceId;
    }

    @Data
    public static class CreateProbeTask {
        @NotBlank
        @Schema(description = "已有数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceId;
        @NotBlank
        @Schema(description = "要探查的数据库名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String databaseName;
        @Schema(description = "模式名；PostgreSQL 未填写时使用 public，其他数据库按默认规则处理")
        private String schemaName;
        @Schema(description = "任务名称，可选")
        private String name;
    }

    @Data
    public static class DeleteDatasource {
        @NotBlank
        @Schema(description = "要删除的数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceId;
    }

    @Data
    public static class ExecuteAction {
        @NotBlank
        @Schema(description = "已提交的动作执行记录 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String executionId;
        @Schema(description = "人工执行阶段补充或覆盖的引用参数 JSON，可选")
        private String inputParams;
    }

    @Data
    public static class GenerateActions {
        @Schema(description = "本体 ID；当前页面或流程已有本体时可省略，由平台上下文补齐")
        private String ontologyId;
        @NotBlank
        @Schema(description = "动作功能描述", requiredMode = Schema.RequiredMode.REQUIRED)
        private String prompt;
        @Schema(description = "动作类型，可选 COMPOSITE/FUNCTION；未明确要求脚本时默认 COMPOSITE")
        private List<String> actionTypes;
    }

    @Data
    public static class GenerateActionWorkflow {
        @NotBlank
        @Schema(description = "本体 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String ontologyId;
        @NotBlank
        @Schema(description = "编排名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;
        @Schema(description = "动作 ID 列表，按执行顺序排列", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<String> actionIds;
        @Schema(description = "编排编码，可选")
        private String code;
        @Schema(description = "触发器引用，可选")
        private String triggerRef;
        @Schema(description = "编排描述，可选")
        private String description;
    }

    @Data
    public static class ListActionWorkflows {
        @Schema(description = "本体 ID；当前页面或流程已有本体时可省略")
        private String ontologyId;
        @Schema(description = "编排名称，支持模糊匹配，可选")
        private String name;
        @Schema(description = "编排状态，可选")
        private String status;
    }

    @Data
    public static class UpdateActionWorkflow {
        @NotBlank @Schema(description = "动作编排ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String workflowId;
        @Schema(description = "编排名称，可选") private String name;
        @Schema(description = "编排编码，可选") private String code;
        @Schema(description = "触发器引用，可选") private String triggerRef;
        @Schema(description = "编排描述，可选") private String description;
    }

    @Data
    public static class DeleteActionWorkflow {
        @NotBlank @Schema(description = "动作编排ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String workflowId;
    }

    @Data
    public static class GetActionWorkflow {
        @NotBlank
        @Schema(description = "动作编排 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String workflowId;
    }

    @Data
    public static class GenerateOntology {
        @Schema(description = "数据源 ID；当前流程已有目标数据源时可省略，由平台上下文补齐")
        private String datasourceId;
        @Schema(description = "已有本体 ID，可选；用于在现有本体上继续生成")
        private String ontologyId;
    }

    @Data
    public static class GenerateSkill {
        @Schema(description = "本体 ID；当前页面或流程已有本体时可省略，由平台上下文补齐")
        private String ontologyId;
        @Schema(description = "是否发布 Skill，默认 false")
        private Boolean publish;
        @Schema(description = "补充生成要求，可选")
        private String manualNotes;
    }

    @Data
    public static class GetActionExecution {
        @NotBlank
        @Schema(description = "动作执行记录 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String executionId;
    }

    @Data
    public static class GetAgentOperation {
        @NotBlank
        @Schema(description = "流程 operationId", requiredMode = Schema.RequiredMode.REQUIRED)
        private String operationId;
    }

    @Data
    public static class GetDatasource {
        @Schema(description = "数据源 ID；不传则返回数据源列表")
        private String datasourceId;
        @Schema(description = "数据源名称；可按名称查询，例如 datamaster_movie")
        private String datasourceName;
    }

    @Data
    public static class GetOntology {
        @Schema(description = "本体 ID；当前页面或流程已有本体时可省略，由平台上下文补齐")
        private String ontologyId;
    }

    @Data
    public static class SearchOntologies {
        @Schema(description = "本体ID，精确匹配，可选")
        private String ontologyId;
        @Schema(description = "本体名称，支持模糊匹配，可选")
        private String name;
        @Schema(description = "本体编码，支持模糊匹配，可选")
        private String code;
        @Schema(description = "状态：0=草稿 1=已发布 2=已归档，可选")
        private Integer status;
        @Schema(description = "最多返回条数，默认20")
        private Integer limit;
    }

    @Data
    public static class CreateOntology {
        @NotBlank @Schema(description = "本体名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;
        @NotBlank @Schema(description = "本体编码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String code;
        @Schema(description = "本体描述，可选")
        private String description;
        @Schema(description = "状态：0=草稿，1=已发布，2=已归档，默认0")
        private Integer status;
    }

    @Data
    public static class UpdateOntology {
        @Schema(description = "本体ID；当前页面或流程已有本体时可省略")
        private String ontologyId;
        @Schema(description = "新的本体名称，可选")
        private String name;
        @Schema(description = "新的本体编码，可选")
        private String code;
        @Schema(description = "新的本体描述，可选")
        private String description;
        @Schema(description = "状态：0=草稿，1=已发布，2=已归档，可选")
        private Integer status;
    }

    @Data
    public static class DeleteOntology {
        @Schema(description = "本体ID；当前页面或流程已有本体时可省略")
        private String ontologyId;
    }

    @Data
    public static class GetProbeTask {
        @Schema(description = "探查任务 ID；当前流程已有探查任务时可省略，由流程上下文补齐")
        private String taskId;
    }

    @Data
    public static class ListActions {
        @Schema(description = "本体 ID；当前页面或流程已有本体时可省略，由平台上下文补齐")
        private String ontologyId;
        @Schema(description = "动作ID，精确匹配，可选")
        private String actionId;
        @Schema(description = "动作名称，支持模糊匹配，可选")
        private String name;
        @Schema(description = "动作类型，可选")
        private String actionType;
        @Schema(description = "最多返回条数，默认100")
        private Integer limit;
    }

    @Data
    public static class CreateAction {
        @NotBlank @Schema(description = "本体ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String ontologyId;
        @Schema(description = "动作名称；使用 previewIndex 保存预览时可省略")
        private String name;
        @Schema(description = "动作类型；使用 previewIndex 保存预览时可省略")
        private String actionType;
        @Schema(description = "触发概念ID，可选") private String conceptId;
        @Schema(description = "动作描述，可选") private String description;
        @Schema(description = "执行步骤 JSON，可选") private String executionSteps;
        @Schema(description = "是否需要人工审批，默认 true") private Boolean needsApproval;
        @Schema(description = "参数配置 JSON，可选") private String paramConfig;
        @Schema(description = "动作预览索引；确认保存最近一次 generate_actions 结果时优先传此字段，避免重新回显长脚本")
        private String previewIndex;
        @Schema(description = "FUNCTION 动作绑定的共享函数 ID，可选") private String functionId;
        @Schema(description = "FUNCTION 动作代码体；通常由 previewIndex 自动带入") private String functionBody;
        @Schema(description = "FUNCTION 动作语言：TYPESCRIPT/PYTHON") private String functionLang;
        @Schema(description = "FUNCTION 动作参数 JSON 数组") private String functionParams;
        @Schema(description = "FUNCTION 数据来源概念 ID") private String sourceConceptId;
        @Schema(description = "FUNCTION 关联关系 ID 列表") private List<String> sourceRelationIds;
        @Schema(description = "FUNCTION 输出目标概念 ID") private String outputConceptId;
        @Schema(description = "FUNCTION 读取行数上限") private Integer readLimit;
    }

    @Data
    public static class UpdateAction {
        @Schema(description = "动作ID；必须提供真实 actionId") private String actionId;
        @Schema(description = "动作名称，可选") private String name;
        @Schema(description = "动作类型，可选") private String actionType;
        @Schema(description = "触发概念ID，可选") private String conceptId;
        @Schema(description = "动作描述，可选") private String description;
        @Schema(description = "执行步骤 JSON，可选") private String executionSteps;
        @Schema(description = "是否需要人工审批，可选") private Boolean needsApproval;
        @Schema(description = "参数配置 JSON，可选") private String paramConfig;
    }

    @Data
    public static class DeleteAction {
        @NotBlank @Schema(description = "动作ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String actionId;
    }

    @Data
    public static class ListAssets {
        @Schema(description = "数据源 ID，可选")
        private String datasourceId;
        @Schema(description = "资产或物理表名称，可选")
        private String tableName;
        @Schema(description = "资产展示名称，可选")
        private String name;
    }

    @Data
    public static class ListDatasources {
    }

    @Data
    public static class ListPendingApprovals {
        @NotBlank
        @Schema(description = "本体 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String ontologyId;
    }

    @Data
    public static class PublishProbeTask {
        @Schema(description = "探查任务 ID；当前流程已有探查任务时可省略，由流程上下文补齐")
        private String taskId;
    }

    @Data
    public static class RunProbeTask {
        @Schema(description = "探查任务 ID；当前流程已有探查任务时可省略，由流程上下文补齐")
        private String taskId;
    }

    @Data
    public static class StandardDataOnboarding {
        @NotBlank
        @Schema(description = "用户的数据接入目标", requiredMode = Schema.RequiredMode.REQUIRED)
        private String goal;
        @Schema(description = "已有数据源 ID，可选")
        private String datasourceId;
        @Schema(description = "数据源名称，可选")
        private String datasourceName;
        @Schema(description = "数据库名称")
        private String databaseName;
        @Schema(description = "模式名，未填写时按数据库规则处理")
        private String schemaName;
        @Schema(description = "数据库类型，例如 PostgreSQL")
        private String datasourceType;
        @Schema(description = "连接配置，例如 username、password、dbname")
        private Map<String, Object> datasourceConfig;
        @Schema(description = "数据库地址")
        private String ip;
        @Schema(description = "数据库端口")
        private Long port;
    }

    @Data
    public static class SubmitAction {
        @NotBlank
        @Schema(description = "动作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String actionId;
        @Schema(description = "动作输入参数 JSON")
        private String inputParams;
        @Schema(description = "触发对象主键")
        private String objectKey;
        @Schema(description = "幂等键；不传时由平台生成")
        private String idempotencyKey;
        @Schema(description = "触发类型")
        private String triggerType;
        @Schema(description = "触发器引用")
        private String triggerRef;
    }

    @Data
    public static class SyncAgentCapability {
        @Schema(description = "要同步的数据源 ID，可选")
        private String datasourceId;
    }

    @Data
    public static class SyncAssets {
        @NotBlank
        @Schema(description = "数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceId;
        @Schema(description = "数据库名称，可选；未填写时从数据源连接配置自动读取")
        private String databaseName;
        @Schema(description = "模式名，可选；未填写时从数据源连接配置读取，PostgreSQL 默认 public")
        private String schemaName;
        @Schema(description = "目标资产目录编码，可选；未填写时自动选择当前空间的资产目录")
        private String catCode;
    }

    @Data
    public static class UpdateDatasource {
        @NotBlank
        @Schema(description = "要更新的数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceId;
        @Schema(description = "新的数据源名称，可选")
        private String datasourceName;
        @Schema(description = "目标数据库名称；修改连接配置时必须明确提供，不能只修改展示名称")
        private String databaseName;
        @Schema(description = "目标模式；PostgreSQL 默认 public")
        private String schemaName;
        @Schema(description = "数据库类型，可选")
        private String datasourceType;
        @Schema(description = "新的连接配置，可选")
        private Map<String, Object> datasourceConfig;
        @Schema(description = "数据库地址，可选")
        private String ip;
        @Schema(description = "数据库端口，可选")
        private Long port;
        @Schema(description = "描述，可选")
        private String description;
    }

    @Data
    public static class UpdateProbeTask {
        @NotBlank
        @Schema(description = "探查任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String taskId;
        @Schema(description = "任务名称，可选")
        private String name;
        @Schema(description = "数据库名称，可选")
        private String databaseName;
        @Schema(description = "模式名，可选；PostgreSQL 默认 public")
        private String schemaName;
    }

    @Data
    public static class SearchProbeTasks {
        @Schema(description = "探查任务ID，精确匹配，可选") private String taskId;
        @Schema(description = "任务名称，支持模糊匹配，可选") private String name;
        @Schema(description = "数据源ID，可选") private String datasourceId;
        @Schema(description = "任务状态，可选") private String status;
        @Schema(description = "最多返回条数，默认20") private Integer limit;
    }

    @Data
    public static class DeleteProbeTask {
        @NotBlank @Schema(description = "探查任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String taskId;
    }

    @Data
    public static class ListDataServices {
        @Schema(description = "服务名称，支持模糊匹配，可选")
        private String name;
        @Schema(description = "服务状态；默认只返回已发布服务")
        private String status;
        @Schema(description = "最多返回条数，默认20，最大100")
        private Integer limit;
    }

    @Data
    public static class CreateDataService {
        @NotBlank
        @Schema(description = "数据源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String datasourceId;
        @NotBlank
        @Schema(description = "物理表名；可带 schema 前缀，例如 public.test_customer_order",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String tableName;
        @Schema(description = "服务名称；默认根据表名生成")
        private String name;
        @Schema(description = "服务路径；默认按表名生成")
        private String apiUrl;
        @Schema(description = "服务描述，可选")
        private String description;
    }

    @Data
    public static class PublishDataService {
        @NotBlank
        @Schema(description = "数据服务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String serviceId;
    }

    @Data
    public static class GetDataService {
        @Schema(description = "数据服务 ID；可选")
        private String serviceId;
        @Schema(description = "数据服务名称；可按名称查询")
        private String name;
    }

    @Data
    public static class InvokeDataService {
        @NotBlank
        @Schema(description = "已发布数据服务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String serviceId;
        @Schema(description = "数据服务请求参数，例如 pageNum、pageSize 及服务声明的筛选参数")
        private Map<String, Object> params;
    }

    @Data
    public static class ListDataDevelopmentTasks {
        @Schema(description = "任务名称，支持模糊匹配，可选")
        private String name;
        @Schema(description = "任务状态，可选")
        private String status;
        @Schema(description = "最多返回条数，默认20，最大100")
        private Integer limit;
    }

    @Data
    public static class GetDataDevelopmentTask {
        @Schema(description = "数据开发任务 ID；可选")
        private String taskId;
        @Schema(description = "数据开发任务名称；可按名称查询")
        private String name;
    }

    @Data
    public static class StartDataDevelopmentTask {
        @NotBlank
        @Schema(description = "数据开发任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String taskId;
    }

    @Data
    public static class PreviewDataDevelopmentPlan {
        @NotBlank
        @Schema(description = "已有数据开发任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String taskId;
        @NotBlank
        @Schema(description = "计划 JSON；包含 nodes 数组，可选 relations 数组", requiredMode = Schema.RequiredMode.REQUIRED)
        private String planJson;
    }

    @Data
    public static class ApplyDataDevelopmentPlan {
        @NotBlank
        @Schema(description = "已有数据开发任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String taskId;
        @NotBlank
        @Schema(description = "最近一次预览通过的计划 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
        private String planJson;
        @NotBlank
        @Schema(description = "preview_data_development_plan 返回的 planHash", requiredMode = Schema.RequiredMode.REQUIRED)
        private String previewHash;
    }
}
