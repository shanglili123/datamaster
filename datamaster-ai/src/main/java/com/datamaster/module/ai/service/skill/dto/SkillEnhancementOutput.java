package com.datamaster.module.ai.service.skill.dto;

import lombok.Data;
import java.util.List;

@Data
public class SkillEnhancementOutput {
    private String businessSummary;
    private List<FieldUsage> fieldUsages;
    private List<String> metricHints;
    private List<String> timeHints;
    private List<String> exampleQuestions;
    private List<String> sqlRules;
    private List<String> qualityHints;

    @Data
    public static class FieldUsage {
        private String field;
        private String usage;
        private String notice;
    }
}
