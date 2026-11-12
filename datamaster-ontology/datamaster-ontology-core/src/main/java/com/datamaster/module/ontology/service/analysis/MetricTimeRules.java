package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.metric.vo.MetricSaveReqVO;
import java.time.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Published fixed-offset contract. Never guess source timezone or silently apply DST rules. */
public final class MetricTimeRules {
    private MetricTimeRules() { }
    public static void validate(MetricSaveReqVO metric) {
        require(metric != null, "指标时间定义不能为空");
        String source = metric.getSourceTimeOffset(), business = metric.getBusinessTimeOffset();
        require((source == null) == (business == null), "来源与业务时间偏移必须同时配置或同时留空");
        if (source != null) { offset(source); offset(business); require(metric.getTimePropertyId() != null, "配置时间偏移必须明确时间属性"); }
    }
    public static ZoneOffset offset(String text) {
        require(text != null && text.matches("[+-](?:0[0-9]|1[0-4]):[0-5][0-9]"), "时间偏移须为±HH:mm，范围±14:00；区域夏令时请勿用固定偏移替代");
        try {
            ZoneOffset value = ZoneOffset.of(text);
            require(Math.abs(value.getTotalSeconds()) <= 14 * 3600 && !"-00:00".equals(text), "时间偏移超出±14:00或使用不明确的-00:00");
            return value;
        } catch (DateTimeException error) { throw new com.datamaster.common.exception.ServiceException("时间偏移格式不合法"); }
    }
    public static int businessShiftMinutes(MetricSaveReqVO metric) {
        validate(metric);
        return metric.getSourceTimeOffset() == null ? 0 : (offset(metric.getBusinessTimeOffset()).getTotalSeconds() - offset(metric.getSourceTimeOffset()).getTotalSeconds()) / 60;
    }
    public static LocalDateTime toSource(LocalDateTime businessTime, MetricSaveReqVO metric) {
        validate(metric);
        try {
            LocalDateTime source = businessTime.minusMinutes(businessShiftMinutes(metric));
            require(source.getYear() >= 1 && source.getYear() <= 9999, "来源时间偏移后超出0001至9999年");
            return source;
        }
        catch (DateTimeException | ArithmeticException invalid) { throw new com.datamaster.common.exception.ServiceException("时间偏移转换超出有效日期范围"); }
    }
    public static String description(MetricSaveReqVO metric) {
        return metric.getSourceTimeOffset() == null ? "沿用来源业务本地时间（未声明偏移，不推断时区）"
                : "来源UTC" + metric.getSourceTimeOffset() + " → 业务UTC" + metric.getBusinessTimeOffset() + "（固定偏移，无夏令时自动转换）";
    }
}
