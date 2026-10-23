package com.datamaster.module.collector.utils;

import com.datamaster.common.utils.sql.SqlStatementParser;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SourceMetadataSupportTest {

    @Test
    public void requiresTrackingForLegacyDisabledInputWithoutMutatingDraft() {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("sourceMetadata", Collections.singletonMap("enabled", false));
        Map<String, Object> normalized = SourceMetadataSupport.withRequiredMetadata(draft);
        assertTrue(SourceMetadataSupport.enabled(normalized));
        assertEquals(Arrays.asList("source_table", "write_time"),
                SourceMetadataSupport.metadataColumns(normalized));
        assertFalse(SourceMetadataSupport.enabled(draft));
    }

    @Test
    public void splitsStatementsWithoutSplittingValuesOrComments() {
        assertEquals(Arrays.asList("INSERT INTO t(a,b) VALUES ('x;y', 2)",
                        "/* ; */ UPDATE t SET a='it''s;ok', b=3"),
                SqlStatementParser.parse("INSERT INTO t(a,b) VALUES ('x;y', 2);"
                        + "/* ; */ UPDATE t SET a='it''s;ok', b=3; -- comment only"));
    }

    @Test
    public void supportsExistingArraysAndDollarQuotedBodies() {
        assertEquals(Arrays.asList("SELECT 1,2", "DO $$ BEGIN RAISE NOTICE 'a;b'; END $$"),
                SqlStatementParser.parse(new String[]{"SELECT 1,2;", "DO $$ BEGIN RAISE NOTICE 'a;b'; END $$;"}));
        assertTrue(SqlStatementParser.parse("; -- comment\n /* comment ; */").isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnterminatedLiteral() {
        SqlStatementParser.parse("UPDATE t SET a='unfinished");
    }
}
