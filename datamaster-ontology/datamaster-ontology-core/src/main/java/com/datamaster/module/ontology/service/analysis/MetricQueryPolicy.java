package com.datamaster.module.ontology.service.analysis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.Semaphore;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Operational protection only. Business source admission is stored in the application database. */
@Component
public class MetricQueryPolicy {
    private final boolean enabled;
    private final int timeoutSeconds;
    private final int maxConcurrent;
    private final Semaphore permits;

    public MetricQueryPolicy(@Value("${ontology.metric-query.enabled:true}") boolean enabled,
            @Value("${ontology.metric-query.timeout-seconds:15}") int timeoutSeconds,
            @Value("${ontology.metric-query.max-concurrent:4}") int concurrency) {
        require(timeoutSeconds >= 1 && timeoutSeconds <= 60, "指标查询超时配置必须在1到60秒");
        require(concurrency >= 1 && concurrency <= 16, "指标查询并发配置必须在1到16之间");
        this.enabled = enabled;
        this.timeoutSeconds = timeoutSeconds;
        this.maxConcurrent = concurrency;
        this.permits = new Semaphore(concurrency);
    }
    public void assertEnabled() { require(enabled, "指标查询已被运维紧急关闭"); }
    public boolean isEnabled() { return enabled; }
    public boolean tryAcquire() { return permits.tryAcquire(); }
    public void release() { permits.release(); }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getMaxConcurrent() { return maxConcurrent; }
}
