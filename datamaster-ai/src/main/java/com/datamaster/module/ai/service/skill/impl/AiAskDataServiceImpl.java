package com.datamaster.module.ai.service.skill.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;
import com.datamaster.common.datasource.mgmt.mapper.DatasourceSpaceRelMgmtMapper;
import com.datamaster.common.datasource.mgmt.service.IDatasourceMgmtService;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.ai.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.ai.dal.dataobject.skill.AiSkillReportTemplateDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.ai.dal.mapper.skill.AiSkillMapper;
import com.datamaster.module.ai.dal.mapper.skill.AiSkillReportTemplateMapper;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatCompletionRequest;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatCompletionResponse;
import com.datamaster.module.ai.model.dto.dbgpt.DbGptChatMessage;
import com.datamaster.module.ai.config.DbGptProperties;
import com.datamaster.module.ai.service.dbgpt.IDbGptClientService;
import com.datamaster.module.ai.service.skill.IAiAskDataContextService;
import com.datamaster.module.ai.service.skill.IAiAskDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI ask-data service implementation.
 */
@Service
public class AiAskDataServiceImpl implements IAiAskDataService {

    private static final Logger log = LoggerFactory.getLogger(AiAskDataServiceImpl.class);
    @Resource
    private AiSkillMapper aiSkillMapper;
    @Resource
    private AiSkillReportTemplateMapper aiSkillReportTemplateMapper;
    @Resource
    private IDatasourceMgmtService datasourceMgmtService;
    @Resource
    private DatasourceSpaceRelMgmtMapper datasourceSpaceRelMgmtMapper;
    @Resource
    private AssetsAssetMapper assetsAssetMapper;
    @Resource
    private IAiAskDataContextService aiAskDataContextService;
    @Resource
    private IAssetsTableGovernanceApiService assetsTableGovernanceApiService;

    @Resource
    private IDbGptClientService dbGptClientService;

    @Resource
    private DbGptProperties dbGptProperties;

    private static final Pattern SQL_BLOCK_PATTERN = Pattern.compile(
            "(?i)```sql\\s*\\n?([\\s\\S]*?)\\s*```");

