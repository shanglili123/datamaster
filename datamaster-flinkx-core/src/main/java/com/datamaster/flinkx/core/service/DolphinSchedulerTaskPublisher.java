package com.datamaster.flinkx.core.service;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.flinkx.core.model.FlinkxJobConfig;
import com.datamaster.flinkx.core.util.EtlLog;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
public class DolphinSchedulerTaskPublisher {

    private static final String DEFAULT_OUTPUT_DIR = System.getProperty("user.dir") + File.separator + "flinkx-jobs";

    public static void publish(FlinkxJobConfig jobConfig, JSONObject taskParams, JSONObject writer, EtlLog.Params logParams) {
        String jobName = jobConfig.getJobName() != null ? sanitizeFileName(jobConfig.getJobName()) : "flinkx-job";
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String filename = jobName + "-" + timestamp + ".json";

        File outputDir = new File(DEFAULT_OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File jobFile = new File(outputDir, filename);

        try {
            String jobJson = buildRunnableJobJson(jobConfig);
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(jobFile), StandardCharsets.UTF_8))) {
                bw.write(jobJson);
                bw.flush();
            }
            log.info("FlinkX job JSON written to: {}", jobFile.getAbsolutePath());
            EtlLog.write(logParams, "FlinkX任务JSON已生成: " + jobFile.getAbsolutePath());

            String runCommand = buildRunCommand(jobFile.getAbsolutePath());
            log.info("Run FlinkX job with command: {}", runCommand);
            EtlLog.write(logParams, "执行命令: " + runCommand);

        } catch (IOException e) {
            log.error("Failed to write FlinkX job JSON to file: {}", jobFile.getAbsolutePath(), e);
            EtlLog.write(logParams, "FlinkX任务JSON写入失败: " + e.getMessage());
        }
    }

    private static String buildRunnableJobJson(FlinkxJobConfig jobConfig) {
        JSONObject root = new JSONObject();
        JSONObject job = new JSONObject();

        Map<String, Object> flinkx = jobConfig.getFlinkx();
        if (flinkx != null) {
            Object setting = flinkx.get("setting");
            if (setting != null) {
                job.put("setting", setting);
            }

            Object content = flinkx.get("content");
            if (content != null) {
                job.put("content", content);
            }
        }

        root.put("job", job);
        return root.toJSONString();
    }

    private static String buildRunCommand(String jobFilePath) {
        return "java -jar flinkx-core.jar -mode standalone -jobType sync -job \""
                + jobFilePath + "\" -pluginRoot flinkx-plugin";
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
    }
}
