package com.datamaster.module.ai.agent.analysis;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class MetricPeriodComparisonTest {
    @Test
    public void calculatesDecimalsWithoutFloatingPointLoss() {
        Map<String, Object> result = MetricPeriodComparison.calculate("9007199254740993.01", "9007199254740992.00");
        assertEquals("1.01", result.get("difference"));
        assertEquals("9007199254740993.01", result.get("currentValue"));
        assertEquals("OK", result.get("status"));
        assertEquals("25.000000", MetricPeriodComparison.calculate("125", "100").get("growthRatePercent"));
        assertEquals("-33.333333", MetricPeriodComparison.calculate("2", "3").get("growthRatePercent"));
    }

    @Test
    public void doesNotInventRatesForUndefinedBaselinesOrMissingData() {
        assertEquals("ZERO_BASELINE", MetricPeriodComparison.calculate("10", "0").get("status"));
        assertNull(MetricPeriodComparison.calculate("0", "0").get("growthRatePercent"));
        assertEquals("NEGATIVE_BASELINE", MetricPeriodComparison.calculate("5", "-10").get("status"));
        assertEquals("NO_DATA", MetricPeriodComparison.calculate(null, "100").get("status"));
        assertNull(MetricPeriodComparison.calculate(null, "100").get("difference"));
    }

    @Test
    public void validatesHalfOpenRangesAndReportsUnequalDurations() {
        assertTrue(MetricPeriodComparison.validateRanges("date", "2026-10-08", "2026-10-15", "2026-10-01", "2026-10-08"));
        assertFalse(MetricPeriodComparison.validateRanges("date", "2026-10-01", "2026-11-01", "2026-09-01", "2026-10-01"));
        assertTrue(MetricPeriodComparison.validateRanges("datetime", "2026-10-02T00:00:00", "2026-10-02T12:00:00", "2026-10-01T00:00:00", "2026-10-01T12:00:00"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOverlappingPeriods() {
        MetricPeriodComparison.validateRanges("date", "2026-10-01", "2026-10-15", "2026-10-08", "2026-10-22");
    }

    @Test(expected = java.time.format.DateTimeParseException.class)
    public void rejectsInvalidDates() {
        MetricPeriodComparison.validateRanges("date", "2026-02-30", "2026-03-01", "2026-01-01", "2026-02-01");
    }
}
