package com.datamaster.module.ai.agent.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** 精确十进制比较；无数据不补零，基期为零或负数时不定义增长率。 */
public final class MetricPeriodComparison {
    private MetricPeriodComparison() { }

    public static boolean validateRanges(String type, String currentStart, String currentEnd,
                                         String baselineStart, String baselineEnd) {
        LocalDateTime cs = parse(type, currentStart), ce = parse(type, currentEnd);
        LocalDateTime bs = parse(type, baselineStart), be = parse(type, baselineEnd);
        if (!cs.isBefore(ce) || !bs.isBefore(be)) throw new IllegalArgumentException("时间段须开始早于结束");
        if (cs.isBefore(be) && bs.isBefore(ce)) throw new IllegalArgumentException("本期与基期不能重叠");
        return Duration.between(cs, ce).equals(Duration.between(bs, be));
    }

    private static LocalDateTime parse(String type, String value) {
        if (value == null || value.length() > 100) throw new IllegalArgumentException("必须明确提供两个时间段");
        if ("date".equalsIgnoreCase(type)) return LocalDate.parse(value).atStartOfDay();
        if ("datetime".equalsIgnoreCase(type) || "timestamp".equalsIgnoreCase(type)) return LocalDateTime.parse(value);
        throw new IllegalArgumentException("需要已发布的时间属性");
    }

    public static Map<String, Object> calculate(String currentText, String baselineText) {
        BigDecimal current = number(currentText), baseline = number(baselineText);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentValue", current == null ? null : current.toPlainString());
        result.put("baselineValue", baseline == null ? null : baseline.toPlainString());
        result.put("difference", null); result.put("growthRatePercent", null);
        result.put("growthRateFormula", "(currentValue - baselineValue) / baselineValue * 100");
        result.put("growthRateScale", 6); result.put("roundingMode", "HALF_UP");
        if (current == null || baseline == null) {
            result.put("status", "NO_DATA");
            return result;
        }
        BigDecimal difference = current.subtract(baseline);
        result.put("difference", difference.toPlainString());
        result.put("direction", difference.signum() > 0 ? "UP" : difference.signum() < 0 ? "DOWN" : "UNCHANGED");
        if (baseline.signum() <= 0) {
            result.put("status", baseline.signum() == 0 ? "ZERO_BASELINE" : "NEGATIVE_BASELINE");
            return result;
        }
        result.put("growthRatePercent", difference.multiply(new BigDecimal("100"))
                .divide(baseline, 6, RoundingMode.HALF_UP).toPlainString());
        result.put("status", "OK");
        return result;
    }

    private static BigDecimal number(String text) {
        if (text == null) return null;
        if (text.length() > 2000) throw new IllegalArgumentException("数值长度超限");
        BigDecimal value = new BigDecimal(text);
        if (value.precision() > 1000 || Math.abs((long) value.scale()) > 1000) throw new IllegalArgumentException("数值超限");
        return value;
    }
}
