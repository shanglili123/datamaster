package com.datamaster.module.assets.service.skill.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.core.domain.entity.SysUser;
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
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceProjectRelDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillReportTemplateDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.assetColumn.AssetsAssetColumnMapper;
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
    private AssetsAssetColumnMapper assetsAssetColumnMapper;

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
        String systemPrompt = buildSystemPrompt(fullContext);

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
        boolean isGeneralChat = "chat_normal".equals(gptRequest.getChatMode());
        String userInput = buildDbGptUserInput(reqVO);
        if (isGeneralChat) {
            gptRequest.setMessages(Collections.singletonList(
                    new DbGptChatMessage("user", userInput)
            ));
        } else {
            gptRequest.setMessages(Arrays.asList(
                    new DbGptChatMessage("system", "你是 DataMaster 的智能问数助手。当前是数据库问答场景，"
                            + "必须基于已选择的数据源回答。优先直接返回查询结果、分析结论和必要说明。"
                            + "只有用户或请求参数明确要求返回SQL时，才附带可执行的 SELECT SQL。"
                            + "生成 SQL 前参考 " + dbGptProperties.getSkillSpaceName() + " 知识库中的字段、质量风险和业务口径。"),
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
                DbGptChatCompletionRequest gptRequest = buildDbGptAskRequest(reqVO, true);
                dbGptClientService.chatCompletionV1Stream(gptRequest, chunk -> {
                    try {
                        fullReply.append(chunk);
                        sendSse(emitter, "message", chunk);
                    } catch (IOException e) {
                        throw new ServiceException("AI问数流式响应发送失败: " + e.getMessage());
                    }
                });
                appendSqlWhenRequested(reqVO, fullReply.toString(), emitter);
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

    private void appendSqlWhenRequested(AiAskDataSqlReqVO reqVO, String reply, SseEmitter emitter) throws IOException {
        if (!Boolean.TRUE.equals(reqVO.getReturnSql())) {
            return;
        }
        String sql = extractSqlFromReply(reply);
        if (StringUtils.isBlank(sql)) {
            try {
                sql = generateReturnSql(reqVO);
            } catch (Exception e) {
                log.warn("返回SQL兜底生成失败：{}", e.getMessage());
            }
        }
        if (StringUtils.isBlank(sql) || sql.startsWith("--")) {
            sendSse(emitter, "sql", "");
            return;
        }
        sendSse(emitter, "sql", sql);
    }

    private String generateReturnSql(AiAskDataSqlReqVO reqVO) {
        DbGptChatCompletionRequest sqlRequest = buildDbGptAskRequest(reqVO, false);
        sqlRequest.setTemperature(0.1);
        sqlRequest.setMaxTokens(2000);
        sqlRequest.setMessages(Collections.singletonList(
                new DbGptChatMessage("user", buildReturnSqlOnlyInput(reqVO))
        ));
        DbGptChatCompletionResponse response = dbGptClientService.chatCompletionV1(sqlRequest);
        return extractSqlFromReply(extractReply(response));
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
        builder.append("你是 DataMaster 报告数据生成助手。请基于已选择的数据库和可用知识库，为报告模板生成结构化数据。\n");
        builder.append("必须只返回一个合法 JSON 对象，不要返回 Markdown，不要返回 HTML，不要使用代码块包裹。\n");
        builder.append("不要输出分析过程、执行步骤、SQL查询过程、Python代码或HTML报告，只返回前端模板渲染需要的数据对象。\n");
        builder.append("返回字段必须满足模板 dataSchema.required 和 dataSchema.fields；图表和表格数据必须返回数组。\n");
        builder.append("字段路径必须按点号组织成嵌套对象，例如 metrics.totalOrderCount 必须返回为 {\"metrics\":{\"totalOrderCount\":...}}。\n");
        appendMetricIntentRules(builder);
        if (Boolean.TRUE.equals(reqVO.getReturnSql())) {
            builder.append("本次需要在 JSON 的 sql 字段返回用于校验的 SELECT SQL。\n");
        } else {
            builder.append("本次不要求返回 SQL，JSON 的 sql 字段请返回 null 或省略。\n");
        }
        if (reqVO.getParams() != null && !reqVO.getParams().isEmpty()) {
            builder.append("\n【报告参数】\n").append(JSON.toJSONString(reqVO.getParams())).append("\n");
        }
        builder.append("\n【必须返回的数据字段】\n").append(buildReportDataSchemaText(template.getTemplateContent())).append("\n");
        builder.append("\n【报告模板JSON】\n").append(template.getTemplateContent()).append("\n");
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

    private DbGptChatCompletionRequest buildDbGptAskRequest(AiAskDataSqlReqVO reqVO, boolean stream) {
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
        boolean isGeneralChat = "chat_normal".equals(gptRequest.getChatMode());
        String userInput = buildDbGptUserInput(reqVO);
        if (isGeneralChat) {
            gptRequest.setMessages(Collections.singletonList(
                    new DbGptChatMessage("user", userInput)
            ));
        } else {
            gptRequest.setMessages(Arrays.asList(
                    new DbGptChatMessage("system", "你是 DataMaster 的智能问数助手。当前是数据库问答场景，"
                            + "必须基于已选择的数据源回答。优先直接返回查询结果、分析结论和必要说明。"
                            + "只有用户或请求参数明确要求返回SQL时，才附带可执行的 SELECT SQL。"
                            + "生成 SQL 前参考 " + dbGptProperties.getSkillSpaceName() + " 知识库中的字段、质量风险和业务口径。"),
                    new DbGptChatMessage("user", userInput)
            ));
        }
        return gptRequest;
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

    private String buildDbGptUserInput(AiAskDataSqlReqVO reqVO) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于已选择的数据库和可用知识库回答用户问题。");
        builder.append("默认由 DB-GPT 完成数据查询与分析，不要只返回待执行 SQL。");
        builder.append("当前是问答模式，不要生成 HTML 页面，不要调用或输出 html_interpreter，不要返回 <!DOCTYPE html>、<html>、工具执行 JSON 或“报告已生成并渲染完成”等状态话术。");
        builder.append("请直接用中文 Markdown 回答用户问题，严格围绕用户原始问题中的分析对象、维度和输出要求组织答案，不要擅自增加固定业务域。");
        appendMetricIntentRules(builder);
        appendDatasourceDialectRules(builder, reqVO);
        appendCurrentUserPermission(builder);
        if (Boolean.TRUE.equals(reqVO.getReturnSql())) {
            builder.append("本次需要返回用于校验的 SQL，请在答案末尾用 ```sql 代码块附带实际使用或建议校验的 SELECT SQL。");
        } else {
            builder.append("本次不要求返回 SQL；除非无法回答且必须说明查询思路，否则不要输出 SQL。");
        }
        builder.append("\n\n用户问题：").append(reqVO.getQuestion());
        appendAuthorizedAssetScope(builder, reqVO);
        return builder.toString();
    }

    private void appendDatasourceDialectRules(StringBuilder builder, AiAskDataSqlReqVO reqVO) {
        if (reqVO == null || reqVO.getDatasourceId() == null) {
            return;
        }
        AssetsDatasourceDO datasource = assetsDatasourceMapper.selectById(reqVO.getDatasourceId());
        if (datasource == null) {
            return;
        }
        String dbType = firstNonBlank(datasource.getDatasourceType()).toLowerCase(Locale.ROOT);
        String dbName = firstNonBlank(datasource.getDatasourceName(), String.valueOf(datasource.getId()));
        builder.append("\n\n【当前数据源方言】\n");
        builder.append("- 数据源：").append(dbName).append("\n");
        builder.append("- 数据库类型：").append(firstNonBlank(datasource.getDatasourceType(), "未知")).append("\n");
        builder.append("- 生成、探测和执行 SQL 时必须使用当前数据库类型的语法，不得混用其他数据库的系统表。\n");
        if (dbType.contains("mysql")) {
            builder.append("- 当前是 MySQL。禁止使用 SQLite 的 `sqlite_master`、PostgreSQL 的 `pg_catalog`、Oracle 的 `all_tables`。\n");
            builder.append("- 如需探测表，请使用 `information_schema.tables`，并按 `table_schema = database()` 或实际库名过滤。\n");
            builder.append("- MySQL 分页使用 `LIMIT n` 或 `LIMIT offset, n`。\n");
        } else if (dbType.contains("postgres") || dbType.contains("kingbase")) {
            builder.append("- 当前是 PostgreSQL/Kingbase。禁止使用 SQLite 的 `sqlite_master` 和 MySQL 专属语法。\n");
            builder.append("- 如需探测表，请使用 `information_schema.tables` 或 `pg_catalog.pg_tables`，分页使用 `LIMIT n OFFSET m`。\n");
        } else if (dbType.contains("oracle")) {
            builder.append("- 当前是 Oracle。禁止使用 SQLite 的 `sqlite_master` 和 MySQL 专属语法。\n");
            builder.append("- 如需探测表，请使用 `all_tables`/`user_tables`，限制行数使用 `FETCH FIRST n ROWS ONLY`。\n");
        } else if (dbType.contains("sqlserver") || dbType.contains("sql server")) {
            builder.append("- 当前是 SQL Server。禁止使用 SQLite 的 `sqlite_master` 和 MySQL 专属语法。\n");
            builder.append("- 如需探测表，请使用 `information_schema.tables` 或 `sys.tables`，限制行数使用 `TOP n` 或 `OFFSET/FETCH`。\n");
        }
    }

    private void appendMetricIntentRules(StringBuilder builder) {
        builder.append("\n\n【指标口径识别规则】\n");
        builder.append("- 用户提到“销售量、销量、销售次数、订单量、租赁量、租赁次数、购买次数、交易次数、贡献 top10%”时，默认按数量/次数口径计算，");
        builder.append("例如 COUNT(*)、COUNT(order_id/rental_id) 或 SUM(quantity)，不要用 SUM(amount/payment/price) 代替。\n");
        builder.append("- 只有用户明确提到“销售额、消费金额、收入、GMV、金额、客单价、高价值用户”时，才按金额口径计算。\n");
        builder.append("- “贡献 top10%”应先按用户问题中的指标聚合并降序排序，再取 top 10%；不得更换用户指定的指标口径。\n");
    }

    private String buildReturnSqlOnlyInput(AiAskDataSqlReqVO reqVO) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于当前已选择的数据源生成一条可校验的只读 SELECT SQL。");
        builder.append("只返回 ```sql 代码块，不要返回查询结果、解释或 Markdown 之外的文本。");
        builder.append("必须使用数据库中真实存在的表名和字段名，必要时参考已选择知识库内容。");
        builder.append("如果需要限制行数，请使用当前数据库类型支持的分页/限行语法。");
        appendMetricIntentRules(builder);
        appendDatasourceDialectRules(builder, reqVO);
        appendCurrentUserPermission(builder);
        appendSelectedSkillContext(builder, reqVO);
        appendAuthorizedAssetScope(builder, reqVO);
        builder.append("\n\n用户问题：").append(reqVO.getQuestion());
        return builder.toString();
    }

    private void appendAuthorizedAssetScope(StringBuilder builder, AiAskDataSqlReqVO reqVO) {
        if (reqVO == null || reqVO.getDatasourceId() == null || reqVO.getProjectId() == null) {
            return;
        }
        List<AssetsAssetDO> assets = reqVO.getAssetId() != null
                ? Collections.singletonList(assetsAssetMapper.selectById(reqVO.getAssetId()))
                : assetsAssetMapper.findByDatasourceId(reqVO.getDatasourceId());
        if (assets == null || assets.isEmpty()) {
            return;
        }
        Long userPermissionLevel = currentUserDataPermissionLevel();
        builder.append("\n\n【当前项目授权数据资产范围】\n");
        builder.append("当前用户 data_permission_level：")
                .append(userPermissionLevel == null ? "未配置，按运行时后端权限结果为准" : userPermissionLevel)
                .append("。字段敏感等级小于该值时不可使用。\n");
        int tableCount = 0;
        for (AssetsAssetDO asset : assets) {
            if (asset == null || StringUtils.isBlank(asset.getTableName())) {
                continue;
            }
            AssetsTableGovernanceReqDTO governanceReq = new AssetsTableGovernanceReqDTO();
            governanceReq.setDatasourceId(asset.getDatasourceId());
            governanceReq.setTableName(asset.getTableName());
            governanceReq.setProjectId(reqVO.getProjectId());
            governanceReq.setProjectCode(reqVO.getProjectCode());
            governanceReq.setEntrance("AI_ASK_DATA_SCOPE");
            AssetsTableGovernanceRespDTO governance = assetsTableGovernanceApiService.resolveTable(governanceReq);
            if (governance == null || Boolean.FALSE.equals(governance.getAccessAllowed())) {
                continue;
            }
            AssetsAssetColumnPageReqVO columnReq = new AssetsAssetColumnPageReqVO();
            columnReq.setAssetId(String.valueOf(asset.getId()));
            columnReq.setProjectId(reqVO.getProjectId());
            columnReq.setProjectCode(reqVO.getProjectCode());
            List<AssetsAssetColumnDO> columns = assetsAssetColumnMapper.selectListByAuth(columnReq);
            builder.append("- ").append(asset.getTableName()).append(": ");
            if (columns == null || columns.isEmpty()) {
                builder.append("字段未配置授权清单，请谨慎使用元数据字段");
            } else {
                builder.append(columns.stream()
                        .filter(column -> isColumnVisibleForUser(column, userPermissionLevel))
                        .map(AssetsAssetColumnDO::getColumnName)
                        .filter(StringUtils::isNotBlank)
                        .limit(80)
                        .collect(java.util.stream.Collectors.joining(", ")));
            }
            builder.append("\n");
            tableCount++;
            if (tableCount >= 30) {
                builder.append("- 其余授权资产已省略，请优先使用以上最相关表。\n");
                break;
            }
        }
        builder.append("只能使用以上当前项目授权的数据资产和字段回答，未列出的表字段不要生成或查询。\n");
    }

    private void appendCurrentUserPermission(StringBuilder builder) {
        Long userPermissionLevel = currentUserDataPermissionLevel();
        builder.append("\n\n【当前用户数据权限】\n");
        if (userPermissionLevel == null) {
            builder.append("- 当前用户未配置 data_permission_level，必须以服务端最终资产预览/查询权限结果为准。\n");
        } else {
            builder.append("- 当前用户 data_permission_level = ").append(userPermissionLevel).append("。数字越小权限越高：1=绝密、2=机密、3=秘密、4=内部、5=公开。\n");
            builder.append("- 字段 sensitive_level_id 小于当前用户 data_permission_level 时，该字段不可查询、不可展示、不可用于过滤/排序/分组/统计和报告输出。\n");
        }
        builder.append("- 命中脱敏规则的字段只能使用脱敏后的值；隐藏字段不得反推或绕过。\n");
    }

    private Long currentUserDataPermissionLevel() {
        try {
            if (SecurityUtils.getLoginUser() == null) {
                return null;
            }
            SysUser user = SecurityUtils.getLoginUser().getUser();
            return user == null ? null : user.getDataPermissionLevel();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isColumnVisibleForUser(AssetsAssetColumnDO column, Long userPermissionLevel) {
        if (column == null) {
            return false;
        }
        return userPermissionLevel == null || column.getSensitiveLevelId() == null
                || column.getSensitiveLevelId() >= userPermissionLevel;
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

    private String buildSystemPrompt(String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个数据查询分析专家。根据以下数据库表结构信息和业务规则，将用户问题转化为SQL查询语句。\n\n");
        appendCurrentUserPermission(prompt);
        prompt.append("【SQL生成规则】\n");
        prompt.append("1. 只生成SELECT查询语句，不允许INSERT/UPDATE/DELETE/CREATE/DROP/ALTER/TRUNCATE\n");
        prompt.append("2. 必须使用表结构中存在的字段名和表名\n");
        prompt.append("3. 如果是聚合查询，需要添加GROUP BY子句\n");
        prompt.append("4. 如需排序，使用ORDER BY子句\n");
        prompt.append("5. 自动添加LIMIT 1000限制返回行数\n");
        prompt.append("6. 使用标准的SQL语法\n");
        prompt.append("7. 对于时间范围查询，根据表结构中的时间字段生成WHERE条件\n\n");
        prompt.append("【输出格式】\n");
        prompt.append("请用```sql\\n\\n```包裹生成的SQL语句，并在SQL上方给出简短的解释说明。\n\n");
        prompt.append("【表结构与业务规则】\n");
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
        String cleaned = removeStepMetaBlocks(reply);
        cleaned = removeHtmlInterpreterSummary(cleaned);
        if (containsFullHtmlDocument(cleaned)) {
            String text = htmlDocumentToPlainText(cleaned);
            if (StringUtils.isNotBlank(text)) {
                cleaned = text;
            }
        }
        return cleaned.trim();
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
