package com.datamaster.module.ai.agent.analysis;

import java.util.*;

/** 声明式展示契约，不生成 JS、HTML、颜色或可执行 ECharts 配置。 */
public final class MetricChartSpec {
    private MetricChartSpec() { }

    public static Map<String, Object> build(Map<?, ?> evidence, String requested) {
        String type = requested == null ? "AUTO" : requested.toUpperCase(Locale.ROOT);
        if (!Arrays.asList("AUTO", "TABLE", "BAR", "LINE").contains(type)) throw new IllegalArgumentException("不支持的图表类型");
        if (!(evidence.get("columns") instanceof List) || !(evidence.get("rows") instanceof List)) throw new IllegalArgumentException("缺少真实查询结果");
        List<?> columns = (List<?>) evidence.get("columns"), rows = (List<?>) evidence.get("rows");
        String chosen = "TABLE";
        String x = null;
        if (Boolean.FALSE.equals(evidence.get("truncated")) && columns.size() == 2 && rows.size() > 1 && rows.size() <= 20) {
            try {
                Map<String, Object> analysis = MetricResultAnalysis.analyze(evidence, false);
                Map<?, ?> first = (Map<?, ?>) columns.get(0);
                x = String.valueOf(first.get("key"));
                if ("date".equalsIgnoreCase(String.valueOf(first.get("dataType")))) {
                    Map<String, Object> trend = MetricResultAnalysis.analyze(evidence, true);
                    if (((Number) trend.get("missingDateIntervals")).intValue() == 0
                            && ((Number) analysis.get("nullValueCount")).longValue() == 0) chosen = "LINE";
                } else if (rows.size() <= 7 && ((Number) analysis.get("nullValueCount")).longValue() == 0) chosen = "BAR";
            } catch (IllegalArgumentException ex) {
                chosen = "TABLE";
            }
        }
        if ("TABLE".equals(type)) chosen = "TABLE";
        else if (!"AUTO".equals(type) && !type.equals(chosen)) throw new IllegalArgumentException("结果不适合指定图表，请使用TABLE/AUTO");
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("schemaVersion", 1); spec.put("chartType", chosen.toLowerCase(Locale.ROOT));
        spec.put("title", evidence.get("metricName")); spec.put("unit", evidence.get("unit"));
        spec.put("metricId", evidence.get("metricId")); spec.put("version", evidence.get("version"));
        spec.put("columns", columns); spec.put("rows", rows); spec.put("truncated", evidence.get("truncated"));
        spec.put("xField", "TABLE".equals(chosen) ? null : x);
        spec.put("yField", "TABLE".equals(chosen) ? null : "metric_value");
        spec.put("tableAvailable", true); spec.put("singleAxis", true); spec.put("connectMissingDates", false);
        spec.put("barBaseline", "0"); spec.put("numericEncoding", "decimal-string");
        spec.put("rendererStatus", "SPEC_ONLY");
        spec.put("notice", "仅生成真实结果的声明式配置，尚未接入前端渲染器；不得执行配置中的任意文本。渲染器须保留表格和精确数值、按日期排序、处理精度和无障碍提示，不得自动求和、插值或增加第二数轴。");
        return spec;
    }
}
