package com.datamaster.module.assets.service.skill;

import java.util.List;

/**
 * AI ask-data context service interface.
 * Builds context for SQL generation from user questions.
 */
public interface IAiAskDataContextService {

    /**
     * Extract keywords from user question for skill search.
     *
     * @param question user question
     * @return extracted keywords
     */
    List<String> extractKeywords(String question);

    /**
     * Build full context for SQL generation.
     *
     * @param question user question
     * @param assetId  optional asset ID to focus on
     * @return context string for LLM prompt
     */
    String buildFullContext(String question, Long assetId);

    /**
     * Extract table and column hints from question.
     *
     * @param question user question
     * @param skillContent skill content
     * @return table/column hints
     */
    TableColumnHints extractTableColumnHints(String question, String skillContent);

    /**
     * Data class for table and column hints.
     */
    class TableColumnHints {
        private String suggestedTable;
        private List<String> suggestedColumns;
        private String timeRange;
        private String metricHint;
        private String dimensionHint;

        public String getSuggestedTable() { return suggestedTable; }
        public void setSuggestedTable(String suggestedTable) { this.suggestedTable = suggestedTable; }
        public List<String> getSuggestedColumns() { return suggestedColumns; }
        public void setSuggestedColumns(List<String> suggestedColumns) { this.suggestedColumns = suggestedColumns; }
        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }
        public String getMetricHint() { return metricHint; }
        public void setMetricHint(String metricHint) { this.metricHint = metricHint; }
        public String getDimensionHint() { return dimensionHint; }
        public void setDimensionHint(String dimensionHint) { this.dimensionHint = dimensionHint; }
    }
}
