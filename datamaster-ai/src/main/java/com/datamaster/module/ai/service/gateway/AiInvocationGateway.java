package com.datamaster.module.ai.service.gateway;

import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-local, nonblocking admission. One valid begin is one logical invocation, including
 * provider retries INSIDE its lease. Never wrap individual retries in additional leases.
 * Metadata must be trusted identifiers, never prompts, credentials, URLs or exception messages.
 * Capture Context before scheduling asynchronous work; this class installs no ThreadLocal state.
 */
@Component
public class AiInvocationGateway {
    private final int modelCapacity;
    private final int dbgptCapacity;
    private final Semaphore modelPermits;
    private final Semaphore dbgptPermits;
    private final AiInvocationRepository repository;
    private final AtomicLong inFlight = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong usageUnknown = new AtomicLong();
    private final AtomicLong persistenceFailures = new AtomicLong();

    public AiInvocationGateway(@Value("${ai.invocation.max-concurrent:8}") int modelCapacity,
                               @Value("${ai.invocation.dbgpt-max-concurrent:4}") int dbgptCapacity,
                               AiInvocationRepository repository) {
        if (modelCapacity < 1 || modelCapacity > 128 || dbgptCapacity < 1 || dbgptCapacity > 128) {
            throw new IllegalArgumentException("AI invocation capacities must be between 1 and 128");
        }
        this.modelCapacity = modelCapacity;
        this.dbgptCapacity = dbgptCapacity;
        this.modelPermits = new Semaphore(modelCapacity);
        this.dbgptPermits = new Semaphore(dbgptCapacity);
        this.repository = repository;
    }

    public Invocation begin(String kind, String scene, String model, boolean streaming) {
        return begin(kind, scene, model, streaming, snapshotContext());
    }

    public Invocation begin(String kind, String scene, String model, boolean streaming, Context context) {
        return begin(kind, scene, null, model, streaming, context);
    }

    public Invocation begin(String kind, String scene, String provider, String model, boolean streaming, Context context) {
        requireIdentifier(provider, 64, true);
        if (!"MODEL".equals(kind) && !"DBGPT".equals(kind)) {
            throw new IllegalArgumentException("Invocation kind must be MODEL or DBGPT");
        }
        requireIdentifier(scene, 60, false);
        requireIdentifier(model, 100, true);
        Semaphore permits = "MODEL".equals(kind) ? modelPermits : dbgptPermits;
        Invocation invocation = new Invocation(kind, scene, provider, model, streaming,
                context == null ? new Context(null, null, null, null) : context, permits);
        if (!permits.tryAcquire()) {
            rejected.incrementAndGet();
            usageUnknown.incrementAndGet();
            invocation.status = "REJECTED";
            invocation.category = "CAPACITY";
            invocation.closed = true;
            persist(invocation);
            throw new RejectedExecutionException("AI invocation capacity exhausted");
        }
        inFlight.incrementAndGet();
        return invocation;
    }

    public static Context snapshotContext() {
        Context explicit = AiInvocationContext.current();
        if (explicit != null) return explicit;
        AgentToolContext agent = AgentToolContextHolder.get();
        Long userId = agent == null ? null : agent.getUserId();
        if (userId == null) {
            try {
                userId = SecurityUtils.getUserId();
            } catch (RuntimeException ignored) {
                // Background execution may have no security context. Never copy exception text.
            }
        }
        return new Context(userId, agent == null ? null : agent.getSpaceId(),
                agent == null ? null : agent.getSpaceCode(), agent == null ? null : agent.getOperationId());
    }

