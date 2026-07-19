package com.datamaster.module.assets.service.skill.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataPrepareReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataPrepareRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataReportReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataReportRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataSqlReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataSqlRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceProjectRelDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillReportTemplateDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceProjectRelMapper;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillMapper;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillReportTemplateMapper;
import com.datamaster.module.assets.model.dto.dbgpt.DbGptChatCompletionRequest;
import com.datamaster.module.assets.model.dto.dbgpt.DbGptChatCompletionResponse;
import com.datamaster.module.assets.model.dto.dbgpt.DbGptChatMessage;
import com.datamaster.module.assets.config.DbGptProperties;
import com.datamaster.module.assets.service.dbgpt.IDbGptClientService;
import com.datamaster.module.assets.service.skill.IAiAskDataContextService;
import com.datamaster.module.assets.service.skill.IAiAskDataService;
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
    private AssetsDatasourceMapper assetsDatasourceMapper;
    @Resource
    private AssetsDatasourceProjectRelMapper assetsDatasourceProjectRelMapper;
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
    public AiAskDataPrepareRespVO prepare(AiAskDataPrepareReqVO reqVO) {
        String question = reqVO == null ? "" : reqVO.getQuestion();
        String keyword = reqVO == null ? "" : firstNonBlank(reqVO.getKeyword(), reqVO.getQuestion());
        AiSkillDO assetSkill = reqVO == null || reqVO.getAssetId() == null ? null : aiSkillMapper.selectByBizObject("TABLE", reqVO.getAssetId());
        checkAskDataAssetAccess(reqVO == null ? null : reqVO.getAssetId(),
                reqVO == null ? null : reqVO.getProjectId(),
                reqVO == null ? null : reqVO.getProjectCode(),
                "AI_ASK_DATA_PREPARE");
        List<AiSkillDO> skillList = mergeSkills(Collections.emptyList(), aiSkillMapper.selectPublishedByKeyword(keyword), assetSkill);
        List<AiSkillRespVO> skills = BeanUtils.toBean(skillList, AiSkillRespVO.class);
        AiAskDataPrepareRespVO respVO = new AiAskDataPrepareRespVO();
        respVO.setQuestion(question);
        respVO.setSkills(skills);
        respVO.setPromptContext(buildPromptContext(question, skills));
        return respVO;
    }

    @Override
    public AiAskDataSqlRespVO generateSql(AiAskDataSqlReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getQuestion())) {
            throw new ServiceException("用户问题不能为空");
        }
        checkAskDataGovernance(reqVO);

        String question = reqVO.getQuestion();
        Long assetId = reqVO.getAssetId();

        // Build full context from skills
        String fullContext = aiAskDataContextService.buildFullContext(question, assetId);

        // Get relevant skills
        List<AiSkillDO> skills = getRelevantSkills(question, assetId);
        List<AiSkillRespVO> skillRespList = BeanUtils.toBean(skills, AiSkillRespVO.class);

        // Build system prompt for SQL generation
        Long userPermissionLevel = currentUserDataPermissionLevel();
        String systemPrompt = buildSystemPrompt(fullContext, userPermissionLevel);

        // Call DB-GPT for SQL generation
        DbGptChatCompletionRequest gptRequest = new DbGptChatCompletionRequest();
        gptRequest.setModel(dbGptProperties.getModel());
        gptRequest.setTemperature(0.6);
        gptRequest.setMaxTokens(4096);
        gptRequest.setStream(false);
        gptRequest.setMessages(Arrays.asList(
                new DbGptChatMessage("system", systemPrompt),
                new DbGptChatMessage("user", question)
        ));

        AiAskDataSqlRespVO respVO = new AiAskDataSqlRespVO();
        respVO.setQuestion(question);
        respVO.setReferencedSkills(skillRespList);
        respVO.setExplanation(fullContext);

        try {
            DbGptChatCompletionResponse gptResponse = dbGptClientService.chatCompletion(gptRequest);

            String llmReply = extractReply(gptResponse);
            String sql = extractSqlFromReply(llmReply);

            respVO.setSql(sql);
            respVO.setQualityWarning(null);

            if (StringUtils.isBlank(sql)) {
                respVO.setSql("-- " + llmReply);
                respVO.setQualityWarning("未能从回复中提取有效SQL，原始回复：" + llmReply.substring(0, Math.min(200, llmReply.length())));
            }
        } catch (Exception e) {
            respVO.setQualityWarning("DB-GPT调用失败：" + e.getMessage());
            respVO.setSql("-- " + fullContext);
        }

        return respVO;
    }

    @Override
    public AiAskDataSqlRespVO chat(AiAskDataSqlReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getQuestion())) {
            throw new ServiceException("用户问题不能为空");
        }

        return generateSql(reqVO);
    }

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
            AssetsDatasourceDO datasource = assetsDatasourceMapper.selectById(reqVO.getDatasourceId());
            if (datasource != null) {
                if (datasource.getDbgptDatasourceId() != null) {
                    gptRequest.setDatasourceId(datasource.getDbgptDatasourceId());
                    // chat_with_db_qa 模式需要以 db_name 作为 chat_param
                    String dbName = dbGptClientService.getDatasourceDbName(datasource.getDbgptDatasourceId());
                    if (dbName != null) {
                        gptRequest.setChatParam(dbName);
                    }
                }
                gptRequest.setDbName(datasource.getDatasourceName());
                gptRequest.setDbType(datasource.getDatasourceType());
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
            respVO.setQualityWarning(e.getMessage());
        }
        respVO.setExplanation(reply);
        if (StringUtils.isBlank(reply) && StringUtils.isNotBlank(respVO.getQualityWarning())) {
            respVO.setExplanation("AI问数暂未返回结果：" + respVO.getQualityWarning());
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
                    sendSse(emitter, "error", "AI问数调用异常: " + e.getMessage());
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
        checkDatasourceAccess(reqVO.getDatasourceId(), reqVO.getProjectId(), reqVO.getProjectCode());
        AiSkillReportTemplateDO template = resolveReportTemplate(reqVO);
        DbGptChatCompletionRequest gptRequest = buildDbGptReportRequest(reqVO, template);

        AiAskDataReportRespVO respVO = new AiAskDataReportRespVO();
        respVO.setQuestion(reqVO.getQuestion());
        respVO.setSkillId(template.getSkillId());
        respVO.setTemplateId(template.getId());
        respVO.setTemplateCode(template.getTemplateCode());
        respVO.setTemplateName(template.getTemplateName());
        respVO.setTemplateContent(template.getTemplateContent());

        try {
            DbGptChatCompletionResponse gptResponse = dbGptClientService.chatCompletion(gptRequest);
            String reply = extractReply(gptResponse);
            respVO.setRawReply(reply);
            respVO.setReportData(extractJsonFromReply(reply));
            if (respVO.getReportData() != null) {
                respVO.setSql(respVO.getReportData().getString("sql"));
            }
            if (respVO.getReportData() == null) {
                respVO.setQualityWarning("DBGPT未返回合法JSON报告数据");
            }
        } catch (Exception e) {
            log.warn("DB-GPT报告生成调用失败：{}", e.getMessage());
            respVO.setQualityWarning(e.getMessage());
        }
        return respVO;
    }

    private AiSkillReportTemplateDO resolveReportTemplate(AiAskDataReportReqVO reqVO) {
        try {
            if (reqVO.getTemplateId() != null) {
                AiSkillReportTemplateDO template = aiSkillReportTemplateMapper.selectById(reqVO.getTemplateId());
                if (template == null) {
                    throw new ServiceException("报告模板不存在");
                }
                if (reqVO.getSkillId() != null && !reqVO.getSkillId().equals(template.getSkillId())) {
                    throw new ServiceException("报告模板不属于当前Skill");
                }
                return template;
            }
            if (reqVO.getSkillId() == null) {
                throw new ServiceException("请选择Skill或报告模板");
            }
            AiSkillReportTemplateDO template = aiSkillReportTemplateMapper.selectDefaultBySkillId(reqVO.getSkillId());
            if (template == null) {
                List<AiSkillReportTemplateDO> templates = aiSkillReportTemplateMapper.selectListBySkillId(reqVO.getSkillId());
                if (templates != null && !templates.isEmpty()) {
                    template = templates.get(0);
                }
            }
            if (template == null) {
                throw new ServiceException("当前Skill未配置报告模板");
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

        AssetsDatasourceDO datasource = assetsDatasourceMapper.selectById(reqVO.getDatasourceId());
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }
        if (datasource.getDbgptDatasourceId() != null) {
            gptRequest.setDatasourceId(datasource.getDbgptDatasourceId());
            String dbName = dbGptClientService.getDatasourceDbName(datasource.getDbgptDatasourceId());
            if (dbName != null) {
                gptRequest.setChatParam(dbName);
            }
        }
        gptRequest.setDbName(datasource.getDatasourceName());
        gptRequest.setDbType(datasource.getDatasourceType());
        gptRequest.setMessages(Collections.singletonList(
                new DbGptChatMessage("user", buildReportUserInput(reqVO, template))
        ));
        return gptRequest;
    }

    private String buildReportUserInput(AiAskDataReportReqVO reqVO, AiSkillReportTemplateDO template) {
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
            AssetsDatasourceDO datasource = assetsDatasourceMapper.selectById(reqVO.getDatasourceId());
            if (datasource != null) {
                if (datasource.getDbgptDatasourceId() != null) {
                    gptRequest.setDatasourceId(datasource.getDbgptDatasourceId());
                    String dbName = dbGptClientService.getDatasourceDbName(datasource.getDbgptDatasourceId());
                    if (dbName != null) {
                        gptRequest.setChatParam(dbName);
                    }
                }
                gptRequest.setDbName(datasource.getDatasourceName());
                gptRequest.setDbType(datasource.getDatasourceType());
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
            checkDatasourceAccess(reqVO.getDatasourceId(), reqVO.getProjectId(), reqVO.getProjectCode());
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
        governanceReq.setProjectId(reqVO.getProjectId());
        governanceReq.setProjectCode(reqVO.getProjectCode());
        governanceReq.setEntrance("AI_ASK_DATA");
        assetsTableGovernanceApiService.checkTableAccess(governanceReq);
    }

    private void checkAskDataAssetAccess(Long assetId, Long projectId, String projectCode, String entrance) {
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
        governanceReq.setProjectId(projectId);
        governanceReq.setProjectCode(projectCode);
        governanceReq.setEntrance(entrance);
        assetsTableGovernanceApiService.checkTableAccess(governanceReq);
    }

    private void checkDatasourceAccess(Long datasourceId, Long projectId, String projectCode) {
        if (datasourceId == null || (projectId == null && StringUtils.isBlank(projectCode))) {
            return;
        }
        Long count = assetsDatasourceProjectRelMapper.selectCount(Wrappers.<AssetsDatasourceProjectRelDO>lambdaQuery()
                .eq(AssetsDatasourceProjectRelDO::getDatasourceId, datasourceId)
                .eq(projectId != null, AssetsDatasourceProjectRelDO::getProjectId, projectId)
                .eq(StringUtils.isNotBlank(projectCode), AssetsDatasourceProjectRelDO::getProjectCode, projectCode));
        if (count == null || count <= 0) {
            throw new ServiceException("当前项目无权访问该数据源");
        }
    }

    private String buildDbGptUserInput(AiAskDataSqlReqVO reqVO, Long userPermissionLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(reqVO.getQuestion()).append("\n\n");
        appendCurrentUserDataPermission(builder, userPermissionLevel);
        return builder.toString();
    }

    private void appendMetricIntentRules(StringBuilder builder) {
        builder.append("\n\n【指标口径识别规则】\n");
        builder.append("- 用户提到“销售量、销量、销售次数、订单量、租赁量、租赁次数、购买次数、交易次数、贡献 top10%”时，默认按数量/次数口径计算，");
        builder.append("例如 COUNT(*)、COUNT(order_id/rental_id) 或 SUM(quantity)，不要用 SUM(amount/payment/price) 代替。\n");
        builder.append("- 只有用户明确提到“销售额、消费金额、收入、GMV、金额、客单价、高价值用户”时，才按金额口径计算。\n");
        builder.append("- “贡献 top10%”应先按用户问题中的指标聚合并降序排序，再取 top 10%；不得更换用户指定的指标口径。\n");
    }

    private String dataPermissionLevelName(Long level) {
        if (level == null) {
            return "未配置";
        }
        if (level == 1L) {
            return "绝密";
        }
        if (level == 2L) {
            return "机密";
        }
        if (level == 3L) {
            return "秘密";
        }
        if (level == 4L) {
            return "内部";
        }
        if (level == 5L) {
            return "公开";
        }
        return "未知";
    }

    private void appendCurrentUserPermission(StringBuilder builder, Long userPermissionLevel) {
        builder.append("\n\n【当前用户数据权限】\n");
        if (userPermissionLevel == null) {
            builder.append("- 当前用户未配置 data_permission_level，必须以服务端最终资产预览/查询权限结果为准。\n");
        } else {
            builder.append("- 当前用户 data_permission_level = ").append(userPermissionLevel).append("。数字越小权限越高：1=绝密、2=机密、3=秘密、4=内部、5=公开。\n");
            builder.append("- 字段 sensitive_level_id 小于当前用户 data_permission_level 时，该字段不可查询、不可展示、不可用于过滤/排序/分组/统计和报告输出。\n");
        }
        builder.append("- 命中脱敏规则的字段只能使用脱敏后的值；隐藏字段不得反推或绕过。\n");
    }

    private void appendCurrentUserDataPermission(StringBuilder builder, Long userPermissionLevel) {
        builder.append("当前用户数据等级 data_permission_level：")
                .append(userPermissionLevel == null ? "未配置，按运行时后端权限结果为准" : userPermissionLevel)
                .append(userPermissionLevel == null ? "" : "（" + dataPermissionLevelName(userPermissionLevel) + "）")
                .append("请严格按照数据字段权限查询");
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

    private String buildPromptContext(String question, List<AiSkillRespVO> skills) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(StringUtils.defaultString(question)).append("\n\n");
        builder.append("可引用Skill：\n");
        if (skills == null || skills.isEmpty()) {
            builder.append("- 未命中已发布Skill。需要先生成平台级Skill或表级Skill。\n");
            return builder.toString();
        }
        for (AiSkillRespVO skill : skills) {
            builder.append("\n## ").append(skill.getSkillName()).append("\n");
            builder.append("- 编码：").append(skill.getSkillCode()).append("\n");
            builder.append("- 类型：").append(skill.getSkillType()).append("\n");
            builder.append("- 版本：").append(skill.getVersion()).append("\n\n");
            builder.append(skill.getContent()).append("\n");
        }
        return builder.toString();
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

    private String buildSystemPrompt(String context, Long userPermissionLevel) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个数据查询分析助手。请根据用户问题、当前用户信息和可用上下文回答。\n\n");
        appendCurrentUserPermission(prompt, userPermissionLevel);
        prompt.append("【可用上下文】\n");
        prompt.append(context).append("\n");

        return prompt.toString();
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
        cleaned = removeDbGptFormatWarnings(cleaned);
        cleaned = removeHtmlInterpreterSummary(cleaned);
        if (containsFullHtmlDocument(cleaned)) {
            String text = htmlDocumentToPlainText(cleaned);
            if (StringUtils.isNotBlank(text)) {
                cleaned = text;
            }
        }
        return cleaned.trim();
    }

    private String removeSkillEchoPrefix(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
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
