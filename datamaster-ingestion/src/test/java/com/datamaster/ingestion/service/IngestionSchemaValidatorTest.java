package com.datamaster.ingestion.service;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.ingestion.config.IngestionProperties;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IngestionSchemaValidatorTest {

    private IngestionProperties properties;
    private IngestionSchemaValidator validator;

    @Before
    public void setUp() throws Exception {
        properties = new IngestionProperties();
        validator = new IngestionSchemaValidator();
        Field field = IngestionSchemaValidator.class.getDeclaredField("properties");
        field.setAccessible(true);
        field.set(validator, properties);
    }

    @Test
    public void shouldAcceptValidMessage() {
        properties.setAllowedTables(Arrays.asList("customer"));
        properties.setRequiredColumns(Arrays.asList("id", "name"));
        properties.setKeyColumns(Arrays.asList("id"));
        properties.setSchemaVersion("v1");
        JSONObject message = new JSONObject();
        message.put("__schemaVersion", "v1");
        message.put("id", 1);
        message.put("name", "Alice");

        assertNull(validator.validate(message, "customer"));
    }

    @Test
    public void shouldRejectUnsafeIdentifierAndTableOutsideWhitelist() {
        properties.setAllowedTables(Arrays.asList("customer"));
        JSONObject message = new JSONObject();
        message.put("id", 1);

        assertTrue(validator.validate(message, "customer;drop").contains("目标表名不合法"));
        assertTrue(validator.validate(message, "orders").contains("白名单"));
    }

    @Test
    public void shouldRejectSchemaMismatchAndUnsafeKeyColumn() {
        properties.setSchemaVersion("v2");
        JSONObject message = new JSONObject();
        message.put("__schemaVersion", "v1");
        message.put("id", 1);
        assertTrue(validator.validate(message, "customer").contains("版本不匹配"));

        properties.setSchemaVersion(null);
        message.put("__keys", "id, bad-name");
        assertTrue(validator.validate(message, "customer").contains("非法字段名"));
    }

    @Test
    public void deleteMessageOnlyRequiresKeyFields() {
        properties.setRequiredColumns(Arrays.asList("name"));
        properties.setKeyColumns(Arrays.asList("id"));
        JSONObject message = new JSONObject();
        message.put("__op", "DELETE");
        message.put("id", 1);

        assertNull(validator.validate(message, "customer"));
    }

    @Test
    public void shouldRejectMessageWithoutAnyKeyDefinition() {
        JSONObject message = new JSONObject();
        message.put("id", 1);

        assertTrue(validator.validate(message, "customer").contains("未配置冲突判定主键列"));
    }

    @Test
    public void shouldRejectCaseInsensitiveDuplicateColumns() {
        properties.setKeyColumns(Arrays.asList("id"));
        JSONObject message = new JSONObject();
        message.put("id", 1);
        message.put("ID", 2);

        assertTrue(validator.validate(message, "customer").contains("大小写不同但名称相同"));
    }
}
