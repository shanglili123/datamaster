package com.datamaster.module.ai.service.gateway;

import com.datamaster.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Explicit migration is required; no table creation is performed at runtime. */
@Repository
public class AiInvocationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(AiInvocationRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public AiInvocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean insert(AiInvocationGateway.Invocation invocation) {
        try {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - invocation.startedNanos);
            Long totalTokens = invocation.totalTokens();
            return jdbcTemplate.update("INSERT INTO ai_model_invocation "
                            + "(id, kind, scene, provider, model, user_id, space_id, space_code, operation_id, streaming, "
                            + "status, duration_ms, input_tokens, output_tokens, total_tokens, usage_known, "
                            + "http_status, category, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    invocation.id, invocation.kind, invocation.scene, invocation.provider, invocation.model,
                    invocation.context.getUserId(), invocation.context.getSpaceId(),
                    invocation.context.getSpaceCode(), invocation.context.getOperationId(), invocation.streaming,
                    invocation.status, durationMs, invocation.inputTokens, invocation.outputTokens, totalTokens,
                    totalTokens != null, invocation.httpStatus, invocation.category, invocation.createdAt) == 1;
        } catch (RuntimeException ignored) {
            // No throwable/driver message: those can contain SQL, URLs or credentials.
            LOG.warn("AI invocation persistence failed; invocation outcome is unchanged");
            return false;
        }
    }

    public List<Map<String, Object>> aggregate(Long spaceId, String spaceCode, int days) {
        if (days < 1 || days > 30 || spaceId == null || spaceCode == null) {
            throw new IllegalArgumentException("A space and a 1..30 day window are required");
        }
        long now = System.currentTimeMillis();
        try {
            return jdbcTemplate.queryForList("SELECT kind, scene, provider, model, status, COUNT(*) AS attempts, "
                            + "SUM(duration_ms) AS durationMsSum, AVG(duration_ms) AS durationMsAverage, "
                            + "COALESCE(SUM(input_tokens), 0) AS inputTokens, "
                            + "COALESCE(SUM(output_tokens), 0) AS outputTokens, "
                            + "COALESCE(SUM(input_tokens), 0) + COALESCE(SUM(output_tokens), 0) AS tokensCount, "
                            + "COALESCE(SUM(total_tokens), 0) AS knownTotalTokens, "
                            + "SUM(CASE WHEN usage_known = TRUE THEN 1 ELSE 0 END) AS usageKnownCount, "
                            + "SUM(CASE WHEN usage_known = FALSE THEN 1 ELSE 0 END) AS usageUnknownCount "
                            + "FROM ai_model_invocation WHERE space_id = ? AND space_code = ? "
                            + "AND created_at >= ? AND created_at <= ? "
                            + "GROUP BY kind, scene, provider, model, status ORDER BY kind, provider, model, scene, status",
                    spaceId, spaceCode, new Timestamp(now - TimeUnit.DAYS.toMillis(days)), new Timestamp(now));
        } catch (RuntimeException ignored) {
            throw new ServiceException("AI调用统计暂不可用，请确认统计表已部署");
        }
    }
}
