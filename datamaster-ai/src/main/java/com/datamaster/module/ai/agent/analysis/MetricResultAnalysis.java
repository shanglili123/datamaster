package com.datamaster.module.ai.agent.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** 仅对完整的单维数值结果进行描述性分析；不以分组之和冒充指标总量。 */
public final class MetricResultAnalysis {
    private MetricResultAnalysis() { }

    public static Map<String, Object> analyze(Map<?, ?> evidence, boolean trend) {
        if (!Boolean.FALSE.equals(evidence.get("truncated"))) throw new IllegalArgumentException("截断结果不能进行完整分析");
        List<?> columns = list(evidence.get("columns"));
        if (columns.size() != 2) throw new IllegalArgumentException("需要单个维度和数值列");
        Map<?, ?> dimension = map(columns.get(0));
        Map<?, ?> metric = map(columns.get(1));
        String key = String.valueOf(dimension.get("key"));
        if (!key.matches("d_[1-9][0-9]*") || !"metric_value".equals(metric.get("key"))
                || !com.datamaster.module.ontology.service.metric.MetricRules.numeric(String.valueOf(metric.get("dataType")))) {
            throw new IllegalArgumentException("需要已验证的维度和数值结果");
        }
        if (trend && !"date".equalsIgnoreCase(String.valueOf(dimension.get("dataType")))) {
            throw new IllegalArgumentException("趋势仅支持已发布date维度；不把时间戳自动聚合成天");
        }
        List<?> rows = list(evidence.get("rows"));
        if (rows.size() > 20) throw new IllegalArgumentException("分析最多20个完整分组");
        List<Point> points = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (Object raw : rows) {
            Map<?, ?> row = map(raw);
            Object category = row.get(key);
            if (category != null && !(category instanceof String) && !(category instanceof Boolean)) throw new IllegalArgumentException("维度类型不受支持");
            if (!seen.add(category)) throw new IllegalArgumentException("维度结果重复");
            Object value = row.get("metric_value");
            if (value != null && !(value instanceof String)) throw new IllegalArgumentException("数值必须是十进制字符串");
            BigDecimal number = value == null ? null : new BigDecimal((String) value);
            if (number != null && (number.precision() > 1000 || Math.abs((long) number.scale()) > 1000)) throw new IllegalArgumentException("数值超限");
            if (trend) {
                if (!(category instanceof String)) throw new IllegalArgumentException("日期维度不能为null");
                LocalDate.parse((String) category);
            }
            points.add(new Point(category, number));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metricId", evidence.get("metricId")); result.put("version", evidence.get("version"));
        result.put("metricName", evidence.get("metricName")); result.put("unit", evidence.get("unit"));
        result.put("groupCount", points.size());
        result.put("nullValueCount", points.stream().filter(point -> point.value == null).count());
        result.put("complete", true);
        if (trend) trend(points, result);
        else distribution(points, result);
        return result;
    }

    private static void distribution(List<Point> points, Map<String, Object> result) {
        List<Point> numeric = new ArrayList<>();
        for (Point point : points) if (point.value != null) numeric.add(point);
        numeric.sort((left, right) -> right.value.compareTo(left.value));
        List<Map<String, Object>> ranking = new ArrayList<>();
        int rank = 0;
        BigDecimal previous = null;
        for (int i = 0; i < numeric.size(); i++) {
            Point point = numeric.get(i);
            if (previous == null || previous.compareTo(point.value) != 0) rank = i + 1;
            Map<String, Object> row = point.toMap(); row.put("rank", rank); ranking.add(row); previous = point.value;
        }
        result.put("ranking", ranking);
        result.put("minimum", numeric.isEmpty() ? null : numeric.get(numeric.size() - 1).value.toPlainString());
        result.put("maximum", numeric.isEmpty() ? null : numeric.get(0).value.toPlainString());
        // 去重、均值、余额等分组结果不可直接相加；没有可加性契约时不计算占比/集中度。
        result.put("notice", "完整单维分组的描述性排名，NULL不参与排名。并列使用竞赛排名；不计算分组求和、占比或集中度，因为当前指标未声明跨维度可加性。不是全局异常或因果结论。");
    }

    private static void trend(List<Point> points, Map<String, Object> result) {
        points.sort(Comparator.comparing(point -> LocalDate.parse((String) point.category)));
        List<Map<String, Object>> changes = new ArrayList<>();
        int missingIntervals = 0;
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            Map<String, Object> row = point.toMap();
            if (i > 0) {
                Point previous = points.get(i - 1);
                boolean adjacent = LocalDate.parse((String) previous.category).plusDays(1).equals(LocalDate.parse((String) point.category));
                row.put("adjacentDay", adjacent);
                if (!adjacent) missingIntervals++;
                row.put("comparison", adjacent ? MetricPeriodComparison.calculate(text(point.value), text(previous.value)) : null);
            }
            changes.add(row);
        }
        result.put("points", changes); result.put("missingDateIntervals", missingIntervals);
        result.put("firstToLast", points.size() < 2 ? null : MetricPeriodComparison.calculate(
                text(points.get(points.size() - 1).value), text(points.get(0).value)));
        result.put("notice", "按已发布date维度排序；仅相邻自然日计算逐点变化，缺失日期不补0、不插值。首尾变化不等于持续趋势或显著性；不做预测、自动分桶、异常检测或因果归因。");
    }

    private static String text(BigDecimal number) { return number == null ? null : number.toPlainString(); }
    private static List<?> list(Object value) {
        if (!(value instanceof List)) throw new IllegalArgumentException("缺少结果列表");
        return (List<?>) value;
    }
    private static Map<?, ?> map(Object value) {
        if (!(value instanceof Map)) throw new IllegalArgumentException("结果结构无效");
        return (Map<?, ?>) value;
    }
    private static final class Point {
        private final Object category;
        private final BigDecimal value;
        private Point(Object category, BigDecimal value) { this.category = category; this.value = value; }
        private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>(); row.put("dimension", category); row.put("value", text(value)); return row;
        }
    }
}
