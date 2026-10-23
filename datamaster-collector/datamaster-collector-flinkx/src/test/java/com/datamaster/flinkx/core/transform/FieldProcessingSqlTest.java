package com.datamaster.flinkx.core.transform;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/** SQL-generation contracts; runtime function verification is performed separately on Flink 1.16.2. */
public class FieldProcessingSqlTest {
    private JSONObject object(String json) {
        return com.alibaba.fastjson2.JSON.parseObject(json, JSONObject.class, com.alibaba.fastjson2.JSONReader.Feature.AllowUnQuotedFieldNames);
    }

    private String build(String component, JSONObject parameter) {
        JSONObject transition = new JSONObject();
        transition.put("componentType", component);
        transition.put("parameter", parameter);
        JSONArray transitions = new JSONArray();
        transitions.add(transition);
        return TransformSqlBuilder.build(transitions, Arrays.asList("a", "b"), "sourceTable");
    }

    @Test
    public void decimalUsesPrecisionAndScale() {
        String sql = build("SELECT_FIELDS", object("{tableFields:[{inputField:'a',outputField:'n',type:'decimal',precision:12,scale:3}]}"));
        Assert.assertTrue(sql.contains("CAST(`a` AS DECIMAL(12, 3)) AS `n`"));
    }

    @Test
    public void nullOnCastFailureUsesTryCastWithoutRegexGuards() {
        String sql = build("SELECT_FIELDS", object("{tableFields:[{inputField:'a',outputField:'n',type:'int',castFailure:'NULL'}]}"));
        Assert.assertTrue(sql.contains("TRY_CAST(`a` AS INTEGER) AS `n`"));
        Assert.assertFalse(sql.contains("REGEXP"));
    }

    @Test
    public void defaultOnCastFailurePreservesGenuineNull() {
        String sql = build("SELECT_FIELDS", object("{tableFields:[{inputField:'a',outputField:'n',type:'decimal',precision:6,scale:2,castFailure:'DEFAULT',defaultValue:'0.00'}]}"));
        Assert.assertTrue(sql.contains("CASE WHEN `a` IS NULL THEN CAST(NULL AS DECIMAL(6, 2)) ELSE COALESCE(TRY_CAST(`a` AS DECIMAL(6, 2)), CAST('0.00' AS DECIMAL(6, 2))) END"));
    }

