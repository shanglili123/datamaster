package com.datamaster.module.assets.service.skill.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
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
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillReportTemplateDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
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
            reply = extractReply(gptResponse);
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
        if (reqVO == null || reqVO.getAssetId() == null) {
            return;
        }
        AssetsAssetDO asset = assetsAssetMapper.selectById(reqVO.getAssetId());
        if (asset == null || asset.getDatasourceId() == null || StringUtils.isBlank(asset.getTableName())) {
            return;
        }
        AssetsTableGovernanceReqDTO governanceReq = new AssetsTableGovernanceReqDTO();
        governanceReq.setDatasourceId(asset.getDatasourceId());
        governanceReq.setTableName(asset.getTableName());
        governanceReq.setEntrance("AI_ASK_DATA");
        assetsTableGovernanceApiService.checkTableAccess(governanceReq);
    }

    private String buildDbGptUserInput(AiAskDataSqlReqVO reqVO) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于已选择的数据库和可用知识库回答用户问题。");
        builder.append("默认由 DB-GPT 完成数据查询与分析，不要只返回待执行 SQL。");
        if (Boolean.TRUE.equals(reqVO.getReturnSql())) {
            builder.append("本次需要返回用于校验的 SQL，请在答案末尾用 ```sql 代码块附带实际使用或建议校验的 SELECT SQL。");
        } else {
            builder.append("本次不要求返回 SQL；除非无法回答且必须说明查询思路，否则不要输出 SQL。");
        }
        builder.append("\n\n用户问题：").append(reqVO.getQuestion());
        return builder.toString();
    }

    private String buildReturnSqlOnlyInput(AiAskDataSqlReqVO reqVO) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于当前已选择的数据源生成一条可校验的只读 SELECT SQL。");
        builder.append("只返回 ```sql 代码块，不要返回查询结果、解释或 Markdown 之外的文本。");
        builder.append("必须使用数据库中真实存在的表名和字段名，必要时参考已选择知识库内容。");
        builder.append("如果需要限制行数，请添加 LIMIT 1000。");
        appendSelectedSkillContext(builder, reqVO);
        builder.append("\n\n用户问题：").append(reqVO.getQuestion());
        return builder.toString();
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
