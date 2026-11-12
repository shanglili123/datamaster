package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import java.time.*;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Explicit absent calendar buckets for a complete single-series result: null, never invented zero. */
public final class MetricTrendRules {
    private MetricTrendRules() { }
    public static void completeCalendar(MetricQueryReqVO request, MetricQueryRespVO result) {
        if (!Boolean.TRUE.equals(request.getRequireComplete()) || "NONE".equals(request.getTimeBucket()) || !request.getDimensionPropertyIds().isEmpty()) return;
        require(Boolean.FALSE.equals(result.getTruncated()), "不能用不完整分组生成完整趋势");
        String grain = request.getTimeBucket();
        LocalDateTime start = parse(request.getStartTime()), end = parse(request.getEndTime());
        LocalDateTime first = floor(start, grain);
        List<String> expected = new ArrayList<>();
        for (LocalDateTime cursor = first; cursor.isBefore(end); cursor = next(cursor, grain)) {
            require(expected.size() < request.getLimit(), "完整趋势日历桶数超出上限，请缩小区间或改用更粗粒度");
            expected.add(label(cursor, grain));
        }
        Map<String,Map<String,Object>> actual = new LinkedHashMap<>();
        require(result.getRows() != null, "完整趋势结果不可用");
        for (Map<String,Object> row : result.getRows()) {
            require(row != null && row.containsKey("metric_value"), "完整趋势存在无效行或缺少值字段");
            Object key = row.get("time_bucket");
            require(key instanceof String && expected.contains(key) && !actual.containsKey(key), "完整趋势来源桶缺失、重复或超出声明区间");
            actual.put((String) key, row);
        }
        List<Map<String,Object>> rows = new ArrayList<>();
        for (String key : expected) {
            Map<String,Object> row = new LinkedHashMap<>(); row.put("time_bucket", key); row.put("metric_value", null);
            if (actual.containsKey(key)) row.putAll(actual.get(key));
            rows.add(row);
        }
        result.setHasMore(false);
        result.setRows(rows);
        result.setNotice((result.getNotice() == null ? "" : result.getNotice()) + "完整日历趋势中未返回的桶显式为缺失null，不按零补值；首末部分桶仅统计原所选区间。");
    }
    public static void validateCalendar(MetricQueryReqVO request) {
        if (!Boolean.TRUE.equals(request.getRequireComplete()) || "NONE".equals(request.getTimeBucket()) || !request.getDimensionPropertyIds().isEmpty()) return;
        LocalDateTime start = floor(parse(request.getStartTime()), request.getTimeBucket()), end = parse(request.getEndTime());
        int count = 0;
        for (LocalDateTime cursor = start; cursor.isBefore(end); cursor = next(cursor, request.getTimeBucket())) require(++count <= request.getLimit(), "完整趋势日历桶数超出上限，请缩小区间或改用更粗粒度");
    }
    private static LocalDateTime parse(String text) {
        try { return text.length() == 10 ? LocalDate.parse(text).atStartOfDay() : LocalDateTime.parse(text); }
        catch (DateTimeException | NullPointerException error) { throw new com.datamaster.common.exception.ServiceException("完整趋势须明确有效业务时间区间"); }
    }
    private static LocalDateTime floor(LocalDateTime value, String grain) {
        if ("DAY".equals(grain)) return value.toLocalDate().atStartOfDay();
        if ("MONTH".equals(grain)) return value.toLocalDate().withDayOfMonth(1).atStartOfDay();
        require("YEAR".equals(grain), "趋势分桶粒度无效"); return LocalDate.of(value.getYear(), 1, 1).atStartOfDay();
    }
    private static LocalDateTime next(LocalDateTime value, String grain) { return "DAY".equals(grain) ? value.plusDays(1) : "MONTH".equals(grain) ? value.plusMonths(1) : value.plusYears(1); }
    private static String label(LocalDateTime value, String grain) { return "DAY".equals(grain) ? value.toLocalDate().toString() : "MONTH".equals(grain) ? YearMonth.from(value).toString() : Year.from(value).toString(); }
}