    @Test
    public void typedDerivationAppliesFailureStrategy() {
        String sql = build("FIELD_DERIVATION", object("{fieldDerivationType:'FIELD_DERIVE_CONSTANT',fieldDerivationName:'n',constantValue:'bad',resultType:'int',castFailure:'DEFAULT',defaultValue:'7'}"));
        Assert.assertTrue(sql.contains("COALESCE(TRY_CAST('bad' AS INTEGER), CAST('7' AS INTEGER))"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void castDefaultRequiresConfiguredValue() {
        build("SELECT_FIELDS", object("{tableFields:[{inputField:'a',outputField:'n',type:'int',castFailure:'DEFAULT'}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDecimalScaleAbovePrecision() {
        build("SELECT_FIELDS", object("{tableFields:[{inputField:'a',outputField:'n',type:'decimal',precision:2,scale:3}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDecimalPrecisionAboveFlinkLimit() {
        build("SELECT_FIELDS", object("{tableFields:[{inputField:'a',outputField:'n',type:'decimal',precision:39,scale:0}]}"));
    }

    private JSONObject concat() {
        return object("{fieldDerivationType:'FIELD_DERIVE_CONCAT',fieldDerivationName:'joined',delimiter:'|',tableFields:[{columnName:'a'},{columnName:'b'}]}");
    }

    @Test
    public void concatKeepsLegacyLiteralDefault() {
        String sql = build("FIELD_DERIVATION", concat());
        Assert.assertTrue(sql.contains("COALESCE(CAST(`a` AS VARCHAR), 'null')"));
    }

    @Test
    public void concatKeepPropagatesNullEvenWithDelimiter() {
        JSONObject parameter = concat();
        parameter.put("nullMode", "KEEP");
        String sql = build("FIELD_DERIVATION", parameter);
        Assert.assertTrue(sql.contains("CONCAT(CAST(`a` AS VARCHAR), '|', CAST(`b` AS VARCHAR))"));
        Assert.assertFalse(sql.contains("CONCAT_WS"));
    }

    @Test
    public void concatSkipUsesConcatWsEvenWithoutDelimiter() {
        JSONObject parameter = concat();
        parameter.put("nullMode", "SKIP");
        parameter.put("delimiter", "");
        String sql = build("FIELD_DERIVATION", parameter);
        Assert.assertTrue(sql.contains("CONCAT_WS('', CAST(`a` AS VARCHAR), CAST(`b` AS VARCHAR))"));
        Assert.assertFalse(sql.contains("COALESCE"));
    }

    @Test
    public void concatEmptyPreservesDelimiterPositions() {
        JSONObject parameter = concat();
        parameter.put("nullMode", "EMPTY");
        String sql = build("FIELD_DERIVATION", parameter);
        Assert.assertTrue(sql.contains("CONCAT_WS('|', COALESCE(CAST(`a` AS VARCHAR), ''), COALESCE(CAST(`b` AS VARCHAR), ''))"));
    }

    @Test
    public void nullDerivationSupportsNumericTypeCodeAndEmptyNormalization() {
        String sql = build("FIELD_DERIVATION", object("{fieldDerivationType:'8',fieldDerivationName:'n',nullAction:'COALESCE',normalizeEmpty:true,trimWhitespace:true,defaultValue:'null',tableFields:[{columnName:'a'},{columnName:'b'}]}"));
        Assert.assertTrue(sql.contains("COALESCE(NULLIF(TRIM(CAST(`a` AS STRING)), ''), NULLIF(TRIM(CAST(`b` AS STRING)), ''), 'null')"));
    }

    @Test
    public void nullDefaultPreservesEmptyString() {
        String sql = build("FIELD_DERIVATION", object("{fieldDerivationType:'FIELD_DERIVE_NULL',fieldDerivationName:'n',nullAction:'DEFAULT',defaultValue:'',tableFields:[{columnName:'a'}]}"));
        Assert.assertTrue(sql.contains("COALESCE(CAST(`a` AS STRING), '')"));
    }

    @Test
    public void derivedResultCanBeTypedDecimal() {
        String sql = build("FIELD_DERIVATION", object("{fieldDerivationType:'FIELD_DERIVE_CONSTANT',fieldDerivationName:'n',constantValue:'1.25',resultType:'decimal',precision:5,scale:2}"));
        Assert.assertTrue(sql.contains("CAST('1.25' AS DECIMAL(5, 2)) AS `n`"));
    }

    @Test
    public void legacyMapConfigKeepsOriginalCaseAndFallback() {
        String sql = build("VALUE_MAP", object("{inputField:'a',outputField:'n',tableFields:[{source:'x',target:'y'}]}"));
        Assert.assertTrue(sql.contains("CASE `a` WHEN 'x' THEN 'y' ELSE `a` END AS `n`"));
    }

    @Test
    public void mappingMatchesNullAndEmptySeparatelyAndReturnsTypedValues() {
        String sql = build("VALUE_MAP", object("{inputField:'a',outputField:'n',outputType:'int',unmatched:'NULL',tableFields:[{matchType:'NULL',target:1},{matchType:'EMPTY',target:2},{source:'x',target:3}]}"));
        Assert.assertTrue(sql.contains("WHEN `a` IS NULL THEN CAST('1' AS INTEGER)"));
        Assert.assertTrue(sql.contains("WHEN CAST(`a` AS STRING) = '' THEN CAST('2' AS INTEGER)"));
        Assert.assertTrue(sql.contains("ELSE CAST(NULL AS INTEGER) END"));
    }

    @Test
    public void mappingStringPreservesNullTextEmptyAndQuotes() {
        JSONObject parameter = object("{inputField:'a',outputField:'n',unmatched:'DEFAULT',defaultValue:'',tableFields:[{source:'null',target:'null'}]}");
        parameter.getJSONArray("tableFields").getJSONObject(0).put("source", "O'Reilly");
        String sql = build("VALUE_MAP", parameter);
        Assert.assertTrue(sql.contains("= 'O''Reilly' THEN 'null'"));
        Assert.assertTrue(sql.contains("ELSE '' END"));
    }

    @Test
    public void typedMappingsSupportLongDecimalAndBoolean() {
        String[] types = {"long", "decimal", "boolean"};
        String[] values = {"9223372036854775807", "123.45", "true"};
        String[] expected = {"CAST('9223372036854775807' AS BIGINT)", "CAST('123.45' AS DECIMAL(10, 2))", "TRUE"};
        for (int i = 0; i < types.length; i++) {
            JSONObject parameter = object("{inputField:'a',outputField:'n',unmatched:'NULL',precision:10,scale:2,tableFields:[{source:'x',target:'x'}]}");
            parameter.put("outputType", types[i]);
            parameter.getJSONArray("tableFields").getJSONObject(0).put("target", values[i]);
            Assert.assertTrue(build("VALUE_MAP", parameter).contains("THEN " + expected[i]));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMappingIntegerOverflow() {
        build("VALUE_MAP", object("{inputField:'a',outputField:'n',outputType:'int',tableFields:[{source:'x',target:'2147483648'}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMappingDecimalRoundingLoss() {
        build("VALUE_MAP", object("{inputField:'a',outputField:'n',outputType:'decimal',precision:5,scale:2,tableFields:[{source:'x',target:'1.234'}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesNotPretendUnmatchedFailIsImplemented() {
        build("VALUE_MAP", object("{inputField:'a',outputField:'n',unmatched:'FAIL',tableFields:[{source:'x',target:'y'}]}"));
    }
}
