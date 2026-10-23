package com.datamaster.ingestion.service;

import com.datamaster.ingestion.dal.mapper.IngestionEventMapper;
import com.datamaster.ingestion.dal.mapper.IngestionLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/** 数据接入运行指标查询服务。 */
@Slf4j
@Service
public class IngestionMetricsService {

    @Resource
    private IngestionLogMapper logMapper;

    @Resource
    private IngestionEventMapper eventMapper;

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Long> logs = new LinkedHashMap<>();
        Map<String, Long> events = new LinkedHashMap<>();
        for (String status : new String[]{"SUCCESS", "CONFLICT_SKIP", "FAILED", "DEAD_LETTER", "REPLAYING", "REPLAYED"}) {
            logs.put(status, safeLogCount(status));
        }
        for (String status : new String[]{"PROCESSING", "FAILED", "SUCCESS", "DEAD_LETTER"}) {
            events.put(status, safeEventCount(status));
        }
        result.put("logs", logs);
        result.put("events", events);
        return result;
    }

    private long safeLogCount(String status) {
        try {
            return logMapper.countByStatus(status);
        } catch (Exception e) {
            log.warn("读取 ingestion 日志指标失败 status={}", status, e);
            return -1L;
        }
    }

    private long safeEventCount(String status) {
        try {
            return eventMapper.countByStatus(status);
        } catch (Exception e) {
            log.warn("读取 ingestion 事件指标失败 status={}", status, e);
            return -1L;
        }
    }
}
