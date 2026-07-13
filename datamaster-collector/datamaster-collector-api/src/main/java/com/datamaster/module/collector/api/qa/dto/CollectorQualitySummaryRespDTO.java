package com.datamaster.module.collector.api.qa.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 表质量结果摘要 DTO。
 */
@Data
public class CollectorQualitySummaryRespDTO {

    private Long taskId;

    private String taskName;

    private Long logId;

    private String logName;

    private String successFlag;

    private Date startTime;

    private Date endTime;

    private Long score;

    private Long problemData;

    private List<Rule> rules = new ArrayList<>();

    @Data
    public static class Rule {

        private Long id;

        private String name;

        private String ruleName;

        private String ruleType;

        private String dimensionType;

        private String evaColumn;

        private String warningLevel;

        private String status;

        private String errDescription;

        private String suggestion;
    }
}
