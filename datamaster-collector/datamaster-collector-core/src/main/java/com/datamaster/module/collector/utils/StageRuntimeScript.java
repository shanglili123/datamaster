package com.datamaster.module.collector.utils;

import com.datamaster.common.exception.ServiceException;
import java.net.URI;

/** Fixed server-generated script; response bytes are files, never shell syntax or DS variables. */
public final class StageRuntimeScript {
    private StageRuntimeScript() { }
    public static String prepare(String url, String token, String stageKey) {
        validate(url, token);
        if (stageKey == null || !stageKey.matches("[A-Za-z0-9_-]{1,128}")) throw new ServiceException("阶段标识不合法");
        return "set -eu\nset +x\numask 077\nwork=$(mktemp -d)\ntrap 'rm -rf -- \"$work\"' EXIT\n"
                + "printf '%s\\n' 'header = \"X-DataMaster-Task-Token: " + token + "\"' > \"$work/headers\"\n"
                + "ready=false\nfor attempt in 1 2 3 4 5; do\n"
                + "status=$(curl --silent --show-error --connect-timeout 15 --max-time 120 --get " + quote(url)
                + " --config \"$work/headers\" --data-urlencode 'stageKey=" + stageKey + "'"
                + " --data-urlencode 'schedulerInstanceId=${system.workflow.instance.id}' --output \"$work/job.json\" --write-out '%{http_code}')\n"
                + "if [ \"$status\" = 200 ]; then ready=true; break; fi\n"
                + "if [ \"$status\" != 409 ]; then exit 1; fi\n"
                + "sleep 2\ndone\n[ \"$ready\" = true ]\n"
                + "test -s \"$work/job.json\"\n"
                + "test -n \"$CHUNJUN_HOME\"\n"
                + "\"$CHUNJUN_HOME/bin/start-chunjun\" -mode local -jobType sync -job \"$work/job.json\" -chunjunDistDir \"$CHUNJUN_HOME/chunjun-dist\"\n";
    }
    public static String guard(String url, String token) {
        validate(url, token);
        return "set -eu\nset +x\numask 077\nwork=$(mktemp -d)\ntrap 'rm -rf -- \"$work\"' EXIT\n"
                + "printf '%s\\n' 'header = \"X-DataMaster-Task-Token: " + token + "\"' > \"$work/headers\"\n"
                + "ready=false\nfor attempt in 1 2 3 4 5; do\n"
                + "status=$(curl --silent --show-error --connect-timeout 15 --max-time 120 --request POST "
                + quote(url) + " --config \"$work/headers\" --dump-header \"$work/response-headers\""
                + " --output \"$work/execution\" --write-out '%{http_code}')\n"
                + "if [ \"$status\" = 200 ]; then ready=true; break; fi\n"
                + "if [ \"$status\" != 503 ] || ! grep -Eiq '^X-DataMaster-Admission-Retryable:[[:space:]]*true[[:space:]]*$' \"$work/response-headers\"; then exit 1; fi\n"
                + "if [ \"$attempt\" = 5 ]; then exit 1; fi\nsleep 2\ndone\n[ \"$ready\" = true ]\n"
                + "grep -Eq '^[1-9][0-9]{0,18}$' \"$work/execution\"\n";
    }
    private static void validate(String url, String token) {
        try {
            URI uri = URI.create(url);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || url.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
        } catch (RuntimeException ex) { throw new ServiceException("阶段回调地址无效"); }
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw new ServiceException("任务回调凭据无效");
    }
    private static String quote(String value) { return "'" + value.replace("'", "'\"'\"'") + "'"; }
}
