package com.datamaster.module.ontology.service.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Align complete calendar buckets by ordinal position, never infer equivalent business days. */
public final class MetricPeriodBucketRules {
    private MetricPeriodBucketRules() { }
    public static void validate(MetricQueryReqVO current, MetricQueryReqVO baseline) {
        if ("NONE".equals(current.getTimeBucket())) return;
        Bounds bounds = bounds(current, baseline);
        require(bounds.count > 0 && bounds.count <= 500, "两期时间桶数量必须在1到500之间");
    }
    public static void calculate(MetricComparisonRespVO result, MetricQueryReqVO current, MetricQueryReqVO baseline) {
        require(result != null && current != null && baseline != null, "两期时间桶请求或结果缺失");
        if ("NONE".equals(current.getTimeBucket())) { MetricComparisonRules.calculate(result); return; }
        require(result.getCurrent() != null && result.getBaseline() != null && result.getCurrent().getRows() != null && result.getBaseline().getRows() != null, "两期时间桶结果不可用");
        Bounds bounds = bounds(current, baseline);
        MetricQueryRespVO original = result.getBaseline();
        MetricQueryRespVO aligned = JSON.parseObject(JSON.toJSONString(original,
                com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue), MetricQueryRespVO.class);
        for (Map<String,Object> row : aligned.getRows()) {
            require(row != null && row.containsKey("time_bucket"), "基期时间桶字段缺失");
            LocalDate actual = parseBucket(row.get("time_bucket"), current.getTimeBucket());
            long index = bounds.unit.between(bounds.baseline, actual);
            require(index >= 0 && index < bounds.count && bounds.baseline.plus(index, bounds.unit).equals(actual), "基期桶不属于已声明日历区间");
            row.put("time_bucket", format(bounds.current.plus(index, bounds.unit), current.getTimeBucket()));
        }
        result.setBaseline(aligned);
        try { MetricComparisonRules.calculate(result); }
        finally { result.setBaseline(original); }
        for (Map<String,Object> row : result.getRows()) {
            LocalDate actual = parseBucket(row.get("time_bucket"), current.getTimeBucket());
            long index = bounds.unit.between(bounds.current, actual);
            require(index >= 0 && index < bounds.count && bounds.current.plus(index, bounds.unit).equals(actual), "本期桶不属于已声明日历区间");
            row.put("baseline_time_bucket", format(bounds.baseline.plus(index, bounds.unit), current.getTimeBucket()));
        }
        List<Map<String,Object>> rows = new ArrayList<>(result.getRows());
        rows.sort(java.util.Comparator.comparing(row -> String.valueOf(row.get("time_bucket"))));
        result.setRows(rows);
        MetricQueryColumnVO counterpart = new MetricQueryColumnVO(); counterpart.setKey("baseline_time_bucket"); counterpart.setLabel("对应基期桶（缺失不补零）"); counterpart.setDataType("string");
        List<MetricQueryColumnVO> columns = new ArrayList<>(result.getColumns()); columns.add(1, counterpart); result.setColumns(columns);
    }
    private static Bounds bounds(MetricQueryReqVO current, MetricQueryReqVO baseline) {
        String grain = current.getTimeBucket();
        require(grain != null && Set.of("DAY", "MONTH", "YEAR").contains(grain), "两期时间桶粒度无效");
        require(Objects.equals(grain, baseline.getTimeBucket()), "两期分桶粒度必须一致");
        ChronoUnit unit = "DAY".equals(grain) ? ChronoUnit.DAYS : "MONTH".equals(grain) ? ChronoUnit.MONTHS : ChronoUnit.YEARS;
        LocalDate start = boundary(current.getStartTime(), grain), end = boundary(current.getEndTime(), grain);
        LocalDate beforeStart = boundary(baseline.getStartTime(), grain), beforeEnd = boundary(baseline.getEndTime(), grain);
        long count = unit.between(start, end), beforeCount = unit.between(beforeStart, beforeEnd);
        require(count == beforeCount && count > 0 && count <= 500, "时间桶比较要求两期完整日历桶数一致（1至500）；不等长月份/闰年请改用总量或明确自定义区间");
        return new Bounds(start, beforeStart, unit, count);
    }
    private static LocalDate boundary(String text, String grain) {
        try {
            LocalDate date;
            if (text.length() == 10) date = LocalDate.parse(text);
            else { LocalDateTime time = LocalDateTime.parse(text); require(time.toLocalTime().equals(LocalTime.MIDNIGHT), "时间桶两期比较的起止时间必须在业务日零点"); date = time.toLocalDate(); }
            if ("MONTH".equals(grain) || "YEAR".equals(grain)) require(date.getDayOfMonth() == 1, "月/年桶比较必须从月初开始并在月初结束");
            if ("YEAR".equals(grain)) require(date.getMonthValue() == 1, "年桶比较必须从1月1日开始并在1月1日结束");
            return date;
        } catch (DateTimeException | NullPointerException error) { throw new com.datamaster.common.exception.ServiceException("时间桶比较区间格式无效"); }
    }
    private static LocalDate parseBucket(Object value, String grain) {
        require(value instanceof String, "来源时间桶格式无效");
        try {
            if ("DAY".equals(grain)) return LocalDate.parse((String) value);
            if ("MONTH".equals(grain)) return YearMonth.parse((String) value).atDay(1);
            return Year.parse((String) value).atDay(1);
        } catch (DateTimeException error) { throw new com.datamaster.common.exception.ServiceException("来源时间桶标签无效"); }
    }
    private static String format(LocalDate value, String grain) { return MetricTrendRules.calendarLabel(value, grain); }
    private static final class Bounds {
        final LocalDate current, baseline; final ChronoUnit unit; final long count;
        Bounds(LocalDate current, LocalDate baseline, ChronoUnit unit, long count) { this.current = current; this.baseline = baseline; this.unit = unit; this.count = count; }
    }
}
