package com.datamaster.module.ontology.service.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import java.math.*;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Align complete bounded groups only. Never join sources or derive totals from truncated groups. */
public final class MetricMultiQueryRules {
    private MetricMultiQueryRules() { }
    public static void comparable(List<MetricRespVO> metrics, List<MetricQueryReqVO> queries, String operation) {
        require(metrics.size() >= 2 && metrics.size() <= 4 && metrics.size() == queries.size(), "请选择2至4个指标");
        require(operation != null && Set.of("NONE", "RATIO", "DIFFERENCE", "SUM").contains(operation), "派生运算不合法");
        if (!"NONE".equals(operation)) require(metrics.size() == 2, "派生运算必须恰好选择两个指标");
        Set<String> metricIdentities = new HashSet<>();
        for (MetricQueryReqVO item : queries) require(metricIdentities.add(item.getMetricId() + ":" + item.getVersion()), "不能重复选择同一指标版本");
        MetricRespVO first = metrics.get(0); MetricQueryReqVO query = queries.get(0);
        for (int i = 0; i < metrics.size(); i++) {
            MetricRespVO metric = metrics.get(i); MetricQueryReqVO item = queries.get(i);
            require(Objects.equals(first.getOntologyId(), metric.getOntologyId()) && Objects.equals(first.getConceptId(), metric.getConceptId())
                    && Objects.equals(first.getConceptTableId(), metric.getConceptTableId()) && Objects.equals(first.getGrain(), metric.getGrain())
                    && Objects.equals(first.getTimePropertyId(), metric.getTimePropertyId())
                    && Objects.equals(first.getSourceTimeOffset(), metric.getSourceTimeOffset()) && Objects.equals(first.getBusinessTimeOffset(), metric.getBusinessTimeOffset())
                    && sameSource(first.getSourceSnapshot(), metric.getSourceSnapshot()),
                    "多指标首版要求同本体、概念、绑定、粒度、时间属性和来源身份");
            require(sharedMappingsAgree(first.getSourceSnapshot(), metric.getSourceSnapshot()), "多指标共享属性的发布映射不一致，请重新核验版本");
            require(Objects.equals(query.getDimensionPropertyIds(), item.getDimensionPropertyIds()) && Objects.equals(query.getTimeBucket(), item.getTimeBucket())
                    && Objects.equals(query.getStartTime(), item.getStartTime()) && Objects.equals(query.getEndTime(), item.getEndTime())
                    && Objects.equals(JSON.toJSONString(query.getFilters()), JSON.toJSONString(item.getFilters())), "多指标分组、时间区间和用户筛选必须一致");
            boolean chronological = !"NONE".equals(item.getTimeBucket()) && "time_bucket".equals(item.getSortBy()) && "ASC".equals(item.getSortDirection());
            require(item.getPageNum() == 1 && (item.getSortBy() == null || chronological), "多指标须使用默认或时间桶升序的第一页核验完整分组，不拼接独立TopN");
        }
        // Comparing only to the first publication misses shared fields used exclusively by later metrics.
        for (int left = 0; left < metrics.size(); left++) for (int right = left + 1; right < metrics.size(); right++) {
            require(sharedMappingsAgree(metrics.get(left).getSourceSnapshot(), metrics.get(right).getSourceSnapshot()), "多指标共享属性的发布映射不一致，请重新核验全部版本");
        }
        if ("RATIO".equals(operation)) require(unitPresent(first.getUnit()) && unitPresent(metrics.get(1).getUnit()), "比率必须明确两项单位");
        if ("SUM".equals(operation) || "DIFFERENCE".equals(operation)) require(unitPresent(first.getUnit()) && Objects.equals(first.getUnit(), metrics.get(1).getUnit()), "相加/差值要求单位明确且一致");
        if ("SUM".equals(operation)) require(!com.datamaster.module.ontology.service.metric.MetricDerivedRules.active(first)
                && !com.datamaster.module.ontology.service.metric.MetricDerivedRules.active(metrics.get(1)), "正式派生结果不自动视作可加指标，请选择并列、比率或差值");
        if ("SUM".equals(operation)) require(first.getAggregation() != null && Set.of("SUM", "COUNT").contains(first.getAggregation())
                && Objects.equals(first.getAggregation(), metrics.get(1).getAggregation()), "相加首版仅支持同类SUM/COUNT，不能把均值或去重计数直接相加");
    }
    public static MetricMultiQueryRespVO align(List<MetricQueryRespVO> results, String operation) {
        require(results != null && results.size() >= 2 && results.size() <= 4 && results.get(0) != null
                && results.get(0).getColumns() != null, "多指标结果结构不可用");
        require(operation != null && Set.of("NONE", "RATIO", "DIFFERENCE", "SUM").contains(operation), "派生运算不合法");
        require("NONE".equals(operation) || results.size() == 2, "二元派生要求两个指标结果");
        MetricMultiQueryRespVO output = new MetricMultiQueryRespVO(); output.setMetrics(results); output.setDerivedOperation(operation);
        List<MetricQueryColumnVO> columns = new ArrayList<>(); List<MetricQueryColumnVO> dimensions = new ArrayList<>();
        for (MetricQueryColumnVO column : results.get(0).getColumns()) if (!"metric_value".equals(column.getKey())) { dimensions.add(column); columns.add(column); }
        Map<String,Map<String,Object>> rows = new TreeMap<>();
        for (int i = 0; i < results.size(); i++) {
            MetricQueryRespVO result = results.get(i); require(result != null && Boolean.FALSE.equals(result.getTruncated()), "多指标结果存在截断，禁止把不完整分组合并；请缩小范围或提高上限");
            String measureType = measureType(result);
            columns.add(column("m_" + i, result.getMetricName() + "（" + result.getUnit() + "）", measureType));
            require(result.getRows() != null && result.getColumns() != null, "多指标结果结构不可用");
            List<String> expectedGroups = new ArrayList<>(), actualGroups = new ArrayList<>();
            require(result.getColumns().stream().anyMatch(item -> "metric_value".equals(item.getKey())), "多指标结果缺少指标值");
            for (MetricQueryColumnVO dimension : dimensions) expectedGroups.add(dimension.getKey());
            for (MetricQueryColumnVO column : result.getColumns()) if (!"metric_value".equals(column.getKey())) actualGroups.add(column.getKey());
            require(expectedGroups.equals(actualGroups), "多指标返回分组结构不一致");
            List<MetricQueryColumnVO> actualDimensions = new ArrayList<>();
            for (MetricQueryColumnVO column : result.getColumns()) if (!"metric_value".equals(column.getKey())) actualDimensions.add(column);
            for (int index = 0; index < dimensions.size(); index++) require(Objects.equals(dimensions.get(index).getDataType(), actualDimensions.get(index).getDataType()), "多指标分组类型不一致");
            Set<String> seen = new HashSet<>();
            for (Map<String,Object> row : result.getRows()) {
                require(row != null && row.containsKey("metric_value"), "多指标值字段缺失");
                String identity = MetricGroupIdentity.of(dimensions, row);
                require(seen.add(identity), "多指标返回重复分组，禁止覆盖");
                Map<String,Object> aligned = rows.computeIfAbsent(identity, key -> { Map<String,Object> value = new LinkedHashMap<>(); for (MetricQueryColumnVO dimension : dimensions) value.put(dimension.getKey(), row.get(dimension.getKey())); return value; });
                aligned.put("m_" + i, row.get("metric_value"));
            }
        }
        if (!"NONE".equals(operation)) {
            String unit = "RATIO".equals(operation) ? (Objects.equals(results.get(0).getUnit(), results.get(1).getUnit()) ? "1" : results.get(0).getUnit() + "/" + results.get(1).getUnit()) : results.get(0).getUnit();
            output.setDerivedUnit(unit); columns.add(column("derived_value", operation + "（" + unit + "）"));
            for (Map<String,Object> row : rows.values()) row.put("derived_value", derive(row.get("m_0"), row.get("m_1"), operation));
        }
        output.setColumns(columns); output.setRows(new ArrayList<>(rows.values()));
        output.setNotice("同源同粒度、明确发布版本分别查询后按完整分组对齐；缺失不补零，不把分组去重之和当总去重，不跨桶再聚合。派生为当前分析临时白名单运算，不创建正式派生指标；比率分母零返回缺失。来源两次查询不冻结底层数据。");
        return output;
    }
    private static boolean unitPresent(String unit) { return unit != null && !unit.isBlank(); }
    private static boolean sameSource(Map<String,Object> a, Map<String,Object> b) {
        if (a == null || b == null) return false;
        for (String key : List.of("datasourceId", "datasourceType", "databaseName", "schemaName", "tableName", "sourceMode")) if (!Objects.equals(a.get(key), b.get(key))) return false;
        return true;
    }
    private static boolean sharedMappingsAgree(Map<String,Object> a, Map<String,Object> b) {
        if (!(a.get("propertyColumns") instanceof Map) || !(b.get("propertyColumns") instanceof Map)) return false;
        Map<?,?> left = (Map<?,?>) a.get("propertyColumns"), right = (Map<?,?>) b.get("propertyColumns");
        for (Object key : left.keySet()) if (right.containsKey(key) && !Objects.equals(left.get(key), right.get(key))) return false;
        return true;
    }
    private static String derive(Object left, Object right, String operation) {
        if (left == null || right == null) return null;
        BigDecimal a, b;
        try {
            a = new BigDecimal(String.valueOf(left)); b = new BigDecimal(String.valueOf(right));
            require(a.precision() <= 1000 && b.precision() <= 1000 && Math.abs((long) a.scale()) <= 1000 && Math.abs((long) b.scale()) <= 1000, "派生数值超出安全计算范围");
        }
        catch (NumberFormatException error) { throw new com.datamaster.common.exception.ServiceException("派生运算只接受数值指标"); }
        if ("SUM".equals(operation)) return a.add(b).toPlainString();
        if ("DIFFERENCE".equals(operation)) return a.subtract(b).toPlainString();
        return b.signum() == 0 ? null : a.divide(b, 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
    private static String measureType(MetricQueryRespVO result) {
        require(result.getColumns() != null, "多指标结果字段不可用");
        for (MetricQueryColumnVO column : result.getColumns()) if (column != null && "metric_value".equals(column.getKey())) return column.getDataType();
        throw new com.datamaster.common.exception.ServiceException("多指标结果缺少指标值字段");
    }
    private static MetricQueryColumnVO column(String key, String label) { return column(key, label, "decimal"); }
    private static MetricQueryColumnVO column(String key, String label, String type) { MetricQueryColumnVO column = new MetricQueryColumnVO(); column.setKey(key); column.setLabel(label); column.setDataType(type); return column; }
}
