package com.datamaster.module.ontology.service.metric;

import com.datamaster.module.ontology.controller.admin.metric.vo.MetricSaveReqVO;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Two explicit aggregates on one registered source; reusable publication lifecycle, no free-form SQL. */
public final class MetricDerivedRules {
    private MetricDerivedRules() { }
    public static boolean active(MetricSaveReqVO metric) { return metric.getDerivedOperation() != null && !"NONE".equals(metric.getDerivedOperation()); }
    public static void validate(MetricSaveReqVO metric, boolean publishing) {
        String operation = metric.getDerivedOperation();
        if (operation == null || "NONE".equals(operation)) {
            require(metric.getSecondaryAggregation() == null && metric.getSecondaryValuePropertyId() == null
                    && metric.getPrimaryUnit() == null && metric.getSecondaryUnit() == null, "非派生指标不能残留第二聚合定义"); return;
        }
        require(Set.of("RATIO", "DIFFERENCE", "SUM").contains(operation), "正式派生仅支持比率、差值、相加");
        if (metric.getSecondaryAggregation() != null) require(Set.of("SUM", "COUNT", "COUNT_DISTINCT", "AVG", "MIN", "MAX").contains(metric.getSecondaryAggregation()), "第二聚合方式不合法");
        if (!publishing) return;
        require(metric.getAggregation() != null && Set.of("SUM", "COUNT", "COUNT_DISTINCT", "AVG", "MIN", "MAX").contains(metric.getAggregation()), "发布派生指标必须明确主聚合");
        require(metric.getSecondaryAggregation() != null, "发布派生指标必须明确第二聚合");
        require(MetricRules.notBlank(metric.getUnit()), "派生指标结果单位不能为空");
        require("COUNT".equals(metric.getSecondaryAggregation()) || metric.getSecondaryValuePropertyId() != null, "第二聚合必须明确属性（COUNT可留空）");
        require(MetricRules.notBlank(metric.getPrimaryUnit()) && MetricRules.notBlank(metric.getSecondaryUnit())
                && metric.getPrimaryUnit().length() <= 50 && metric.getSecondaryUnit().length() <= 50, "派生指标必须声明两项聚合单位（最多50字符）");
        if ("RATIO".equals(operation)) {
            String unit = metric.getPrimaryUnit().equals(metric.getSecondaryUnit()) ? "1" : metric.getPrimaryUnit() + "/" + metric.getSecondaryUnit();
            require(unit.equals(metric.getUnit()), "比率结果单位须为相同单位时的1，或明确的主单位/次单位；不自动转换为百分数");
        } else {
            require(metric.getPrimaryUnit().equals(metric.getSecondaryUnit()) && metric.getUnit().equals(metric.getPrimaryUnit()), "相加/差值要求主次及结果单位相同");
        }
        if ("SUM".equals(operation)) require(Set.of("SUM", "COUNT").contains(metric.getAggregation())
                && Objects.equals(metric.getAggregation(), metric.getSecondaryAggregation()), "相加仅支持同类SUM/COUNT，不直接累加均值或去重计数");
    }
    public static String expression(String primary, String secondary, String operation, boolean postgres) {
        if ("RATIO".equals(operation)) return "((" + primary + ") * " + (postgres ? "1.0" : "CAST(1 AS DECIMAL(38, 12))") + " / NULLIF((" + secondary + "), 0))";
        if ("DIFFERENCE".equals(operation)) return "((" + primary + ") - (" + secondary + "))";
        if ("SUM".equals(operation)) return "((" + primary + ") + (" + secondary + "))";
        throw new com.datamaster.common.exception.ServiceException("派生运算不合法");
    }
}
