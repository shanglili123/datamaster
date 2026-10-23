package com.datamaster.module.system.controller.admin.monitor.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * AI ops diagnosis report response.
 */
public class AiOpsReportRespVO {

    private String status;

    private Integer score;

    private String summary;

    private String diagnoseTime;

    private List<AiOpsMetricVO> metrics = new ArrayList<AiOpsMetricVO>();

    private List<AiOpsFindingVO> findings = new ArrayList<AiOpsFindingVO>();

    private List<AiOpsLogEventVO> logEvents = new ArrayList<AiOpsLogEventVO>();

    private List<String> suggestions = new ArrayList<String>();

    private Object server;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDiagnoseTime() {
        return diagnoseTime;
    }

    public void setDiagnoseTime(String diagnoseTime) {
        this.diagnoseTime = diagnoseTime;
    }

    public List<AiOpsMetricVO> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<AiOpsMetricVO> metrics) {
        this.metrics = metrics;
    }

    public List<AiOpsFindingVO> getFindings() {
        return findings;
    }

    public void setFindings(List<AiOpsFindingVO> findings) {
        this.findings = findings;
    }

    public List<AiOpsLogEventVO> getLogEvents() {
        return logEvents;
    }

    public void setLogEvents(List<AiOpsLogEventVO> logEvents) {
        this.logEvents = logEvents;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public Object getServer() {
        return server;
    }

    public void setServer(Object server) {
        this.server = server;
    }

    public static class AiOpsMetricVO {

        private String name;

        private String value;

        private String level;

        private String description;

        public AiOpsMetricVO() {
        }

        public AiOpsMetricVO(String name, String value, String level, String description) {
            this.name = name;
            this.value = value;
            this.level = level;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class AiOpsFindingVO {

        private String level;

        private String title;

        private String detail;

        private String suggestion;

        public AiOpsFindingVO() {
        }

        public AiOpsFindingVO(String level, String title, String detail, String suggestion) {
            this.level = level;
            this.title = title;
            this.detail = detail;
            this.suggestion = suggestion;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }

    public static class AiOpsLogEventVO {

        private String fileName;

        private String level;

        private String message;

        public AiOpsLogEventVO() {
        }

        public AiOpsLogEventVO(String fileName, String level, String message) {
            this.fileName = fileName;
            this.level = level;
            this.message = message;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
