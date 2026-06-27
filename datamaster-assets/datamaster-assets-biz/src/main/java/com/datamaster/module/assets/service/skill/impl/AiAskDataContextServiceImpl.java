package com.datamaster.module.assets.service.skill.impl;

import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillMapper;
import com.datamaster.module.assets.service.skill.IAiAskDataContextService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI ask-data context service implementation.
 * Builds context for SQL generation from user questions.
 */
@Service
public class AiAskDataContextServiceImpl implements IAiAskDataContextService {

    @Resource
    private AiSkillMapper aiSkillMapper;

    // Common Chinese time-related patterns
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(今天|昨天|前天|本周|上周|本月|上月|今年|去年|最近\\d+[天周月年]|过去\\d+[天周月年]|近\\d+[天周月年]|\\d{4}年\\d{1,2}月|\\d{4}-\\d{2}-\\d{2}|\\d{4}/\\d{2}/\\d{2})");

    // Common metric patterns
    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "(总数|数量|金额|总额|平均|最大|最小|汇总|统计|求和|计数|占比|比率|百分比|增长率|同比|环比)");

    // Common dimension patterns
    private static final Pattern DIMENSION_PATTERN = Pattern.compile(
            "(按|分|组|类别|类型|部门|项目|区域|地区|城市|省份)");

    @Override
    public List<String> extractKeywords(String question) {
        if (StringUtils.isBlank(question)) {
            return Collections.emptyList();
        }

        List<String> keywords = new ArrayList<>();

        // Extract Chinese words (simple segmentation by common delimiters)
        String[] parts = question.split("[\\s,，.。!！?？、；;：:（）()\\[\\]【】{}]+");
        for (String part : parts) {
            if (StringUtils.isNotBlank(part) && part.length() >= 2) {
                keywords.add(part);
            }
        }

        // Extract table/column-like patterns (e.g., order_info, t_user)
        Pattern tablePattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        Matcher matcher = tablePattern.matcher(question);
        while (matcher.find()) {
            String word = matcher.group(1);
            if (word.length() >= 3) {
                keywords.add(word.toLowerCase());
            }
        }

        // Remove duplicates while preserving order
        Set<String> seen = new LinkedHashSet<>();
        List<String> uniqueKeywords = new ArrayList<>();
        for (String keyword : keywords) {
            if (seen.add(keyword.toLowerCase())) {
                uniqueKeywords.add(keyword);
            }
        }

        return uniqueKeywords;
    }

    @Override
    public String buildFullContext(String question, Long assetId) {
        StringBuilder context = new StringBuilder();

        // Add user question
        context.append("## 用户问题\n\n");
        context.append(question).append("\n\n");

        // Extract time range
        String timeRange = extractTimeRange(question);
        if (StringUtils.isNotBlank(timeRange)) {
            context.append("## 识别到的时间范围\n\n");
            context.append(timeRange).append("\n\n");
        }

        // Extract metric hints
        String metricHint = extractMetricHint(question);
        if (StringUtils.isNotBlank(metricHint)) {
            context.append("## 识别到的指标意图\n\n");
            context.append(metricHint).append("\n\n");
        }

        // Extract dimension hints
        String dimensionHint = extractDimensionHint(question);
        if (StringUtils.isNotBlank(dimensionHint)) {
            context.append("## 识别到的维度意图\n\n");
            context.append(dimensionHint).append("\n\n");
        }

        // Load relevant skills
        List<String> keywords = extractKeywords(question);
        String keyword = keywords.isEmpty() ? question : String.join(" ", keywords.subList(0, Math.min(3, keywords.size())));

        List<AiSkillDO> matchedSkills = aiSkillMapper.selectPublishedByKeyword(keyword);
        AiSkillDO assetSkill = assetId != null ? aiSkillMapper.selectByBizObject("TABLE", assetId) : null;

        context.append("## 可引用的Skill\n\n");

        // Add asset-specific skill
        if (assetSkill != null && "PUBLISHED".equals(assetSkill.getStatus())) {
            context.append("### ").append(assetSkill.getSkillName()).append("\n\n");
            context.append(assetSkill.getContent()).append("\n\n");
        }

        // Add matched skills
        if (matchedSkills != null && !matchedSkills.isEmpty()) {
            for (AiSkillDO skill : matchedSkills) {
                // Skip if already added
                if (assetSkill != null && skill.getId().equals(assetSkill.getId())) {
                    continue;
                }
                context.append("### ").append(skill.getSkillName()).append("\n\n");
                context.append(skill.getContent()).append("\n\n");
            }
        }

        // Add SQL generation instructions
        context.append("## SQL生成要求\n\n");
        context.append("1. 只生成SELECT查询语句\n");
        context.append("2. 根据识别到的时间范围添加WHERE条件\n");
        context.append("3. 根据识别到的指标意图选择合适的聚合函数\n");
        context.append("4. 根据识别到的维度意图添加GROUP BY子句\n");
        context.append("5. 使用Skill中推荐的字段名和表名\n");
        context.append("6. 如果字段缺少注释或口径不明确，在回答中标注待确认\n");

        return context.toString();
    }

    @Override
    public TableColumnHints extractTableColumnHints(String question, String skillContent) {
        TableColumnHints hints = new TableColumnHints();

        // Extract suggested table from skill content
        Pattern tableNamePattern = Pattern.compile("表名[：:]\\s*`?([a-zA-Z_][a-zA-Z0-9_]*)`?");
        Matcher tableMatcher = tableNamePattern.matcher(skillContent);
        if (tableMatcher.find()) {
            hints.setSuggestedTable(tableMatcher.group(1));
        }

        // Extract time range
        hints.setTimeRange(extractTimeRange(question));

        // Extract metric hint
        hints.setMetricHint(extractMetricHint(question));

        // Extract dimension hint
        hints.setDimensionHint(extractDimensionHint(question));

        return hints;
    }

    private String extractTimeRange(String question) {
        if (StringUtils.isBlank(question)) {
            return "";
        }

        Matcher matcher = TIME_PATTERN.matcher(question);
        StringBuilder timeRange = new StringBuilder();
        while (matcher.find()) {
            if (timeRange.length() > 0) {
                timeRange.append(", ");
            }
            timeRange.append(matcher.group());
        }

        return timeRange.toString();
    }

    private String extractMetricHint(String question) {
        if (StringUtils.isBlank(question)) {
            return "";
        }

        Matcher matcher = METRIC_PATTERN.matcher(question);
        StringBuilder metric = new StringBuilder();
        while (matcher.find()) {
            if (metric.length() > 0) {
                metric.append(", ");
            }
            metric.append(matcher.group());
        }

        return metric.toString();
    }

    private String extractDimensionHint(String question) {
        if (StringUtils.isBlank(question)) {
            return "";
        }

        Matcher matcher = DIMENSION_PATTERN.matcher(question);
        StringBuilder dimension = new StringBuilder();
        while (matcher.find()) {
            if (dimension.length() > 0) {
                dimension.append(", ");
            }
            dimension.append(matcher.group());
        }

        return dimension.toString();
    }
}
