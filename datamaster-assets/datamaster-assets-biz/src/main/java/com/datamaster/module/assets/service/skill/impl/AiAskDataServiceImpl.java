package com.datamaster.module.assets.service.skill.impl;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataPrepareReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataPrepareRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataSqlReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataSqlRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillMapper;
import com.datamaster.module.assets.model.dto.dbgpt.DbGptChatCompletionRequest;
import com.datamaster.module.assets.model.dto.dbgpt.DbGptChatCompletionResponse;
import com.datamaster.module.assets.model.dto.dbgpt.DbGptChatMessage;
import com.datamaster.module.assets.config.DbGptProperties;
import com.datamaster.module.assets.service.dbgpt.IDbGptClientService;
import com.datamaster.module.assets.service.skill.IAiAskDataContextService;
import com.datamaster.module.assets.service.skill.IAiAskDataService;
import com.datamaster.module.assets.service.skill.IAiSqlSafetyService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
    private AssetsAssetMapper assetsAssetMapper;

    @Resource
    private AssetsDatasourceMapper assetsDatasourceMapper;

    @Resource
    private IAiSqlSafetyService aiSqlSafetyService;

    @Resource
    private IAiAskDataContextService aiAskDataContextService;

    @Resource
    private DataSourceFactory dataSourceFactory;

    @Resource
    private IDbGptClientService dbGptClientService;

    @Resource
    private DbGptProperties dbGptProperties;

    private static final int DEFAULT_MAX_ROWS = 1000;
    private static final int MAX_EXECUTE_ROWS = 10000;

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
    public List<Map<String, Object>> executeSql(Long datasourceId, String sql, Integer maxRows) {
        if (datasourceId == null) {
            throw new ServiceException("数据源ID不能为空");
        }

        // Validate SQL safety
        String safeSql = aiSqlSafetyService.validateReadOnly(sql);

        // Get datasource
        AssetsDatasourceDO datasource = assetsDatasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }

        // Build DbQuery
        DbQueryProperty property = new DbQueryProperty(
                datasource.getDatasourceType(),
                datasource.getIp(),
                datasource.getPort(),
                datasource.getDatasourceConfig()
        );

        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            if (!dbQuery.valid()) {
                throw new ServiceException("数据源连接失败");
            }

            // Execute query with row limit
            int limit = maxRows != null ? Math.min(maxRows, MAX_EXECUTE_ROWS) : DEFAULT_MAX_ROWS;
            String limitedSql = hasLimitClause(safeSql) ? safeSql : safeSql + " LIMIT " + limit;

            PageResult<Map<String, Object>> result = dbQuery.queryByPage(limitedSql, 0, limit);
            return result.getData() != null ? result.getData() : Collections.emptyList();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("SQL执行失败: " + e.getMessage());
        } finally {
            dbQuery.close();
        }
    }

    @Override
    public AiAskDataSqlRespVO chat(AiAskDataSqlReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getQuestion())) {
            throw new ServiceException("用户问题不能为空");
        }

        // First, generate SQL context
        AiAskDataSqlRespVO respVO = generateSql(reqVO);

        // If execute flag is set and SQL is provided, execute it
        if (Boolean.TRUE.equals(reqVO.getExecute()) && StringUtils.isNotBlank(respVO.getSql())
                && !respVO.getSql().startsWith("--")) {
            try {
                Long datasourceId = extractDatasourceId(respVO);
                if (datasourceId != null) {
                    List<Map<String, Object>> results = executeSql(datasourceId, respVO.getSql(), DEFAULT_MAX_ROWS);
                    respVO.setExecuteResult(results);
                    respVO.setRowCount(results.size());
                    respVO.setExecuteSuccess(true);
                }
            } catch (Exception e) {
                respVO.setExecuteSuccess(false);
                respVO.setExecuteError(e.getMessage());
            }
        }

        return respVO;
    }

    private boolean hasLimitClause(String sql) {
        if (StringUtils.isBlank(sql)) {
            return false;
        }
        return Pattern.compile("\\blimit\\s+\\d+\\b", Pattern.CASE_INSENSITIVE).matcher(sql).find();
    }

    @Override
    public AiAskDataSqlRespVO chatWithDbGpt(AiAskDataSqlReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getQuestion())) {
            throw new ServiceException("用户问题不能为空");
        }
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
        if (isGeneralChat) {
            gptRequest.setMessages(Collections.singletonList(
                    new DbGptChatMessage("user", reqVO.getQuestion())
            ));
        } else {
            gptRequest.setMessages(Arrays.asList(
                    new DbGptChatMessage("system", "你是 DataMaster 的智能问数助手。当前是数据库问答场景，"
                            + "必须基于已选择的数据源回答。用户要求查询数据、统计、排序、筛选时，必须输出一个可执行的 SELECT SQL，"
                            + "并用 ```sql 代码块包裹；不要复述规则，不要要求用户再次提出问题。"
                            + "生成 SQL 前参考 " + dbGptProperties.getSkillSpaceName() + " 知识库中的字段、质量风险和业务口径。"),
                    new DbGptChatMessage("user", reqVO.getQuestion())
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
                if (!"chat_normal".equals(reqVO.getChatMode()) && !looksLikeDataQuestion(reqVO.getQuestion())) {
                    // pass through — let AI handle it
                }

                DbGptChatCompletionRequest gptRequest = buildDbGptAskRequest(reqVO, true);
                dbGptClientService.chatCompletionV1Stream(gptRequest, chunk -> {
                    try {
                        sendSse(emitter, "message", chunk);
                    } catch (IOException e) {
                        throw new ServiceException("AI问数流式响应发送失败: " + e.getMessage());
                    }
                });
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
        if (isGeneralChat) {
            gptRequest.setMessages(Collections.singletonList(
                    new DbGptChatMessage("user", reqVO.getQuestion())
            ));
        } else {
            gptRequest.setMessages(Arrays.asList(
                    new DbGptChatMessage("system", "你是 DataMaster 的智能问数助手。当前是数据库问答场景，"
                            + "必须基于已选择的数据源回答。用户要求查询数据、统计、排序、筛选时，必须输出一个可执行的 SELECT SQL，"
                            + "并用 ```sql 代码块包裹；不要复述规则，不要要求用户再次提出问题。"
                            + "生成 SQL 前参考 " + dbGptProperties.getSkillSpaceName() + " 知识库中的字段、质量风险和业务口径。"),
                    new DbGptChatMessage("user", reqVO.getQuestion())
            ));
        }
        return gptRequest;
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

    private Long extractDatasourceId(AiAskDataSqlRespVO respVO) {
        if (respVO.getReferencedSkills() != null) {
            for (AiSkillRespVO skill : respVO.getReferencedSkills()) {
                if ("TABLE".equals(skill.getBizObjectType()) && skill.getBizObjectId() != null) {
                    AssetsAssetDO asset = assetsAssetMapper.selectById(skill.getBizObjectId());
                    if (asset != null) {
                        return asset.getDatasourceId();
                    }
                }
            }
        }
        return null;
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

}
