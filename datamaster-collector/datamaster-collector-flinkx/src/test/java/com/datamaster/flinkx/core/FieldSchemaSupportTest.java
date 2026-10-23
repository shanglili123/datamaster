package com.datamaster.flinkx.core;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class FieldSchemaSupportTest {
    @Test public void retainsSelectedOrderAndSourceTypes() {
        List<JSONObject> columns = FieldSchemaSupport.columns(Arrays.asList("amount", "id", "name", "write_time"), Arrays.asList(
                field("id", "BIGINT"), field("name", "VARCHAR"), field("amount", "numeric", "columnLength", "12", "columnScale", "2")), Collections.emptyMap());
        assertEquals("amount", columns.get(0).getString("name"));
        assertEquals("DECIMAL(12,2)", columns.get(0).getString("type"));
        assertEquals("BIGINT", columns.get(1).getString("type"));
        assertEquals("STRING", columns.get(2).getString("type"));
        assertEquals("TIMESTAMP", columns.get(3).getString("type"));
    }
    @Test public void preservesPrecisionAndNullableWithoutMakingNullFalse() {
        Map<String, Object> field = field("total", "decimal", "precision", 18, "scale", 4, "nullable", true);
        List<JSONObject> columns = FieldSchemaSupport.columns(Collections.singletonList("total"), Collections.singletonList(field), null);
        assertEquals("DECIMAL(18,4)", columns.get(0).getString("type"));
        assertEquals(Boolean.TRUE, columns.get(0).getBoolean("nullable"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsInvalidDecimalInsteadOfSilentlyUsingString() {
        FieldSchemaSupport.columns(Collections.singletonList("amount"), Collections.singletonList(field("amount", "decimal", "precision", 39, "scale", 2)), null);
    }
    private Map<String, Object> field(String name,String type,Object...extras) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("columnName",name);result.put("columnType",type);
        for(int i=0;i<extras.length;i+=2)result.put(String.valueOf(extras[i]),extras[i+1]);return result;
    }
}
