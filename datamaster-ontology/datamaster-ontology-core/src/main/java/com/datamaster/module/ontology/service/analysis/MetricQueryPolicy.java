package com.datamaster.module.ontology.service.analysis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.Semaphore;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Deployment gate, not a replacement for source/space/column permissions. */
@Component
public class MetricQueryPolicy {
    private final boolean enabled;
    private final Set<Long> approvedBindingIds;
    private final Set<String> readonlyUsernames;
    private final int timeoutSeconds;
    private final Semaphore permits;

    public MetricQueryPolicy(@Value("${ontology.metric-query.enabled:false}") boolean enabled,
            @Value("${ontology.metric-query.approved-binding-ids:}") String approvedBindingIds,
            @Value("${ontology.metric-query.allowlisted-readonly-usernames:}") String readonlyUsernames,
            @Value("${ontology.metric-query.timeout-seconds:15}") int timeoutSeconds,
            @Value("${ontology.metric-query.max-concurrent:4}") int concurrency) {
        this.enabled = enabled;
        this.approvedBindingIds = parseBindingIds(approvedBindingIds);
        this.readonlyUsernames = parseUsernames(readonlyUsernames);
        require(timeoutSeconds >= 1 && timeoutSeconds <= 60, "指标查询超时配置必须在1到60秒");
        require(concurrency >= 1 && concurrency <= 16, "指标查询并发配置必须在1到16之间");
        this.timeoutSeconds = timeoutSeconds;
        this.permits = new Semaphore(concurrency);
    }
    public void assertEnabled() { require(enabled, "受控指标查询尚未启用"); }
    public void assertBinding(Long id) {
        assertEnabled(); require(id != null && approvedBindingIds.contains(id), "该表绑定未经管理员批准用于指标分析");
    }
    public void assertReadonlyUsername(String username) {
        require(username != null && readonlyUsernames.contains(username), "数据源账号不在批准的只读账号名单中");
    }
    public boolean tryAcquire() { return permits.tryAcquire(); }
    public void release() { permits.release(); }
    public int getTimeoutSeconds() { return timeoutSeconds; }

    public static Set<Long> parseBindingIds(String text) {
        Set<Long> ids = new LinkedHashSet<>();
        if (text == null || text.trim().isEmpty()) return Collections.emptySet();
        require(text.length() <= 10000, "批准绑定名单过长");
        for (String token : text.split(",", -1)) {
            String value = token.trim();
            require(value.matches("[1-9][0-9]*"), "批准绑定名单只能包含逗号分隔的正整数ID");
            try { require(ids.add(Long.valueOf(value)), "批准绑定名单不能重复"); }
            catch (NumberFormatException ex) { throw new com.datamaster.common.exception.ServiceException("批准绑定ID超出Long范围"); }
        }
        return Collections.unmodifiableSet(ids);
    }
    private static Set<String> parseUsernames(String text) {
        Set<String> users = new LinkedHashSet<>();
        if (text == null || text.trim().isEmpty()) return Collections.emptySet();
        require(text.length() <= 10000, "只读账号名单过长");
        for (String token : text.split(",", -1)) {
            String value = token.trim();
            require(value.matches("[A-Za-z0-9_@.\\-]{1,128}") && users.add(value), "只读账号名单格式不正确或重复");
        }
        return Collections.unmodifiableSet(users);
    }
}