    @Override
    public AiAskDataSqlRespVO chatWithDbGpt(AiAskDataSqlReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getQuestion())) {
            throw new ServiceException("用户问题不能为空");
        }
        checkAskDataGovernance(reqVO);
        if (!"chat_normal".equals(reqVO.getChatMode()) && !looksLikeDataQuestion(reqVO.getQuestion())) {
                // pass through — let AI handle it
            }
        DbGptChatCompletionRequest gptRequest = new DbGptChatCompletionRequest();
        gptRequest.setModel(firstNonBlank(reqVO.getModel(), dbGptProperties.getModel()));
        gptRequest.setTemperature(0.6);
        gptRequest.setMaxTokens(4000);
        gptRequest.setStream(false);
        gptRequest.setChatMode(firstNonBlank(reqVO.getChatMode(), dbGptProperties.getChatMode()));
        gptRequest.setSpaceName(dbGptProperties.getSkillSpaceName());
        gptRequest.setConvUid("datamaster-ai-" + System.currentTimeMillis());
        Long datasourceId = reqVO.getDatasourceId();
        if (reqVO.getDatasourceId() != null) {
            DatasourceDO datasource = datasourceMgmtService.getDatasourceDOById(reqVO.getDatasourceId());
            if (datasource != null) {
                if (datasource.getDbgptDatasourceId() != null) {
                    gptRequest.setDatasourceId(datasource.getDbgptDatasourceId().intValue());
                    // chat_with_db_qa 模式需要以 db_name 作为 chat_param
                    String dbName = dbGptClientService.getDatasourceDbName(datasource.getDbgptDatasourceId().intValue());
                    if (dbName != null) {
                        gptRequest.setChatParam(dbName);
                    }
                }
                gptRequest.setDbName(datasource.getDatasourceName());
                gptRequest.setDbType(datasource.getDatasourceType());
                String schemaName = resolveDatasourceSchema(datasource);
                if (StringUtils.isNotBlank(schemaName)) {
                    gptRequest.getExtra().put("schema", schemaName);
                }
            }
        }
        bindSelectedDbGptSkill(reqVO, gptRequest);
        boolean isGeneralChat = "chat_normal".equals(gptRequest.getChatMode());
        Long userPermissionLevel = currentUserDataPermissionLevel();
        String userInput = buildDbGptUserInput(reqVO, userPermissionLevel);
        if (isGeneralChat) {
            gptRequest.setMessages(Collections.singletonList(
                    new DbGptChatMessage("user", userInput)
            ));
        } else {
            gptRequest.setMessages(Collections.singletonList(
                    new DbGptChatMessage("user", userInput)
            ));
        }

        AiAskDataSqlRespVO respVO = new AiAskDataSqlRespVO();
        respVO.setQuestion(reqVO.getQuestion());
        String reply = "";
        try {
            DbGptChatCompletionResponse gptResponse = dbGptClientService.chatCompletionV1(gptRequest);
            reply = normalizeAskReply(extractReply(gptResponse));
        } catch (Exception e) {
            log.warn("DB-GPT原生问数调用失败：{}", e.getMessage());
            respVO.setQualityWarning(normalizeAgentMessage(e.getMessage()));
        }
        respVO.setExplanation(reply);
        if (StringUtils.isBlank(reply) && StringUtils.isNotBlank(respVO.getQualityWarning())) {
            respVO.setExplanation("决策智能体暂未返回结果：" + respVO.getQualityWarning());
        }
        respVO.setSql(extractSqlFromReply(reply));
        if (!isGeneralChat) {
            try {
                respVO.setReferencedSkills(BeanUtils.toBean(
                        getRelevantSkills(reqVO.getQuestion(), reqVO.getAssetId()), AiSkillRespVO.class));
            } catch (Exception e) {
                log.warn("查询AI Skill失败，跳过：{}", e.getMessage());
            }
        }
        return respVO;
    }

    @Override
    public SseEmitter chatWithDbGptStream(AiAskDataSqlReqVO reqVO) {
        SseEmitter emitter = new SseEmitter(0L);
        Long userPermissionLevel = currentUserDataPermissionLevel();
        CompletableFuture.runAsync(() -> {
            try {
                if (reqVO == null || StringUtils.isBlank(reqVO.getQuestion())) {
                    sendSse(emitter, "error", "用户问题不能为空");
                    emitter.complete();
                    return;
                }
                checkAskDataGovernance(reqVO);
                if (!"chat_normal".equals(reqVO.getChatMode()) && !looksLikeDataQuestion(reqVO.getQuestion())) {
                    // pass through — let AI handle it
                }

                StringBuilder fullReply = new StringBuilder();
                DbGptChatCompletionRequest gptRequest = buildDbGptAskRequest(reqVO, true, userPermissionLevel);
                dbGptClientService.chatCompletionV1Stream(gptRequest, chunk -> {
                    fullReply.append(chunk);
                });
                String rawReply = fullReply.toString();
                String reply = normalizeAskReply(rawReply);
                sendSse(emitter, "message", reply);
                appendAgentStepsFromNativeSteps(rawReply, emitter);
                appendSqlFromNativeSteps(rawReply, emitter);
                sendSse(emitter, "done", "[DONE]");
                emitter.complete();
            } catch (Exception e) {
                log.warn("DB-GPT原生问数流式调用失败：{}", e.getMessage());
                try {
                    sendSse(emitter, "error", "决策智能体调用异常: " + normalizeAgentMessage(e.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private void appendAgentStepsFromNativeSteps(String reply, SseEmitter emitter) throws IOException {
        String steps = extractAgentStepsFromNative(reply);
        if (StringUtils.isNotBlank(steps)) {
            sendSse(emitter, "steps", steps);
        }
    }

    private String extractAgentStepsFromNative(String reply) {
        if (StringUtils.isBlank(reply)) {
            return "";
        }
        List<String> stepTexts = new ArrayList<>();
        List<String> fragments = extractJsonFragments(reply);
        for (String fragment : fragments) {
            try {
                Object parsed = JSON.parse(fragment);
                collectAgentStepTexts(parsed, stepTexts);
            } catch (Exception ignored) {
            }
        }
        return String.join("\n\n", stepTexts).trim();
    }

    private void collectAgentStepTexts(Object value, List<String> stepTexts) {
        if (value instanceof com.alibaba.fastjson2.JSONObject) {
            com.alibaba.fastjson2.JSONObject object = (com.alibaba.fastjson2.JSONObject) value;
            String type = object.getString("type");
            if ("step.meta".equals(type) || "step".equals(type)) {
                String step = formatAgentStepText(object, stepTexts.size());
                if (StringUtils.isNotBlank(step)) {
                    stepTexts.add(step);
                }
                return;
            }
            for (String key : object.keySet()) {
                collectAgentStepTexts(object.get(key), stepTexts);
            }
        } else if (value instanceof com.alibaba.fastjson2.JSONArray) {
            com.alibaba.fastjson2.JSONArray array = (com.alibaba.fastjson2.JSONArray) value;
            for (Object item : array) {
                collectAgentStepTexts(item, stepTexts);
            }
        }
    }

    private String formatAgentStepText(com.alibaba.fastjson2.JSONObject object, int index) {
        String title = firstNonBlank(
                object.getString("title"),
                object.getString("action_intention"),
                object.getString("action"),
                "步骤" + (index + 1));
        List<String> lines = new ArrayList<>();
        lines.add("【" + title + "】");
        appendAgentStepLine(lines, "思考", object.getString("thought"));
        appendAgentStepLine(lines, "动作", object.getString("action"));
        appendAgentStepLine(lines, "原因", object.getString("action_reason"));
        appendAgentStepLine(lines, "输入", object.getString("action_input"));
        return String.join("\n", lines).trim();
    }

    private void appendAgentStepLine(List<String> lines, String label, String value) {
        if (StringUtils.isNotBlank(value)) {
            lines.add(label + "：" + value);
        }
    }

    private void appendSqlFromNativeSteps(String reply, SseEmitter emitter) throws IOException {
        String sql = firstNonBlank(extractSqlFromAgentSteps(reply), extractSqlFromReply(reply));
        if (StringUtils.isBlank(sql)) {
            sendSse(emitter, "sql", "");
            return;
        }
        if (StringUtils.isBlank(sql) || sql.startsWith("--")) {
            sendSse(emitter, "sql", "");
            return;
        }
        sendSse(emitter, "sql", sql);
    }

    private String extractSqlFromAgentSteps(String reply) {
        if (StringUtils.isBlank(reply)) {
            return null;
        }
        List<String> fragments = extractJsonFragments(reply);
        for (String fragment : fragments) {
            try {
                Object parsed = JSON.parse(fragment);
                String sql = extractSqlFromAgentStepJson(parsed);
                if (StringUtils.isNotBlank(sql)) {
                    return sql;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractSqlFromAgentStepJson(Object value) {
        if (value instanceof com.alibaba.fastjson2.JSONObject) {
            com.alibaba.fastjson2.JSONObject object = (com.alibaba.fastjson2.JSONObject) value;
            String sql = extractSqlFromJson(object);
            if (StringUtils.isNotBlank(sql)) {
                return sql;
            }
            String actionInput = object.getString("action_input");
            if (StringUtils.isNotBlank(actionInput)) {
                try {
                    sql = extractSqlFromJson(JSON.parse(actionInput));
                    if (StringUtils.isNotBlank(sql)) {
                        return sql;
                    }
                } catch (Exception ignored) {
                }
            }
            for (String key : object.keySet()) {
                sql = extractSqlFromAgentStepJson(object.get(key));
                if (StringUtils.isNotBlank(sql)) {
                    return sql;
                }
            }
        }
        if (value instanceof com.alibaba.fastjson2.JSONArray) {
            com.alibaba.fastjson2.JSONArray array = (com.alibaba.fastjson2.JSONArray) value;
            for (Object item : array) {
                String sql = extractSqlFromAgentStepJson(item);
                if (StringUtils.isNotBlank(sql)) {
                    return sql;
                }
            }
        }
        return null;
    }

    @Override
    public AiAskDataReportRespVO generateReport(AiAskDataReportReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getQuestion())) {
            throw new ServiceException("用户问题不能为空");
        }
        if (reqVO.getDatasourceId() == null) {
            throw new ServiceException("数据源ID不能为空");
        }
        checkDatasourceAccess(reqVO.getDatasourceId(), reqVO.getSpaceId(), reqVO.getSpaceCode());
        AiSkillReportTemplateDO template = resolveReportTemplate(reqVO);
        DbGptChatCompletionRequest gptRequest = buildDbGptReportRequest(reqVO, template);

        AiAskDataReportRespVO respVO = new AiAskDataReportRespVO();
        respVO.setQuestion(reqVO.getQuestion());
        if (template != null) {
            respVO.setSkillId(template.getSkillId());
            respVO.setTemplateId(template.getId());
            respVO.setTemplateCode(template.getTemplateCode());
            respVO.setTemplateName(template.getTemplateName());
            respVO.setTemplateContent(template.getTemplateContent());
        } else {
            respVO.setSkillId(reqVO.getSkillId());
        }

        try {
            // 报告也必须走原生 react-agent，这条链路才具备真实的数据源查询能力。
            // v2 chat/completions 只负责文本生成，无法保证执行 SQL 后返回报告数据。
            DbGptChatCompletionResponse gptResponse = dbGptClientService.chatCompletionV1(gptRequest);
            String reply = extractReply(gptResponse);
            respVO.setRawReply(normalizeAgentMessage(reply));
            if (template != null) {
                respVO.setReportData(extractJsonFromReply(reply));
            }
            if (respVO.getReportData() != null) {
                respVO.setSql(respVO.getReportData().getString("sql"));
            } else {
                respVO.setSql(extractSqlFromReply(reply));
            }
            if (StringUtils.isBlank(reply)) {
                respVO.setQualityWarning("数据智能体未返回合法报告数据");
            }
        } catch (Exception e) {
            log.warn("决策智能体报告生成调用失败：{}", e.getMessage());
            respVO.setQualityWarning(normalizeAgentMessage(e.getMessage()));
        }
        return respVO;
    }

    private AiSkillReportTemplateDO resolveReportTemplate(AiAskDataReportReqVO reqVO) {
        try {
            if (reqVO.getTemplateId() != null) {
                AiSkillReportTemplateDO template = aiSkillReportTemplateMapper.selectById(reqVO.getTemplateId());
                if (template != null
                        && (reqVO.getSkillId() == null || reqVO.getSkillId().equals(template.getSkillId()))) {
                    return template;
                }
                // 页面会话可能保存了已删除/替换的模板 ID；自动回退到当前 Skill 的默认或动态模板。
            }
            Long skillId = reqVO.getSkillId();
            if (skillId == null) {
                skillId = resolveReportSkillId(reqVO);
            }
            if (skillId == null) {
                return null;
            }
            AiSkillReportTemplateDO template = aiSkillReportTemplateMapper.selectDefaultBySkillId(skillId);
            if (template == null) {
                List<AiSkillReportTemplateDO> templates = aiSkillReportTemplateMapper.selectListBySkillId(skillId);
                if (templates != null && !templates.isEmpty()) {
                    template = templates.get(0);
                }
            }
            if (template == null) {
                // 没有模板时允许 DB-GPT 直接返回 HTML 或 Markdown 报告。
                return null;
            }
            return template;
        } catch (RuntimeException e) {
            if (isReportTemplateTableMissing(e)) {
                throw new ServiceException("报告模板表未初始化，请执行 sql/postgresql/upgrade/V1.6.0/add-ai-skill.sql 中 AI_SKILL_REPORT_TEMPLATE 建表语句");
            }
            throw e;
        }
    }

    private boolean isReportTemplateTableMissing(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains("ai_skill_report_template")
                    && message.toLowerCase(Locale.ROOT).contains("does not exist")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private DbGptChatCompletionRequest buildDbGptReportRequest(AiAskDataReportReqVO reqVO, AiSkillReportTemplateDO template) {
        DbGptChatCompletionRequest gptRequest = new DbGptChatCompletionRequest();
        gptRequest.setModel(firstNonBlank(reqVO.getModel(), dbGptProperties.getModel()));
        gptRequest.setTemperature(0.4);
        gptRequest.setMaxTokens(6000);
        gptRequest.setStream(false);
        gptRequest.setChatMode(dbGptProperties.getChatMode());
        gptRequest.setSpaceName(dbGptProperties.getSkillSpaceName());
        gptRequest.setConvUid("datamaster-report-" + System.currentTimeMillis());

        DatasourceDO datasource = datasourceMgmtService.getDatasourceDOById(reqVO.getDatasourceId());
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }
        if (datasource.getDbgptDatasourceId() != null) {
            gptRequest.setDatasourceId(datasource.getDbgptDatasourceId().intValue());
            String dbName = dbGptClientService.getDatasourceDbName(datasource.getDbgptDatasourceId().intValue());
            if (dbName != null) {
                gptRequest.setChatParam(dbName);
            }
        }
        gptRequest.setDbName(datasource.getDatasourceName());
        gptRequest.setDbType(datasource.getDatasourceType());
        String schemaName = resolveDatasourceSchema(datasource);
        if (StringUtils.isNotBlank(schemaName)) {
            gptRequest.getExtra().put("schema", schemaName);
        }
        gptRequest.setMessages(Collections.singletonList(
                new DbGptChatMessage("user", buildReportUserInput(reqVO, template))
        ));
        return gptRequest;
    }

    private String buildReportUserInput(AiAskDataReportReqVO reqVO, AiSkillReportTemplateDO template) {
        if (template == null) {
            return buildDirectReportUserInput(reqVO);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("你是 DataMaster 报告数据生成助手。请根据用户需求和当前用户信息，为报告模板准备结构化数据。\n");
        builder.append("必须只返回一个合法 JSON 对象，不要返回 Markdown，不要返回 HTML，不要使用代码块包裹。\n");
        builder.append("不要输出分析过程、执行步骤、代码或HTML报告，只返回前端模板渲染需要的数据对象。\n");
        builder.append("返回字段必须满足模板 dataSchema.required 和 dataSchema.fields；图表和表格数据必须返回数组。\n");
        builder.append("字段路径必须按点号组织成嵌套对象，例如 metrics.totalOrderCount 必须返回为 {\"metrics\":{\"totalOrderCount\":...}}。\n");
        appendMetricIntentRules(builder);
        if (reqVO.getParams() != null && !reqVO.getParams().isEmpty()) {
            builder.append("\n【报告参数】\n").append(JSON.toJSONString(reqVO.getParams())).append("\n");
        }
        builder.append("\n【必须返回的数据字段】\n").append(buildReportDataSchemaText(template.getTemplateContent())).append("\n");
        builder.append("\n【报告模板JSON】\n").append(template.getTemplateContent()).append("\n");
        Long userPermissionLevel = currentUserDataPermissionLevel();
        builder.append("\n【当前用户信息】\n");
        appendCurrentUserDataPermission(builder, userPermissionLevel);
        builder.append("\n");
        builder.append("\n【用户需求】\n").append(reqVO.getQuestion()).append("\n");
        return builder.toString();
    }

    private String buildDirectReportUserInput(AiAskDataReportReqVO reqVO) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是 DataMaster 数据报告智能体。请先使用当前数据源的 SQL 查询能力获取真实数据，再生成完整报告。\n");
        builder.append("报告可以返回完整 HTML 文档或 Markdown；不要为了生成报告调用 shell、code_interpreter 或文件系统工具。\n");
        builder.append("不要只给出制作建议，必须包含实际统计结果、关键指标、明细或分组数据以及分析结论。\n");
        builder.append("如果返回 HTML，必须是可直接渲染的完整 HTML 文档；如果返回 Markdown，必须包含清晰的标题、指标和表格。\n");
        appendMetricIntentRules(builder);
        if (reqVO.getSkillId() != null) {
            AiSkillDO skill = aiSkillMapper.selectById(reqVO.getSkillId());
            if (skill != null && StringUtils.isNotBlank(skill.getContent())) {
                builder.append("\n\n【业务 Skill 上下文】\n").append(skill.getContent()).append("\n");
            }
        }
        builder.append("\n【用户报告需求】\n").append(reqVO.getQuestion()).append("\n");
        builder.append("\n【当前用户信息】\n");
        appendCurrentUserDataPermission(builder, currentUserDataPermissionLevel());
        return builder.toString();
    }

    /** 根据当前数据源和问题自动选择报告 Skill，减少报告模式的人工前置配置。 */
    private Long resolveReportSkillId(AiAskDataReportReqVO reqVO) {
        List<AiSkillDO> skills = aiSkillMapper.selectPublishedSkills();
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        String question = firstNonBlank(reqVO.getQuestion()).toLowerCase(Locale.ROOT);
        AiSkillDO best = null;
        int bestScore = Integer.MIN_VALUE;
        for (AiSkillDO skill : skills) {
            if (skill == null || !"PUBLISHED".equals(skill.getStatus())) {
                continue;
            }
            int score = 0;
            if (reqVO.getDatasourceId() != null
                    && "DATA_SOURCE".equalsIgnoreCase(skill.getBizObjectType())
                    && reqVO.getDatasourceId().equals(skill.getBizObjectId())) {
                score += 1000;
            }
            String searchable = (firstNonBlank(skill.getSkillName()) + " "
                    + firstNonBlank(skill.getSkillCode()) + " "
                    + firstNonBlank(skill.getContent())).toLowerCase(Locale.ROOT);
            if (StringUtils.isNotBlank(skill.getSkillName()) && question.contains(skill.getSkillName().toLowerCase(Locale.ROOT))) {
                score += 300;
            }
            if (StringUtils.isNotBlank(skill.getSkillCode()) && question.contains(skill.getSkillCode().toLowerCase(Locale.ROOT))) {
                score += 200;
            }
            for (String token : question.replaceAll("[^\\p{L}\\p{N}]", " ").split("\\s+")) {
                if (token.length() >= 2 && searchable.contains(token)) {
                    score += 20;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = skill;
            }
        }
        return best == null || bestScore <= 0 ? null : best.getId();
    }

    private String buildReportDataSchemaText(String templateContent) {
        if (StringUtils.isBlank(templateContent)) {
            return "请参考报告模板 dataSchema 返回完整 JSON 数据。";
        }
        try {
            com.alibaba.fastjson2.JSONObject template = JSON.parseObject(templateContent);
            com.alibaba.fastjson2.JSONObject dataSchema = template.getJSONObject("dataSchema");
            if (dataSchema == null) {
                return "请参考报告模板 dataSchema 返回完整 JSON 数据。";
            }
            StringBuilder builder = new StringBuilder();
            if (dataSchema.getJSONArray("required") != null) {
                builder.append("- required: ").append(dataSchema.getJSONArray("required").toJSONString()).append("\n");
            }
            if (dataSchema.getJSONArray("fields") != null) {
                builder.append("- fields: ").append(dataSchema.getJSONArray("fields").toJSONString()).append("\n");
            }
            return builder.length() > 0 ? builder.toString() : dataSchema.toJSONString();
        } catch (Exception e) {
            return "请参考报告模板 dataSchema 返回完整 JSON 数据。";
        }
    }

    private DbGptChatCompletionRequest buildDbGptAskRequest(AiAskDataSqlReqVO reqVO, boolean stream, Long userPermissionLevel) {
        DbGptChatCompletionRequest gptRequest = new DbGptChatCompletionRequest();
        gptRequest.setModel(firstNonBlank(reqVO.getModel(), dbGptProperties.getModel()));
        gptRequest.setTemperature(0.6);
        gptRequest.setMaxTokens(4000);
        gptRequest.setStream(stream);
        gptRequest.setChatMode(firstNonBlank(reqVO.getChatMode(), dbGptProperties.getChatMode()));
        gptRequest.setSpaceName(dbGptProperties.getSkillSpaceName());
        gptRequest.setConvUid("datamaster-ai-" + System.currentTimeMillis());
        if (reqVO.getDatasourceId() != null) {
            DatasourceDO datasource = datasourceMgmtService.getDatasourceDOById(reqVO.getDatasourceId());
            if (datasource != null) {
                if (datasource.getDbgptDatasourceId() != null) {
                    gptRequest.setDatasourceId(datasource.getDbgptDatasourceId().intValue());
                    String dbName = dbGptClientService.getDatasourceDbName(datasource.getDbgptDatasourceId().intValue());
                    if (dbName != null) {
                        gptRequest.setChatParam(dbName);
                    }
                }
                gptRequest.setDbName(datasource.getDatasourceName());
                gptRequest.setDbType(datasource.getDatasourceType());
                String schemaName = resolveDatasourceSchema(datasource);
                if (StringUtils.isNotBlank(schemaName)) {
                    gptRequest.getExtra().put("schema", schemaName);
                }
            }
        }
        bindSelectedDbGptSkill(reqVO, gptRequest);
        boolean isGeneralChat = "chat_normal".equals(gptRequest.getChatMode());
        String userInput = buildDbGptUserInput(reqVO, userPermissionLevel);
        if (isGeneralChat) {
            gptRequest.setMessages(Collections.singletonList(
                    new DbGptChatMessage("user", userInput)
            ));
        } else {
            gptRequest.setMessages(Collections.singletonList(
                    new DbGptChatMessage("user", userInput)
            ));
        }
        return gptRequest;
    }

    private void bindSelectedDbGptSkill(AiAskDataSqlReqVO reqVO, DbGptChatCompletionRequest gptRequest) {
        AiSkillDO skill = resolveSelectedDbGptSkill(reqVO);
        if (skill == null || StringUtils.isBlank(skill.getSkillCode())) {
            return;
        }
        // 本体决策 Skill 是 DataMaster 的内部动作/权限上下文，不是 DB-GPT 的问数知识库。
        // 将它作为 skill_id 传给 chat_react_agent 会导致部分适配器把 Skill 文档原样回显，
        // 问数场景应继续使用数据库问答模式，由数据源直接生成查询结果。
        if ("ONTOLOGY_DECISION".equalsIgnoreCase(skill.getSkillType())
                || "ONTOLOGY".equalsIgnoreCase(skill.getBizObjectType())) {
            gptRequest.getExtra().put("ontology_id", skill.getBizObjectId());
            return;
        }
        String skillId = firstNonBlank(skill.getDbgptDocumentName(), skill.getSkillCode());
        if (skillId.endsWith(".md")) {
            skillId = skill.getSkillCode();
        }
        gptRequest.setChatMode("chat_react_agent");
        gptRequest.setSpaceName(null);
        gptRequest.getExtra().put("skill_id", skillId);
        gptRequest.getExtra().put("skill_name", skillId);
        log.info("AI问数绑定DB-GPT Skill：skillId={}, localSkillId={}, question={}",
                skillId, skill.getId(), reqVO == null ? "" : reqVO.getQuestion());
    }

    private String resolveDatasourceSchema(DatasourceDO datasource) {
        if (datasource == null) return null;
        try {
            com.alibaba.fastjson2.JSONObject config = StringUtils.isBlank(datasource.getDatasourceConfig())
                    ? null : JSON.parseObject(datasource.getDatasourceConfig());
            String schema = config == null ? null : firstNonBlank(config.getString("sid"), config.getString("schema"));
            if (StringUtils.isNotBlank(schema)) return schema;
        } catch (Exception e) {
            log.debug("读取数据源 schema 配置失败: {}", e.getMessage());
        }
        String type = firstNonBlank(datasource.getDatasourceType()).toLowerCase(Locale.ROOT);
        return type.contains("postgres") || type.contains("kingbase") ? "public" : null;
    }

    private AiSkillDO resolveSelectedDbGptSkill(AiAskDataSqlReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        if (reqVO.getSkillIds() != null && !reqVO.getSkillIds().isEmpty()) {
            List<AiSkillDO> skills = aiSkillMapper.selectBatchIds(reqVO.getSkillIds());
            if (skills != null) {
                for (AiSkillDO skill : skills) {
                    if (isUsableDbGptSkill(skill)) {
                        return skill;
                    }
                }
            }
        }
        if (reqVO.getAssetId() != null) {
            AiSkillDO skill = aiSkillMapper.selectByBizObject("TABLE", reqVO.getAssetId());
            if (isUsableDbGptSkill(skill)) {
                return skill;
            }
        }
        return null;
    }

    private boolean isUsableDbGptSkill(AiSkillDO skill) {
        return skill != null
                && "PUBLISHED".equals(skill.getStatus())
                && StringUtils.isNotBlank(skill.getSkillCode());
    }

    private void checkAskDataGovernance(AiAskDataSqlReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (reqVO.getAssetId() == null) {
            checkDatasourceAccess(reqVO.getDatasourceId(), reqVO.getSpaceId(), reqVO.getSpaceCode());
            return;
        }
        AssetsAssetDO asset = assetsAssetMapper.selectById(reqVO.getAssetId());
        if (asset == null || asset.getDatasourceId() == null || StringUtils.isBlank(asset.getTableName())) {
            return;
        }
        if (reqVO.getDatasourceId() != null && !reqVO.getDatasourceId().equals(asset.getDatasourceId())) {
            throw new ServiceException("当前问数数据源与资产所属数据源不一致");
        }
        AssetsTableGovernanceReqDTO governanceReq = new AssetsTableGovernanceReqDTO();
        governanceReq.setDatasourceId(asset.getDatasourceId());
        governanceReq.setTableName(asset.getTableName());
        governanceReq.setSpaceId(reqVO.getSpaceId());
        governanceReq.setSpaceCode(reqVO.getSpaceCode());
        governanceReq.setEntrance("AI_ASK_DATA");
        assetsTableGovernanceApiService.checkTableAccess(governanceReq);
    }

    private void checkAskDataAssetAccess(Long assetId, Long spaceId, String spaceCode, String entrance) {
        if (assetId == null) {
            return;
        }
        AssetsAssetDO asset = assetsAssetMapper.selectById(assetId);
        if (asset == null || asset.getDatasourceId() == null || StringUtils.isBlank(asset.getTableName())) {
            return;
        }
        AssetsTableGovernanceReqDTO governanceReq = new AssetsTableGovernanceReqDTO();
        governanceReq.setDatasourceId(asset.getDatasourceId());
        governanceReq.setTableName(asset.getTableName());
        governanceReq.setSpaceId(spaceId);
        governanceReq.setSpaceCode(spaceCode);
        governanceReq.setEntrance(entrance);
        assetsTableGovernanceApiService.checkTableAccess(governanceReq);
    }

    private void checkDatasourceAccess(Long datasourceId, Long spaceId, String spaceCode) {
        if (datasourceId == null || (spaceId == null && StringUtils.isBlank(spaceCode))) {
            return;
        }
        Long count = datasourceSpaceRelMgmtMapper.selectCount(Wrappers.<DatasourceSpaceRelDO>lambdaQuery()
                .eq(DatasourceSpaceRelDO::getDatasourceId, datasourceId)
                .eq(spaceId != null, DatasourceSpaceRelDO::getSpaceId, spaceId)
                .eq(StringUtils.isNotBlank(spaceCode), DatasourceSpaceRelDO::getSpaceCode, spaceCode));
        if (count == null || count <= 0) {
            throw new ServiceException("当前空间无权访问该数据源");
        }
    }

    private String buildDbGptUserInput(AiAskDataSqlReqVO reqVO, Long userPermissionLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是平台数据智能体，Skill 只属于内部决策上下文，绝不能把 Skill 名称、Markdown 文档、角色边界或输出协议原样展示给用户。\n");
        builder.append("请真正根据用户问题查询数据并返回最终答案；查询类问题必须给出查询结果，必要时附带 SQL。不要返回 Skill 文档本身。\n\n");
        builder.append("本次是数据查询任务：禁止调用 shell、code_interpreter 或文件系统工具，也不要输出容器目录、工作目录或工具原始日志；只使用当前数据源的 SQL 查询能力。\n\n");
        builder.append("用户问题：").append(reqVO.getQuestion()).append("\n\n");
        if (reqVO.getDatasourceId() != null) {
            DatasourceDO datasource = datasourceMgmtService.getDatasourceDOById(reqVO.getDatasourceId());
            String schema = resolveDatasourceSchema(datasource);
            if (datasource != null && StringUtils.isNotBlank(schema)) {
                builder.append("当前真实数据库 schema：").append(schema)
                        .append("。数据库名称不能当作 schema；PostgreSQL/Kingbase 查询 information_schema 时必须使用该 schema。\n\n");
            }
        }
        appendCurrentUserDataPermission(builder, userPermissionLevel);
        return builder.toString();
    }

    private void appendCurrentUserDataPermission(StringBuilder builder, Long userPermissionLevel) {
        builder.append("当前用户数据等级 data_permission_level：")
                .append(userPermissionLevel == null ? "未配置，按运行时后端权限结果为准" : userPermissionLevel)
                .append("请严格按照数据字段权限查询");
    }

    private void appendMetricIntentRules(StringBuilder builder) {
        builder.append("\n\n【指标口径识别规则】\n");
        builder.append("- 用户提到\u201c销售量、销量、销售次数、订单量、租赁量、租赁次数、购买次数、交易次数、贡献 top10%\u201d时，默认按数量/次数口径计算，");
        builder.append("例如 COUNT(*)、COUNT(order_id/rental_id) 或 SUM(quantity)，不要用 SUM(amount/payment/price) 代替。\n");
        builder.append("- 只有用户明确提到\u201c销售额、消费金额、收入、GMV、金额、客单价、高价值用户\u201d时，才按金额口径计算。\n");
        builder.append("- \u201c贡献 top10%\u201d应先按用户问题中的指标聚合并降序排序，再取 top 10%；不得更换用户指定的指标口径。\n");
    }

    private List<AiSkillDO> getRelevantSkills(String question, Long assetId) {
        String keyword = firstNonBlank(question);
        AiSkillDO assetSkill = assetId != null ? aiSkillMapper.selectByBizObject("TABLE", assetId) : null;
        return mergeSkills(
                Collections.emptyList(),
                aiSkillMapper.selectPublishedByKeyword(keyword),
                assetSkill
        );
    }

    private List<AiSkillDO> mergeSkills(List<AiSkillDO> platformSkills, List<AiSkillDO> matchedSkills, AiSkillDO assetSkill) {
        Map<Long, AiSkillDO> map = new LinkedHashMap<>();
        if (platformSkills != null) {
            for (AiSkillDO skill : platformSkills) {
                map.put(skill.getId(), skill);
            }
        }
        if (assetSkill != null && "PUBLISHED".equals(assetSkill.getStatus())) {
            map.put(assetSkill.getId(), assetSkill);
        }
        if (matchedSkills != null) {
            for (AiSkillDO skill : matchedSkills) {
                map.put(skill.getId(), skill);
            }
        }
        return new ArrayList<>(map.values());
    }

    private Long currentUserDataPermissionLevel() {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null) {
                log.warn("AI问数：当前登录用户为空，无法获取数据权限等级");
                return null;
            }
            SysUser user = loginUser.getUser();
            if (user == null) {
                log.warn("AI问数：当前用户信息为空，无法获取数据权限等级");
                return null;
            }
            Long level = user.getDataPermissionLevel();
            log.info("AI问数：当前用户={}, dataPermissionLevel={}", user.getUserName(), level);
            return level;
        } catch (Exception e) {
            log.warn("AI问数：获取用户数据权限等级异常: {}", e.getMessage());
            return null;
        }
    }

    private void appendSelectedSkillContext(StringBuilder builder, AiAskDataSqlReqVO reqVO) {
        if (reqVO == null || reqVO.getSkillIds() == null || reqVO.getSkillIds().isEmpty()) {
            return;
        }
        List<AiSkillDO> skills = aiSkillMapper.selectBatchIds(reqVO.getSkillIds());
        if (skills == null || skills.isEmpty()) {
            return;
        }
        builder.append("\n\n【已选择知识库】\n");
        for (AiSkillDO skill : skills) {
            if (skill == null || !"PUBLISHED".equals(skill.getStatus())) {
                continue;
            }
            builder.append("### ").append(firstNonBlank(skill.getSkillName(), skill.getSkillCode())).append("\n");
            if (StringUtils.isNotBlank(skill.getContent())) {
                builder.append(skill.getContent()).append("\n");
            }
        }
    }

    private void sendSse(SseEmitter emitter, String event, String data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data == null ? "" : data));
    }

    private boolean looksLikeDataQuestion(String question) {
        if (StringUtils.isBlank(question)) {
            return false;
        }
        return containsAny(question,
                "查询", "查看", "统计", "分析", "汇总", "明细", "前", "条", "多少", "数量", "趋势", "计算", "平均",
                "订单", "客户", "用户", "销售", "金额", "质量", "异常", "价格", "最大值", "最小值",
                "sql", "select", "order", "count", "sum", "avg");
    }

    private boolean containsAny(String value, String... keywords) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String extractReply(DbGptChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "";
        }
        DbGptChatCompletionResponse.Choice choice = response.getChoices().get(0);
        return choice.getMessage() != null ? choice.getMessage().getContent() : "";
    }

    private String normalizeAskReply(String reply) {
        if (StringUtils.isBlank(reply)) {
            return reply;
        }
        String cleaned = removeSkillEchoPrefix(reply);
        cleaned = removeStepMetaBlocks(cleaned);
        cleaned = removeNativeAgentTraceText(cleaned);
        cleaned = removeDbGptFormatWarnings(cleaned);
        cleaned = removeHtmlInterpreterSummary(cleaned);
        if (containsFullHtmlDocument(cleaned)) {
            String text = htmlDocumentToPlainText(cleaned);
            if (StringUtils.isNotBlank(text)) {
                cleaned = text;
            }
        }
        return normalizeAgentMessage(cleaned.trim());
    }

    /** 适配器名称只允许留在内部实现，统一隐藏在平台对外消息中。 */
    private String normalizeAgentMessage(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        return text.replaceAll("(?i)DB[-_ ]?GPT", "决策智能体")
                .replace("AI问数", "决策智能体");
    }

    private String removeSkillEchoPrefix(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        // DB-GPT 某些版本会把加载的 Skill 文档追加到最终回答末尾；文档属于内部上下文，不能透传给用户。
        java.util.regex.Matcher skillMatcher = java.util.regex.Pattern
                .compile("(?im)^[ \\t]*Skill\\s*:\\s*[^\\r\\n]*(?:[\\r\\n]+[\\s\\S]*)?$")
                .matcher(text);
        if (skillMatcher.find()) {
            String answer = text.substring(0, skillMatcher.start()).trim();
            if (StringUtils.isNotBlank(answer)) {
                return answer;
            }
            return "";
        }
        int start = -1;
        int stepStart = text.indexOf("{\"type\":\"step.meta\"");
        if (stepStart < 0) {
            stepStart = text.indexOf("{\"type\": \"step.meta\"");
        }
        if (stepStart >= 0) {
            int stepEnd = findJsonObjectEnd(text, stepStart);
            start = stepEnd >= 0 ? stepEnd + 1 : stepStart;
        } else {
            int tableStart = text.indexOf("| ");
            if (tableStart >= 0) {
                start = tableStart;
            }
        }
        int sqlStart = indexOfIgnoreCase(text, "```sql");
        if (sqlStart >= 0 && start < 0) {
            start = sqlStart;
        }
        if (start > 0 && text.substring(0, start).contains("Skill:")) {
            return text.substring(start);
        }
        return text;
    }

    private int findJsonObjectEnd(String text, int start) {
        if (StringUtils.isBlank(text) || start < 0 || start >= text.length() || text.charAt(start) != '{') {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = inString;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String removeDbGptFormatWarnings(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        return text.replaceAll("(?is)No correct response found\\. Please check your response, which must be in the format indicated in the system prompt\\.\\s*", "");
    }

    /**
     * DB-GPT 的 react-agent 适配器有些版本会把工具轨迹直接拼进 assistant content。
     * 这些内容（例如 sql_query 的思考/动作以及 code-interpreter 工作目录列表）
     * 只属于内部执行日志，不能作为用户答案透出。
     */
    private String removeNativeAgentTraceText(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        String cleaned = text
                .replaceAll("(?im)^\\s*\\[[^\\]\\r\\n]+\\]\\s*(?=(思考|动作|原因|输入)\\s*[:：])", "")
                .replaceAll("(?im)^\\s*(思考|动作|原因|输入)\\s*[:：].*$", "")
                .replaceAll("(?im)^\\s*[bcdlps-][rwx-]{9}\\.?\\s+\\d+\\s+\\S+\\s+\\S+\\s+\\d+\\s+[^\\r\\n]+$", "")
                .replaceAll("(?im)^\\s*[^\\r\\n]*\\bop_snapshots\\b[^\\r\\n]*$", "");
        return cleaned.replaceAll("(?m)^[ \\t]*\\r?\\n", "\\n").trim();
    }

    private String removeStepMetaBlocks(String text) {
        String cleaned = text;
        int guard = 0;
        while (guard++ < 20) {
            int start = cleaned.indexOf("{\"type\":\"step.meta\"");
            if (start < 0) {
                start = cleaned.indexOf("{\"type\": \"step.meta\"");
            }
            if (start < 0) {
                break;
            }
            int htmlStart = indexOfIgnoreCase(cleaned, "<!DOCTYPE", start);
            if (htmlStart < 0) {
                htmlStart = indexOfIgnoreCase(cleaned, "<html", start);
            }
            int end = htmlStart >= 0 ? htmlStart : cleaned.indexOf('\n', start);
            if (end < 0) {
                return cleaned.substring(0, start).trim();
            }
            cleaned = cleaned.substring(0, start) + cleaned.substring(end);
        }
        return cleaned;
    }

    private boolean containsFullHtmlDocument(String text) {
        return indexOfIgnoreCase(text, "<!DOCTYPE") >= 0 || indexOfIgnoreCase(text, "<html") >= 0;
    }

    private String htmlDocumentToPlainText(String html) {
        int bodyStart = indexOfIgnoreCase(html, "<body");
        String content = html;
        if (bodyStart >= 0) {
            int bodyOpenEnd = html.indexOf('>', bodyStart);
            int bodyEnd = indexOfIgnoreCase(html, "</body>", bodyOpenEnd);
            if (bodyOpenEnd >= 0) {
                content = bodyEnd >= 0 ? html.substring(bodyOpenEnd + 1, bodyEnd) : html.substring(bodyOpenEnd + 1);
            }
        }
        content = content.replaceAll("(?is)<script[\\s\\S]*?</script>", "");
        content = content.replaceAll("(?is)<style[\\s\\S]*?</style>", "");
        content = content.replaceAll("(?i)<br\\s*/?>", "\n");
        content = content.replaceAll("(?i)</(h[1-6]|p|div|section|article|tr|li|table)>", "\n");
        content = content.replaceAll("(?is)<[^>]+>", "");
        content = content.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"");
        content = removeHtmlInterpreterSummary(content);
        return content.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n")
                .trim();
    }

    private String removeHtmlInterpreterSummary(String text) {
        return text.replaceAll("(?is)✅\\s*[^\\n<]*?报告已生成并渲染完成。.*?所有内容已通过交互式\\s*HTML\\s*页面直观展示。?\\s*$", "")
                .replaceAll("(?is)报告包含：\\s*[•\\-\\s\\S]*?所有内容已通过交互式\\s*HTML\\s*页面直观展示。?\\s*$", "");
    }

    private int indexOfIgnoreCase(String text, String needle) {
        return indexOfIgnoreCase(text, needle, 0);
    }

    private int indexOfIgnoreCase(String text, String needle, int fromIndex) {
        if (text == null || needle == null) {
            return -1;
        }
        return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT), Math.max(0, fromIndex));
    }

    private String extractSqlFromReply(String reply) {
        if (StringUtils.isBlank(reply)) {
            return null;
        }
        try {
            String sql = extractSqlFromJson(JSON.parse(reply));
            if (StringUtils.isNotBlank(sql)) {
                return sql;
            }
        } catch (Exception ignored) {
        }
        String sqlFromJsonFragment = extractSqlFromJsonFragments(reply);
        if (StringUtils.isNotBlank(sqlFromJsonFragment)) {
            return sqlFromJsonFragment;
        }

        Matcher codeMatcher = SQL_BLOCK_PATTERN.matcher(reply);
        if (codeMatcher.find()) {
            String sql = codeMatcher.group(1).trim();
            if (sql.toLowerCase().startsWith("select")) {
                return sql;
            }
        }

        String[] lines = reply.split("\n");
        StringBuilder sqlBuilder = new StringBuilder();
        boolean inSql = false;
        for (String line : lines) {
            String trimmed = line.trim().toLowerCase();
            if (trimmed.startsWith("select")) {
                inSql = true;
            }
            if (inSql) {
                sqlBuilder.append(line).append(" ");
                if (trimmed.endsWith(";")) {
                    break;
                }
            }
        }

        if (sqlBuilder.length() > 0) {
            String sql = sqlBuilder.toString().trim();
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1);
            }
            return sql;
        }

        return null;
    }

    private String extractSqlFromJsonFragments(String reply) {
        List<String> fragments = extractJsonFragments(reply);
        for (int i = fragments.size() - 1; i >= 0; i--) {
            try {
                String sql = extractSqlFromJson(JSON.parse(fragments.get(i)));
                if (StringUtils.isNotBlank(sql)) {
                    return sql;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private List<String> extractJsonFragments(String content) {
        List<String> fragments = new ArrayList<>();
        if (StringUtils.isBlank(content)) {
            return fragments;
        }
        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = inString;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (ch == '}' && depth > 0) {
                depth--;
                if (depth == 0 && start >= 0) {
                    fragments.add(content.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return fragments;
    }

    private String extractSqlFromJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof com.alibaba.fastjson2.JSONObject) {
            com.alibaba.fastjson2.JSONObject object = (com.alibaba.fastjson2.JSONObject) value;
            String sql = firstNonBlank(
                    object.getString("sql"),
                    object.getString("SQL"),
                    object.getString("text2sql"),
                    object.getString("sqlText"),
                    object.getString("query")
            );
            if (StringUtils.isNotBlank(sql)) {
                return sql.trim();
            }
            for (String key : object.keySet()) {
                sql = extractSqlFromJson(object.get(key));
                if (StringUtils.isNotBlank(sql)) {
                    return sql;
                }
            }
        }
        if (value instanceof com.alibaba.fastjson2.JSONArray) {
            com.alibaba.fastjson2.JSONArray array = (com.alibaba.fastjson2.JSONArray) value;
            for (Object item : array) {
                String sql = extractSqlFromJson(item);
                if (StringUtils.isNotBlank(sql)) {
                    return sql;
                }
            }
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.toLowerCase(Locale.ROOT).startsWith("select")) {
                return text;
            }
            try {
                return extractSqlFromJson(JSON.parse(text));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private com.alibaba.fastjson2.JSONObject extractJsonFromReply(String reply) {
        if (StringUtils.isBlank(reply)) {
            return null;
        }
        String content = reply.trim();
        try {
            return JSON.parseObject(content);
        } catch (Exception ignored) {
        }

        Matcher jsonBlock = Pattern.compile("(?i)```json\\s*\\n?([\\s\\S]*?)\\s*```").matcher(content);
        if (jsonBlock.find()) {
            try {
                return JSON.parseObject(jsonBlock.group(1).trim());
            } catch (Exception ignored) {
            }
        }

        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return JSON.parseObject(content.substring(start, end + 1));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

}

