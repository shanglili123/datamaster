package com.datamaster.module.ontology.service.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Compare two complete period result sets; never add truncated groups, AVG or distinct counts. */
public final class MetricComparisonRules {
    private MetricComparisonRules() { }
    public static MetricQueryReqVO baseline(MetricComparisonReqVO request) {
        require(request != null, "比较请求不能为空");
        MetricQueryReqVO current = MetricQueryBuilder.normalize(request.getQuery());
        require(current.getPageNum() == 1, "两期比较必须从第一页查询完整分组");
        boolean chronological = !"NONE".equals(current.getTimeBucket()) && "time_bucket".equals(current.getSortBy()) && "ASC".equals(current.getSortDirection());
        require((current.getDimensionPropertyIds().isEmpty() && "NONE".equals(current.getTimeBucket())) || current.getSortBy() == null || chronological, "分组/时间桶比较不得按独立TopN排序，请使用默认或时间桶升序核验完整分组");
        require(current.getStartTime() != null && current.getEndTime() != null, "两期比较必须明确当前时间区间");
        String mode = request.getMode();
        require(mode != null && java.util.Set.of("PREVIOUS_PERIOD", "PREVIOUS_MONTH", "PREVIOUS_YEAR", "CUSTOM").contains(mode), "比较方式不合法");
        MetricQueryReqVO baseline = JSON.parseObject(JSON.toJSONString(current,
                com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue), MetricQueryReqVO.class);
        boolean dateOnly = current.getStartTime().length() == 10 && current.getEndTime().length() == 10;
        require((current.getStartTime().length() == 10) == (current.getEndTime().length() == 10), "两期比较起止须采用一致日期或日期时间格式");
        try {
            LocalDateTime start = parse(current.getStartTime(), dateOnly), end = parse(current.getEndTime(), dateOnly);
            require(start.isBefore(end), "开始时间必须小于结束时间");
            LocalDateTime beforeStart, beforeEnd;
            if ("CUSTOM".equals(mode)) {
                beforeStart = parse(request.getBaselineStartTime(), dateOnly); beforeEnd = parse(request.getBaselineEndTime(), dateOnly);
            } else if ("PREVIOUS_YEAR".equals(mode)) { beforeStart = start.minusYears(1); beforeEnd = end.minusYears(1); }
            else if ("PREVIOUS_MONTH".equals(mode)) { beforeStart = start.minusMonths(1); beforeEnd = end.minusMonths(1); }
            else { beforeEnd = start; beforeStart = start.minus(Duration.between(start, end)); }
            require(beforeStart.getYear() >= 1 && beforeStart.getYear() <= 9999
                    && beforeEnd.getYear() >= 1 && beforeEnd.getYear() <= 9999, "比较区间平移后超出0001至9999年，请明确自定义区间");
            require(beforeStart.isBefore(beforeEnd), "比较区间为空或日期平移后不合法，请明确自定义区间");
            baseline.setStartTime(format(beforeStart, dateOnly)); baseline.setEndTime(format(beforeEnd, dateOnly));
            MetricPeriodBucketRules.validate(current, baseline);
            return baseline;
        } catch (java.time.DateTimeException | NullPointerException error) {
            throw new com.datamaster.common.exception.ServiceException("比较区间必须采用一致的业务本地日期或日期时间格式");
        }
    }
    private static LocalDateTime parse(String value, boolean dateOnly) {
        require(value != null && value.matches(dateOnly ? "[0-9]{4}-[0-9]{2}-[0-9]{2}"
                : "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}(:[0-9]{2}(\\.[0-9]{1,9})?)?"), "比较区间须为严格ISO业务本地日期或时间");
        LocalDateTime result = dateOnly ? LocalDate.parse(value).atStartOfDay() : LocalDateTime.parse(value);
        require(result.getYear() >= 1 && result.getYear() <= 9999, "比较日期仅支持0001至9999年");
        return result;
    }
    private static String format(LocalDateTime value, boolean dateOnly) { return dateOnly ? value.toLocalDate().toString() : value.toString(); }
    /** Dimension filters apply unchanged to both periods; time filters must not pin the baseline to current dates. */
    public static void validateFilters(MetricRespVO publication, MetricQueryReqVO query) {
        for (com.datamaster.module.ontology.controller.admin.metric.vo.MetricFilterVO filter : query.getFilters()) {
            require(!Objects.equals(publication.getTimePropertyId(), filter.getPropertyId()), "两期比较请仅使用起止区间约束时间，不得附加时间属性过滤；其他维度过滤可保留");
        }
    }
    public static void calculate(MetricComparisonRespVO result) {
        require(result != null, "两期比较结果不能为空");
        Map<String,Map<String,Object>> aligned = new LinkedHashMap<>();
        List<MetricQueryColumnVO> dimensions = new ArrayList<>();
        MetricQueryRespVO current = result.getCurrent(), baseline = result.getBaseline();
        require(current != null && baseline != null && Boolean.FALSE.equals(current.getTruncated())
                && Boolean.FALSE.equals(baseline.getTruncated()), "两期比较存在截断，禁止以不完整分组计算变化，请缩小范围或提高上限");
        require(current.getColumns() != null && baseline.getColumns() != null && current.getRows() != null && baseline.getRows() != null, "两期返回结构不可用");
        for (MetricQueryColumnVO column : current.getColumns()) if (!"metric_value".equals(column.getKey())) dimensions.add(column);
        List<String> beforeKeys = new ArrayList<>(), afterKeys = new ArrayList<>();
        for (MetricQueryColumnVO column : current.getColumns()) afterKeys.add(column.getKey());
        for (MetricQueryColumnVO column : baseline.getColumns()) beforeKeys.add(column.getKey());
        require(afterKeys.equals(beforeKeys) && afterKeys.contains("metric_value"), "两期分组返回字段不一致或缺少指标值");
        for (int i = 0; i < current.getColumns().size(); i++) require(Objects.equals(current.getColumns().get(i).getDataType(), baseline.getColumns().get(i).getDataType()), "两期返回维度或度量类型不一致");
        result.setGrouped(!dimensions.isEmpty());
        addPeriod(aligned, dimensions, current.getRows(), "current_value");
        addPeriod(aligned, dimensions, baseline.getRows(), "baseline_value");
        for (Map<String,Object> row : aligned.values()) {
            BigDecimal now = decimal(row.get("current_value")), before = decimal(row.get("baseline_value"));
            row.put("difference", now == null || before == null ? null : now.subtract(before).toPlainString());
            row.put("change_percent", now == null || before == null || before.signum() == 0 ? null
                    : now.subtract(before).multiply(new BigDecimal("100")).divide(before.abs(), 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        }
        List<MetricQueryColumnVO> columns = new ArrayList<>(dimensions);
        columns.add(column("current_value", "本期值", "decimal")); columns.add(column("baseline_value", "基期值", "decimal"));
        columns.add(column("difference", "差值（本期−基期）", "decimal")); columns.add(column("change_percent", "变化率（%）", "decimal"));
        List<Map<String,Object>> orderedRows = new ArrayList<>(aligned.values());
        orderedRows.sort(MetricGroupIdentity.ascending(dimensions));
        result.setColumns(columns); result.setRows(orderedRows);
        result.setDifference(null); result.setChangePercent(null);
        if (!result.isGrouped()) {
            require(result.getRows().size() == 1, "两期总量返回结构异常");
            result.setDifference((String) result.getRows().get(0).get("difference"));
            result.setChangePercent((String) result.getRows().get(0).get("change_percent"));
        }
    }
    private static void addPeriod(Map<String,Map<String,Object>> aligned, List<MetricQueryColumnVO> dimensions, List<Map<String,Object>> rows, String period) {
        Set<String> seen = new HashSet<>();
        for (Map<String,Object> row : rows) {
            require(row != null && row.containsKey("metric_value"), "两期指标值字段缺失");
            String identity = MetricGroupIdentity.of(dimensions, row); require(seen.add(identity), "两期返回重复分组，禁止覆盖原结果");
            Map<String,Object> target = aligned.computeIfAbsent(identity, ignored -> {
                Map<String,Object> value = new LinkedHashMap<>();
                for (MetricQueryColumnVO dimension : dimensions) value.put(dimension.getKey(), row.get(dimension.getKey()));
                return value;
            });
            target.put(period, row.get("metric_value"));
        }
    }
    private static BigDecimal decimal(Object value) {
        if (value == null) return null;
        try {
            BigDecimal number = new BigDecimal(String.valueOf(value));
            require(number.precision() <= 1000 && Math.abs((long) number.scale()) <= 1000, "比较数值超出安全计算范围");
            return number;
        }
        catch (NumberFormatException error) { throw new com.datamaster.common.exception.ServiceException("比较指标必须返回数值"); }
    }
    private static MetricQueryColumnVO column(String key, String label, String type) {
        MetricQueryColumnVO column = new MetricQueryColumnVO(); column.setKey(key); column.setLabel(label); column.setDataType(type); return column;
    }
}
