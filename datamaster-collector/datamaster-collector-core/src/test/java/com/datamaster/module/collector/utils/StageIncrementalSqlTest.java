package com.datamaster.module.collector.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class StageIncrementalSqlTest {

    @Test
    public void comparesTypedWatermarksWithoutLexicalOrdering() {
        assertEquals(1, Integer.signum(StageIncrementalSql.compare("100", "99", "ID")));
        assertEquals(0, StageIncrementalSql.compare("1.00", "1", "ID"));
        assertEquals(-1, Integer.signum(StageIncrementalSql.compare("2026-01-01 00:00:00.1", "2026-01-01 00:00:00.2", "TIME")));
    }

    @Test
    public void idUsesInclusiveStartAndEndAndCanonicalNumbers() {
        assertEquals("(`s`.`RecordId` >= 100 AND `s`.`RecordId` <= 201.5)",
                StageIncrementalSql.predicate("RecordId", "ID", "1E+2", "201.500", "MySql", "s"));
        assertEquals("(\"id\" >= -2.5 AND \"id\" <= 0)",
                StageIncrementalSql.predicate("\"id\"", "ID", "-2.50", "-0.00"));
    }

    @Test
    public void timeAcceptsDateSpaceAndIsoAndPreservesNanos() {
        assertEquals("(`ChangedAt` >= '2026-02-28 00:00:00' AND `ChangedAt` <= '2026-03-01 12:13:14.123456789')",
                StageIncrementalSql.predicate("ChangedAt", "TIME", "2026-02-28",
                        "2026-03-01T12:13:14.123456789", "MYSQL", null));
        assertEquals("(\"t\".\"ChangedAt\" <= '2024-02-29 12:13:14.12')",
                StageIncrementalSql.predicate("\"t\".\"ChangedAt\"", "TIME", null,
                        "2024-02-29 12:13:14.120"));
    }

    @Test
    public void nullEndClosesWindowAndNullStartAllowsUpperBoundOnly() {
        assertEquals("1=0", StageIncrementalSql.predicate("`id`", "ID", "1", null));
        assertEquals("1=0", StageIncrementalSql.predicate("`id`", "ID", null, null));
        assertEquals("([id] <= 10)",
                StageIncrementalSql.predicate("id", "ID", null, "10", "SQLServer", null));
    }

    @Test
    public void dialectQuotesPreservePhysicalCase() {
        assertEquals("(\"src\".\"MixedCase\" >= 1 AND \"src\".\"MixedCase\" <= 2)",
                StageIncrementalSql.predicate("MixedCase", "ID", "1", "2", "PostgreSQL", "src"));
        for (String db : new String[]{"Oracle11", "Oracle", "DM8", "Kingbase8", "DB2", "H2"}) {
            assertEquals("(\"MixedCase\" <= 2)",
                    StageIncrementalSql.predicate("MixedCase", "ID", null, "2", db, null));
        }
        assertEquals("([s].[Order] >= 1 AND [s].[Order] <= 2)",
                StageIncrementalSql.predicate("Order", "ID", "1", "2", "SQLServer", "s"));
    }

    @Test
    public void timeUsesExplicitDialectConversionsWhereRequired() {
        assertEquals("(\"Time\" <= TIMESTAMP '2026-01-01 00:00:00')",
                StageIncrementalSql.predicate("Time", "TIME", null, "2026-01-01", "Kingbase8", null));
        for (String db : new String[]{"Oracle", "Oracle11", "DM8"}) {
            assertEquals("(\"Time\" <= TO_TIMESTAMP('2026-01-01 00:00:00.0', 'YYYY-MM-DD HH24:MI:SS.FF9'))",
                    StageIncrementalSql.predicate("Time", "TIME", null, "2026-01-01", db, null));
        }
        assertEquals("([Time] <= CAST('2026-01-01T00:00:00' AS datetime2))",
                StageIncrementalSql.predicate("Time", "TIME", null, "2026-01-01", "SQLServer", null));
    }

    @Test
    public void rejectsInvalidDatesAndTimeInjection() {
        for (String value : new String[]{"2026-02-29", "2026-13-01", "2026-01-01 24:00:00",
                "2026-01-01 01:02", "2026-01-01T01:02:03Z", "2026-01-01 01:02:03.1234567890",
                "2026-01-01' OR 1=1 --", "0000-01-01", " 2026-01-01", ""}) {
            invalid(() -> StageIncrementalSql.predicate("`time`", "TIME", null, value));
            invalid(() -> StageIncrementalSql.predicate("`time`", "TIME", value, null));
        }
    }

    @Test
    public void rejectsInvalidNumbersAndTypes() {
        for (String value : new String[]{"NaN", "Infinity", "-Infinity", "1; DROP TABLE t", "1 OR 1=1", "", " 1"}) {
            invalid(() -> StageIncrementalSql.predicate("`id`", "ID", null, value));
        }
        for (String type : new String[]{null, "", "NUMBER", "TIME;", " ID"}) {
            invalid(() -> StageIncrementalSql.predicate("`id`", type, null, null));
        }
    }

    @Test
    public void rejectsExpressionsCommentsAndUnquotedCompatibilityColumns() {
        for (String column : new String[]{null, "id", "t.id", "`id`;", "`id` --", "`id`/*x*/",
                "`id` + 1", "sum(`id`)", "\"a\".`id`", "[id]]", "`a`.`b`.`c`"}) {
            invalid(() -> StageIncrementalSql.predicate(column, "ID", "1", "2"));
        }
        for (String identifier : new String[]{null, "", "id;", "a.b", "id --", "a/*b*/", "id + 1", "`id`"}) {
            invalid(() -> StageIncrementalSql.predicate(identifier, "ID", "1", "2", "MYSQL", null));
        }
        invalid(() -> StageIncrementalSql.predicate("id", "ID", "1", "2", "MYSQL", "s JOIN t"));
        invalid(() -> StageIncrementalSql.predicate("id", "ID", "1", "2", "MYSQL", ""));
        invalid(() -> StageIncrementalSql.predicate("id", "ID", "1", "2", "unknown", null));
        invalid(() -> StageIncrementalSql.predicate("id", "ID", "1", "2", null, null));
    }

    @Test
    public void fragmentCanBeUsedInJoinWithoutWhere() {
        String fragment = StageIncrementalSql.predicate("id", "ID", "1", "2", "MYSQL", "s");
        assertFalse(fragment.contains("WHERE"));
        assertEquals("JOIN source s ON s.key = t.key AND (`s`.`id` >= 1 AND `s`.`id` <= 2)",
                "JOIN source s ON s.key = t.key AND " + fragment);
    }

    @Test
    public void maximumQueryUsesSameDialectAndPhysicalNamesAsWindow() {
        assertEquals("SELECT MAX(\"更新时间\") AS MAX_VALUE FROM \"Warehouse\".\"Events\"",
                StageIncrementalSql.maxQuery("Warehouse.Events", "更新时间", "PostgreSQL"));
        assertEquals("SELECT MAX(`UpdatedAt`) AS MAX_VALUE FROM `db`.`Events`",
                StageIncrementalSql.maxQuery("db.Events", "UpdatedAt", "MySQL"));
        assertEquals("SELECT MAX([Order]) AS MAX_VALUE FROM [dbo].[Events]",
                StageIncrementalSql.maxQuery("dbo.Events", "Order", "SQLServer"));
        assertEquals("SELECT MAX(\"UPDATED_AT\") AS MAX_VALUE FROM \"APP\".\"EVENTS\"",
                StageIncrementalSql.maxQuery("APP.EVENTS", "UPDATED_AT", "Oracle"));
    }

    @Test
    public void maximumQueryRejectsExpressionsAndMalformedIdentifiers() {
        for (String table : new String[]{null, "", ".Events", "Events.", "a..b", "Events;DELETE", "\"Events\"", "a JOIN b"}) {
            invalid(() -> StageIncrementalSql.maxQuery(table, "id", "PostgreSQL"));
        }
        for (String column : new String[]{null, "", "a.id", "MAX(id)", "`id`", "id --"}) {
            invalid(() -> StageIncrementalSql.maxQuery("Events", column, "PostgreSQL"));
        }
        invalid(() -> StageIncrementalSql.maxQuery("Events", "id", "unknown"));
    }

    private static void invalid(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected validation failure, without a dependency on common.ServiceException.
        }
    }
}
