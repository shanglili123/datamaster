package com.datamaster.module.ontology.service.metric;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import org.springframework.beans.BeanUtils;
import java.util.*;

/** Pure business rules; deliberately has no database/data-source execution dependencies. */
public final class MetricRules {
    private static final Set<String> AGGREGATIONS = new HashSet<>(Arrays.asList(
            "SUM", "COUNT", "COUNT_DISTINCT", "AVG", "MIN", "MAX"));
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
            "EQ", "NE", "GT", "GE", "LT", "LE", "IN", "IS_NULL", "IS_NOT_NULL"));
    private MetricRules() { }

    public static MetricSaveReqVO normalize(MetricSaveReqVO input) {
        MetricSaveReqVO vo = copyDefinition(input);
        require(vo.getOntologyId() != null && vo.getConceptId() != null, "本体和概念不能为空");
        require(vo.getCode() != null && vo.getCode().matches("[a-z][a-z0-9_]{0,99}"), "指标编码须为小写字母开头的小写字母、数字、下划线");
        require(notBlank(vo.getName()) && vo.getName().length() <= 200, "指标名称不能为空且不能超过200字符");
        require(notBlank(vo.getDescription()), "业务定义不能为空");
        if (notBlank(vo.getAggregation())) require(AGGREGATIONS.contains(vo.getAggregation()), "不支持的聚合方式");
        else vo.setAggregation(null);
        List<Long> dimensions = vo.getDimensionPropertyIds();
        require(!dimensions.contains(null), "维度属性不能为空");
        vo.setDimensionPropertyIds(new ArrayList<>(new LinkedHashSet<>(dimensions)));
        for (MetricFilterVO filter : vo.getFixedFilters()) {
            require(filter != null && filter.getPropertyId() != null, "过滤条件属性不能为空");
            require(OPERATORS.contains(filter.getOperator()), "不支持的过滤运算符");
            if ("IS_NULL".equals(filter.getOperator()) || "IS_NOT_NULL".equals(filter.getOperator())) {
                filter.setValue(null);
            } else if ("IN".equals(filter.getOperator())) {
                require(filter.getValue() instanceof Collection, "IN需要非空数组");
                Collection<?> values = (Collection<?>) filter.getValue();
                require(!values.isEmpty(), "IN需要非空数组");
                for (Object value : values) require(isScalar(value), "IN元素必须是非空标量");
            } else {
                require(isScalar(filter.getValue()), "过滤值必须是非空标量，不接受对象或数组");
            }
        }
        return vo;
    }

    public static void requirePublishFields(MetricSaveReqVO vo) {
        require(notBlank(vo.getOwner()), "发布前请填写负责人");
        require(notBlank(vo.getUnit()), "发布前请填写单位");
        require(notBlank(vo.getGrain()), "发布前请填写一行粒度说明");
        require(AGGREGATIONS.contains(vo.getAggregation()), "发布前请选择聚合方式");
        require(vo.getConceptTableId() != null, "发布前请绑定概念表");
        require("COUNT".equals(vo.getAggregation()) || vo.getValuePropertyId() != null, "该聚合方式必须指定度量属性");
    }

    public static void checkRevision(Integer expected, Integer actual) {
        require(expected != null && expected.equals(actual), "指标已被修改，请刷新后重试（revision不匹配）");
    }

    public static void checkIdentity(MetricDO current, MetricSaveReqVO update) {
        require(Objects.equals(current.getId(), update.getId())
                        && Objects.equals(current.getOntologyId(), update.getOntologyId())
                        && Objects.equals(current.getConceptId(), update.getConceptId())
                        && Objects.equals(current.getCode(), update.getCode()),
                "指标ID、本体、概念和编码创建后不可更改");
    }

    /** Independent VO copy excludes revision/id/status/audit fields and drops unknown client fields. */
    public static MetricSaveReqVO copyDefinition(MetricSaveReqVO input) {
        MetricSaveReqVO copy = new MetricSaveReqVO();
        BeanUtils.copyProperties(input, copy);
        copy.setId(null);
        copy.setRevision(null);
        copy.setDimensionPropertyIds(input.getDimensionPropertyIds() == null ? new ArrayList<>()
                : new ArrayList<>(input.getDimensionPropertyIds()));
        copy.setFixedFilters(input.getFixedFilters() == null ? new ArrayList<>()
                : JSON.parseArray(JSON.toJSONString(input.getFixedFilters()), MetricFilterVO.class));
        return copy;
    }

    public static String definitionJson(MetricSaveReqVO vo) {
        return JSON.toJSONString(copyDefinition(vo));
    }

    public static MetricRespVO snapshot(MetricRespVO draft, Map<String, Object> source, int version, int revision) {
        MetricRespVO snapshot = new MetricRespVO();
        BeanUtils.copyProperties(copyDefinition(draft), snapshot);
        snapshot.setId(draft.getId());
        snapshot.setRevision(revision);
        snapshot.setStatus("PUBLISHED");
        snapshot.setPublishedVersion(version);
        snapshot.setDraftChanged(false);
        snapshot.setSourceSnapshot(JSON.parseObject(JSON.toJSONString(source,
                com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue)));
        return snapshot;
    }

    public static boolean samePublishedDefinition(MetricSaveReqVO draft, Map<String, Object> source, MetricRespVO published) {
        return definitionJson(draft).equals(definitionJson(published))
                && Objects.equals(JSON.parseObject(JSON.toJSONString(source)),
                        JSON.parseObject(JSON.toJSONString(published.getSourceSnapshot())));
    }

    public static void validateTypedFilter(MetricFilterVO filter, String dataType) {
        if ("IS_NULL".equals(filter.getOperator()) || "IS_NOT_NULL".equals(filter.getOperator())) return;
        Collection<?> values = "IN".equals(filter.getOperator())
                ? (Collection<?>) filter.getValue() : Collections.singletonList(filter.getValue());
        if (numeric(dataType)) {
            for (Object value : values) {
                require(value instanceof Number || value instanceof String, "数值属性过滤值必须为数值");
                try {
                    new java.math.BigDecimal(String.valueOf(value));
                } catch (NumberFormatException ex) {
                    throw new ServiceException("数值属性过滤值格式不正确");
                }
            }
        } else if ("boolean".equalsIgnoreCase(dataType)) {
            for (Object value : values) require(value instanceof Boolean
                    || "true".equals(value) || "false".equals(value), "布尔属性过滤值必须为true/false");
        } else if (temporal(dataType)) {
            for (Object value : values) {
                require(value instanceof String, "日期属性过滤值必须为ISO日期/时间字符串");
                try {
                    String text = (String) value;
                    if ("date".equalsIgnoreCase(dataType)) java.time.LocalDate.parse(text);
                    else java.time.LocalDateTime.parse(text.replace(' ', 'T'));
                } catch (java.time.format.DateTimeParseException ex) {
                    throw new ServiceException("日期属性过滤值格式不正确");
                }
            }
        }
    }
    public static boolean numeric(String type) {
        return type != null && Arrays.asList("integer", "decimal", "number", "long", "int", "double", "float", "numeric", "bigint", "smallint").contains(type.toLowerCase(Locale.ROOT));
    }
    public static boolean temporal(String type) {
        return type != null && Arrays.asList("date", "datetime", "timestamp").contains(type.toLowerCase(Locale.ROOT));
    }
    public static boolean adsSource(String database, String table) {
        return notBlank(table) && (table.toLowerCase(Locale.ROOT).startsWith("ads_")
                || database != null && database.equalsIgnoreCase("ads"));
    }
    public static Set<Long> usedProperties(MetricSaveReqVO vo) {
        Set<Long> ids = new LinkedHashSet<>(vo.getDimensionPropertyIds());
        if (vo.getValuePropertyId() != null) ids.add(vo.getValuePropertyId());
        if (vo.getTimePropertyId() != null) ids.add(vo.getTimePropertyId());
        for (MetricFilterVO filter : vo.getFixedFilters()) ids.add(filter.getPropertyId());
        return ids;
    }
    public static boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
    private static boolean isScalar(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }
    public static void require(boolean condition, String message) {
        if (!condition) throw new ServiceException(message);
    }
}
