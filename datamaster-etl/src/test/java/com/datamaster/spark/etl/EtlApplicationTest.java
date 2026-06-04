package com.datamaster.spark.etl;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ETL应用测试类
 */
public class EtlApplicationTest {

    private static final Logger log = LoggerFactory.getLogger(EtlApplicationTest.class);

    @Test
    public void testEtlApplication() {
        // 初始化数据库工具
        com.datamaster.spark.etl.utils.db.DBUtils.init();
        
        // 创建日期对象
        java.util.Date now = new java.util.Date();
        
        // 创建测试用的JSON配置
        JSONObject taskParams = new JSONObject();
        
        // 配置对象
        JSONObject config = new JSONObject();
        
        // Redis配置
        JSONObject redis = new JSONObject();
        redis.put("host", "localhost");
        redis.put("port", 6379);
        config.put("redis", redis);
        
        // TaskInfo配置
        JSONObject taskInfo = new JSONObject();
        taskInfo.put("name", "test_task");
        taskInfo.put("taskCode", "TEST_001");
        taskInfo.put("taskVersion", 1);
        taskInfo.put("projectCode", "PROJ_001");
        config.put("taskInfo", taskInfo);
        
        taskParams.put("config", config);
        
        // Reader配置
        JSONObject reader = new JSONObject();
        reader.put("componentType", "CSV_READER"); // 假设这是一个有效的组件类型
        JSONObject readParameter = new JSONObject();
        readParameter.put("path", "test.csv");
        reader.put("parameter", readParameter);
        taskParams.put("reader", reader);
        
        // Writer配置
        JSONObject writer = new JSONObject();
        writer.put("componentType", "DB_WRITER"); // 假设这是一个有效的组件类型
        JSONObject writeParameter = new JSONObject();
        writeParameter.put("tableName", "test_table");
        writer.put("parameter", writeParameter);
        taskParams.put("writer", writer);
        
        // 转换配置（可选）
        // JSONArray transitionArr = new JSONArray();
        // // 添加转换配置...
        // taskParams.put("transition", transitionArr);
        
        // 编码为Base64
        String jsonStr = taskParams.toJSONString();
        String args0 = Base64.encode(jsonStr);
        log.info("Args0: {}", args0);
        
        // 解码Base64字符串
        String decodedStr = Base64.decodeStr(args0);
        log.info("Decoded: {}", decodedStr);
        
        // 解析JSON对象
        JSONObject parsedTaskParams = JSONObject.parseObject(decodedStr);
        assertNotNull(parsedTaskParams, "Parsed task params should not be null");
        
        // 现在可以安全地访问属性
        JSONObject parsedConfig = parsedTaskParams.getJSONObject("config");
        assertNotNull(parsedConfig, "Config should not be null");
        
        JSONObject parsedRedis = parsedConfig.getJSONObject("redis");
        assertNotNull(parsedRedis, "Redis config should not be null");
        
        JSONObject parsedTaskInfo = parsedConfig.getJSONObject("taskInfo");
        assertNotNull(parsedTaskInfo, "TaskInfo config should not be null");
        
        assertTrue(parsedRedis.getString("host").equals("localhost"), "Redis host should be localhost");
        assertTrue(parsedTaskInfo.getString("name").equals("test_task"), "Task name should be test_task");
        
        log.info("Test passed!");
    }
}