    /** Internal process metrics only; do not expose this cross-space snapshot to tenant HTTP APIs. */
    public Map<String, Long> metrics() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("inflight", inFlight.get());
        result.put("success", successes.get());
        result.put("failure", failures.get());
        result.put("rejected", rejected.get());
        result.put("usageUnknown", usageUnknown.get());
        result.put("persistenceFailures", persistenceFailures.get());
        return result;
    }

    public Map<String, Integer> processCapacity() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("MODEL", modelCapacity);
        result.put("DBGPT", dbgptCapacity);
        return result;
    }

    private void persist(Invocation invocation) {
        if (!repository.insert(invocation)) persistenceFailures.incrementAndGet();
    }

    private static void requireIdentifier(String value, int max, boolean optional) {
        if (optional && value == null) return;
        if (value == null || value.length() > max
                || !value.matches("[A-Za-z0-9][A-Za-z0-9_.:/@+\\-]*") || value.contains("://")
                || value.regionMatches(true, 0, "sk-", 0, 3)
                || value.regionMatches(true, 0, "sk_", 0, 3)
                || value.regionMatches(true, 0, "bearer", 0, 6)) {
            throw new IllegalArgumentException("AI metadata must be a bounded non-secret identifier");
        }
    }

    public static final class Context {
        private final Long userId;
        private final Long spaceId;
        private final String spaceCode;
        private final String operationId;

        /** Identity must come from an authenticated/authorized boundary, not model output. */
        public Context(Long userId, Long spaceId, String spaceCode, String operationId) {
            this.userId = userId;
            this.spaceId = spaceId;
            this.spaceCode = bounded(spaceCode, 128);
            this.operationId = bounded(operationId, 64);
        }

        private static String bounded(String value, int max) {
            return value != null && value.length() <= max
                    && value.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*") ? value : null;
        }

        public Long getUserId() { return userId; }
        public Long getSpaceId() { return spaceId; }
        public String getSpaceCode() { return spaceCode; }
        public String getOperationId() { return operationId; }
    }

    public final class Invocation implements AutoCloseable {
        final String id = UUID.randomUUID().toString();
        final String kind;
        final String scene;
        final String provider;
        final String model;
        final boolean streaming;
        final Context context;
        final long startedNanos = System.nanoTime();
        final java.sql.Timestamp createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        private final Semaphore permits;
        String status = "FAILURE";
        String category = "UNMARKED";
        Integer httpStatus;
        Long inputTokens;
        Long outputTokens;
        private boolean closed;

        private Invocation(String kind, String scene, String provider, String model, boolean streaming,
                           Context context, Semaphore permits) {
            this.kind = kind;
            this.scene = scene;
            this.provider = provider;
            this.model = model;
            this.streaming = streaming;
            this.context = context;
            this.permits = permits;
        }

        public synchronized void success(Long inputTokens, Long outputTokens) {
            if (closed) return;
            usage(inputTokens, outputTokens);
            status = "SUCCESS";
            category = null;
            httpStatus = null;
        }

        /** Optional measured usage, including partial usage before an eventual failure. */
        public synchronized void usage(Long inputTokens, Long outputTokens) {
            if (closed) return;
            this.inputTokens = nonnegative(inputTokens);
            this.outputTokens = nonnegative(outputTokens);
        }

        public synchronized void failure(String category, Integer httpStatus) {
            if (closed) return;
            status = "FAILURE";
            // Never persist arbitrary upstream text supplied as a category.
            this.category = category != null && category.matches(
                    "TIMEOUT|HTTP|NETWORK|CANCELLED|PROVIDER|PARSE|CONFIGURATION|INTERNAL|UNKNOWN")
                    ? category : "UNKNOWN";
            this.httpStatus = httpStatus != null && httpStatus >= 100 && httpStatus <= 599 ? httpStatus : null;
        }

        Long totalTokens() {
            return inputTokens != null && outputTokens != null && inputTokens <= Long.MAX_VALUE - outputTokens
                    ? inputTokens + outputTokens : null;
        }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            try {
                if ("SUCCESS".equals(status)) successes.incrementAndGet();
                else failures.incrementAndGet();
                if (totalTokens() == null) usageUnknown.incrementAndGet();
            } finally {
                inFlight.decrementAndGet();
                permits.release();
            }
            persist(this);
        }

        private Long nonnegative(Long value) { return value != null && value >= 0 ? value : null; }
    }
}
