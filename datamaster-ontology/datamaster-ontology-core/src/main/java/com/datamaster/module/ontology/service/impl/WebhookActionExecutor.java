package com.datamaster.module.ontology.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.DnsResolver;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 受控的外部 Webhook 执行器。
 *
 * <p>动作执行必须经过这里，不能直接从 Workflow 调 HTTP。这里统一做模板解析、
 * SSRF 防护、请求超时、禁止重定向和响应大小限制。超时发生在请求可能已经发出之后，
 * 会抛出 unknownOutcome=true，调用方应将执行记录标记为人工对账，而不是自动重放。</p>
 */
@Component
public class WebhookActionExecutor {

    public static final String ACTION_TYPE = "WEBHOOK";
    private static final int MAX_CONFIG_LENGTH = 512 * 1024;
    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_HEADER_COUNT = 32;
    private static final int MAX_HEADER_LENGTH = 2048;
    private static final int MAX_BODY_LENGTH = 256 * 1024;
    private static final int MAX_RESPONSE_LENGTH = 64 * 1024;
    private static final int DEFAULT_TIMEOUT_MS = 10000;
    private static final int MIN_TIMEOUT_MS = 100;
    private static final int MAX_TIMEOUT_MS = 30000;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^{}]+)}");
    private static final Pattern HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,128}");
    private static final Set<String> METHODS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE")));
    private static final Set<String> FORBIDDEN_HEADERS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("host", "content-length", "transfer-encoding", "connection")));

    @Resource
    private ObjectMapper objectMapper;

    /** 逗号分隔的精确 host 或 *.example.com；为空时允许公网地址（仍禁止内网/保留地址）。 */
    @Value("${datamaster.ontology.webhook.allowed-hosts:}")
    private String allowedHosts;

    /** 默认只允许 Webhook 访问标准 HTTP(S) 端口，避免把动作变成任意端口扫描器。 */
    @Value("${datamaster.ontology.webhook.allowed-ports:80,443}")
    private String allowedPorts;

    /** 只做配置结构校验，不访问网络。保存动作时调用。 */
    public static void validateDefinition(String rawConfig) {
        if (rawConfig == null || rawConfig.trim().isEmpty()) {
            throw new IllegalArgumentException("WEBHOOK 动作必须配置 executionSteps JSON");
        }
        if (rawConfig.getBytes(StandardCharsets.UTF_8).length > MAX_CONFIG_LENGTH) {
            throw new IllegalArgumentException("WEBHOOK 配置过大，不能超过 " + MAX_CONFIG_LENGTH + " 字节");
        }
        final JsonNode root;
        try {
            root = new ObjectMapper().readTree(rawConfig);
        } catch (Exception e) {
            throw new IllegalArgumentException("WEBHOOK 配置不是合法 JSON: " + e.getMessage(), e);
        }
        validateDefinition(root);
    }

    private static void validateDefinition(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("WEBHOOK executionSteps 必须是 JSON 对象");
        }
        String url = text(root, "url");
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("WEBHOOK 配置缺少 url");
        }
        if (url.length() > MAX_URL_LENGTH || containsControl(url)) {
            throw new IllegalArgumentException("WEBHOOK url 长度或字符不合法");
        }
        // URL 可以包含参数模板，最终请求前会再次进行严格 URI/SSRF 校验。
        if (!url.contains("${")) {
            validateUriSyntax(url);
        }
        String method = text(root, "method");
        if (method == null || method.trim().isEmpty()) method = "POST";
        method = method.toUpperCase(Locale.ROOT);
        if (!METHODS.contains(method)) {
            throw new IllegalArgumentException("WEBHOOK 不支持 HTTP 方法: " + method);
        }
        JsonNode headers = root.get("headers");
        if (headers != null && !headers.isNull()) {
            if (!headers.isObject() || headers.size() > MAX_HEADER_COUNT) {
                throw new IllegalArgumentException("WEBHOOK headers 必须是对象且最多包含 " + MAX_HEADER_COUNT + " 项");
            }
            Iterator<Map.Entry<String, JsonNode>> it = headers.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = entry.getKey();
                if (!HEADER_NAME.matcher(key).matches()) {
                    throw new IllegalArgumentException("WEBHOOK header 名称不合法: " + key);
                }
                if (FORBIDDEN_HEADERS.contains(key.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("WEBHOOK 不允许覆盖请求控制 header: " + key);
                }
                JsonNode value = entry.getValue();
                if (value == null || !value.isValueNode() || value.isNull()) {
                    throw new IllegalArgumentException("WEBHOOK header 值必须是字符串/数字/布尔值: " + key);
                }
                String valueText = value.asText();
                if (valueText.length() > MAX_HEADER_LENGTH || containsControl(valueText)) {
                    throw new IllegalArgumentException("WEBHOOK header 值过长或包含换行: " + key);
                }
            }
        }
        JsonNode body = root.get("bodyTemplate");
        if (body != null && !body.isNull()) {
            try {
                if (body.toString().getBytes(StandardCharsets.UTF_8).length > MAX_BODY_LENGTH) {
                    throw new IllegalArgumentException("WEBHOOK bodyTemplate 不能超过 " + MAX_BODY_LENGTH + " 字节");
                }
                validateTemplateTree(body);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("WEBHOOK bodyTemplate 无法解析: " + e.getMessage(), e);
            }
            if (("GET".equals(method) || "DELETE".equals(method))) {
                throw new IllegalArgumentException(method + " WEBHOOK 不允许配置 bodyTemplate");
            }
        }
        JsonNode timeout = root.get("timeoutMs");
        if (timeout != null && !timeout.isNull()
                && (!timeout.isIntegralNumber()
                || timeout.asInt() < MIN_TIMEOUT_MS || timeout.asInt() > MAX_TIMEOUT_MS)) {
            throw new IllegalArgumentException("WEBHOOK timeoutMs 必须在 " + MIN_TIMEOUT_MS
                    + " 到 " + MAX_TIMEOUT_MS + " 毫秒之间");
        }
        JsonNode idempotent = root.get("idempotent");
        if (idempotent != null && !idempotent.isBoolean()) {
            throw new IllegalArgumentException("WEBHOOK idempotent 必须是布尔值");
        }
    }

    private static void validateTemplateTree(JsonNode node) {
        if (node == null) return;
        if (node.isTextual()) {
            if (containsControl(node.asText()) || node.asText().length() > MAX_BODY_LENGTH) {
                throw new IllegalArgumentException("WEBHOOK 模板字符串过长或包含控制字符");
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) validateTemplateTree(child);
        } else if (node.isObject()) {
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) validateTemplateTree(values.next());
        }
    }

    private static boolean containsControl(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\u0000') return true;
        }
        return false;
    }

    private static String text(JsonNode root, String name) {
        JsonNode value = root.get(name);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static void validateUriSyntax(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException("WEBHOOK url 只允许不带用户信息/片段的 http/https 地址");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("WEBHOOK url 不是合法 URI: " + e.getMessage(), e);
        }
    }

    /** 解析并验证一次请求，调用方可用其生成审批预览；此方法不会发送网络请求。 */
    public PreparedRequest prepare(String rawConfig, Map<String, Object> params,
                                   Map<String, Object> context) {
        validateDefinition(rawConfig);
        try {
            JsonNode root = objectMapper.readTree(rawConfig);
            Map<String, Object> safeParams = params == null ? Collections.<String, Object>emptyMap() : params;
            Map<String, Object> safeContext = context == null ? Collections.<String, Object>emptyMap() : context;
            String url = resolveScalar(root.path("url"), safeParams, safeContext, "url");
            String method = root.path("method").asText("POST").toUpperCase(Locale.ROOT);
            URI uri = validateTargetUri(url);
            Map<String, String> headers = new LinkedHashMap<>();
            JsonNode headerNode = root.get("headers");
            if (headerNode != null && headerNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = headerNode.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    headers.put(entry.getKey(), resolveScalar(entry.getValue(), safeParams, safeContext,
                            "header " + entry.getKey()));
                }
            }
            String body = null;
            JsonNode bodyNode = root.get("bodyTemplate");
            if (bodyNode != null && !bodyNode.isNull()) {
                JsonNode resolved = resolveNode(bodyNode, safeParams, safeContext);
                body = objectMapper.writeValueAsString(resolved);
                if (body.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_LENGTH) {
                    throw new IllegalArgumentException("WEBHOOK bodyTemplate 解析后超过大小限制");
                }
            }
            int timeoutMs = root.path("timeoutMs").asInt(DEFAULT_TIMEOUT_MS);
            boolean idempotent = root.path("idempotent").asBoolean(false);
            return new PreparedRequest(uri, method, headers, body, timeoutMs, idempotent);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("WEBHOOK 请求模板解析失败: " + e.getMessage(), e);
        }
    }

    /** 执行已解析请求。所有重定向均关闭。 */
    public WebhookResult execute(PreparedRequest request) throws WebhookCallException {
        long started = System.currentTimeMillis();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(request.timeoutMs)
                .setConnectionRequestTimeout(request.timeoutMs)
                .setSocketTimeout(request.timeoutMs)
                .build();
        HttpUriRequest httpRequest = createRequest(request);
        for (Map.Entry<String, String> entry : request.headers.entrySet()) {
            httpRequest.addHeader(entry.getKey(), entry.getValue());
        }
        if (request.body != null && httpRequest instanceof HttpEntityEnclosingRequestBase) {
            if (!containsHeader(request.headers, "Content-Type")) {
                httpRequest.setHeader("Content-Type", "application/json;charset=UTF-8");
            }
            ((HttpEntityEnclosingRequestBase) httpRequest)
                    .setEntity(new StringEntity(request.body, ContentType.APPLICATION_JSON));
        }
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .setDnsResolver(new SafeDnsResolver(this))
                .build()) {
            org.apache.http.client.methods.CloseableHttpResponse response = null;
            try {
                response = client.execute(httpRequest);
                int status = response.getStatusLine().getStatusCode();
                String contentType = response.getFirstHeader("Content-Type") == null
                        ? null : response.getFirstHeader("Content-Type").getValue();
                String responseBody = readBodyBounded(response.getEntity());
                long duration = System.currentTimeMillis() - started;
                if (status < 200 || status >= 300) {
                    throw new WebhookCallException("WEBHOOK 返回非 2xx 状态: " + status,
                            false, status);
                }
                return new WebhookResult(status, contentType, responseBody, duration);
            } catch (ConnectTimeoutException e) {
                throw new WebhookCallException("WEBHOOK 连接超时，未确认请求是否到达目标端", false, 0, e);
            } catch (SocketTimeoutException e) {
                throw new WebhookCallException("WEBHOOK 响应超时，目标系统可能已处理请求，需人工对账", true, 0, e);
            } catch (WebhookCallException e) {
                throw e;
            } catch (Exception e) {
                throw new WebhookCallException("WEBHOOK 网络请求失败: " + e.getMessage(), false, 0, e);
            } finally {
                if (response != null) {
                    try { response.close(); } catch (Exception ignored) { }
                }
            }
        } catch (WebhookCallException e) {
            throw e;
        } catch (Exception e) {
            throw new WebhookCallException("WEBHOOK 客户端初始化失败: " + e.getMessage(), false, 0, e);
        }
    }

    private HttpUriRequest createRequest(PreparedRequest request) throws WebhookCallException {
        if ("GET".equals(request.method)) return new HttpGet(request.uri);
        if ("POST".equals(request.method)) return new HttpPost(request.uri);
        if ("PUT".equals(request.method)) return new HttpPut(request.uri);
        if ("PATCH".equals(request.method)) return new HttpPatch(request.uri);
        if ("DELETE".equals(request.method)) return new HttpDelete(request.uri);
        throw new WebhookCallException("WEBHOOK 方法不受支持: " + request.method, false, 0);
    }

    private String readBodyBounded(HttpEntity entity) throws Exception {
        if (entity == null) return "";
        try (InputStream input = entity.getContent()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_RESPONSE_LENGTH) {
                    throw new IllegalArgumentException("WEBHOOK 响应体超过 " + MAX_RESPONSE_LENGTH + " 字节");
                }
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static boolean containsHeader(Map<String, String> headers, String name) {
        for (String key : headers.keySet()) {
            if (name.equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private JsonNode resolveNode(JsonNode node, Map<String, Object> params,
                                 Map<String, Object> context) {
        if (node == null || node.isNull()) return objectMapper.getNodeFactory().nullNode();
        if (node.isTextual()) {
            String value = node.asText();
            Matcher exact = Pattern.compile("^\\$\\{([^{}]+)}$").matcher(value);
            if (exact.matches()) {
                Object resolved = resolvePath(exact.group(1), params, context);
                if (resolved == null) throw new IllegalArgumentException("模板引用不存在: " + exact.group(1));
                return objectMapper.valueToTree(resolved);
            }
            Matcher matcher = PLACEHOLDER.matcher(value);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                Object resolved = resolvePath(matcher.group(1), params, context);
                if (resolved == null) throw new IllegalArgumentException("模板引用不存在: " + matcher.group(1));
                matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(resolved)));
            }
            matcher.appendTail(result);
            return objectMapper.getNodeFactory().textNode(result.toString());
        }
        if (node.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                result.set(field.getKey(), resolveNode(field.getValue(), params, context));
            }
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            for (JsonNode child : node) result.add(resolveNode(child, params, context));
            return result;
        }
        return node;
    }

    private String resolveScalar(JsonNode node, Map<String, Object> params,
                                 Map<String, Object> context, String name) {
        if (node == null || node.isNull()) return "";
        if (!node.isValueNode()) throw new IllegalArgumentException("WEBHOOK " + name + " 必须是标量模板");
        JsonNode resolved = resolveNode(node, params, context);
        return resolved.isValueNode() ? resolved.asText() : resolved.toString();
    }

    private Object resolvePath(String rawPath, Map<String, Object> params,
                               Map<String, Object> context) {
        String path = rawPath == null ? "" : rawPath.trim();
        if (path.startsWith("param.")) return readPath(params, path.substring("param.".length()));
        if (path.startsWith("input.")) return readPath(params, path.substring("input.".length()));
        if (path.startsWith("context.")) return readPath(context, path.substring("context.".length()));
        Object value = readPath(params, path);
        if (value != null) return value;
        return readPath(context, path);
    }

    @SuppressWarnings("unchecked")
    private Object readPath(Map<String, Object> source, String path) {
        if (source == null || path == null || path.isEmpty()) return null;
        Object current = source;
        for (String part : path.split("\\.")) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else if (current instanceof List && part.matches("\\d+")) {
                int index = Integer.parseInt(part);
                List<?> list = (List<?>) current;
                current = index < list.size() ? list.get(index) : null;
            } else {
                return null;
            }
            if (current == null) return null;
        }
        return current;
    }

    private URI validateTargetUri(String rawUrl) {
        if (rawUrl == null || rawUrl.length() > MAX_URL_LENGTH || containsControl(rawUrl)) {
            throw new IllegalArgumentException("WEBHOOK url 不合法");
        }
        final URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("WEBHOOK url 不是合法 URI: " + e.getMessage(), e);
        }
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("WEBHOOK url 只允许 http/https，且不能包含用户信息或片段");
        }
        String host = normalizeHost(uri.getHost());
        int port = uri.getPort() < 0 ? ("https".equalsIgnoreCase(scheme) ? 443 : 80) : uri.getPort();
        if (!parseAllowedPorts().contains(port)) {
            throw new IllegalArgumentException("WEBHOOK 目标端口不在允许范围内: " + port);
        }
        List<String> hosts = parseAllowedHosts();
        if (!hosts.isEmpty() && !isAllowedHost(host, hosts)) {
            throw new IllegalArgumentException("WEBHOOK 目标 host 未加入允许白名单: " + host);
        }
        InetAddress[] addresses = resolvePublicAddresses(host);
        if (addresses.length == 0) throw new IllegalArgumentException("WEBHOOK host 无法解析: " + host);
        return uri;
    }

    private List<String> parseAllowedHosts() {
        if (allowedHosts == null || allowedHosts.trim().isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String value : allowedHosts.split(",")) {
            if (!value.trim().isEmpty()) result.add(normalizeHost(value.trim()));
        }
        return result;
    }

    private Set<Integer> parseAllowedPorts() {
        Set<Integer> result = new HashSet<>();
        String value = allowedPorts == null || allowedPorts.trim().isEmpty() ? "80,443" : allowedPorts;
        for (String token : value.split(",")) {
            try {
                int port = Integer.parseInt(token.trim());
                if (port > 0 && port <= 65535) result.add(port);
            } catch (NumberFormatException ignored) { }
        }
        if (result.isEmpty()) { result.add(80); result.add(443); }
        return result;
    }

    private boolean isAllowedHost(String host, List<String> allowed) {
        for (String item : allowed) {
            if (item.startsWith("*.")) {
                String suffix = item.substring(1);
                if (host.endsWith(suffix) && host.length() > suffix.length()) return true;
            } else if (host.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeHost(String host) {
        String value = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private InetAddress[] resolvePublicAddresses(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new IllegalArgumentException("WEBHOOK 目标解析到内网或保留地址: " + address.getHostAddress());
                }
            }
            return addresses;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("WEBHOOK host 解析失败: " + host, e);
        }
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) return true;
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address && bytes.length == 4) {
            int a = bytes[0] & 0xff, b = bytes[1] & 0xff;
            // 100.64/10、192.0.0/24、198.18/15、198.51.100/24、203.0.113/24 等保留/测试网段。
            if (a == 100 && b >= 64 && b <= 127) return true;
            if (a == 192 && b == 0) return true;
            if (a == 198 && b >= 18 && b <= 19) return true;
            if (a == 198 && b == 51) return true;
            if (a == 203 && b == 0) return true;
        }
        if (address instanceof Inet6Address && bytes.length == 16) {
            // IPv6 ULA fc00::/7。
            if ((bytes[0] & 0xfe) == 0xfc) return true;
            // IPv4-mapped IPv6 也必须按其 IPv4 地址重新判断，避免绕过回环/私网拦截。
            if (isIpv4Mapped(bytes)) {
                int a = bytes[12] & 0xff, b = bytes[13] & 0xff;
                int c = bytes[14] & 0xff, d = bytes[15] & 0xff;
                if (a == 10 || a == 127 || (a == 192 && b == 168)
                        || (a == 172 && b >= 16 && b <= 31)
                        || (a == 169 && b == 254)
                        || (a == 100 && b >= 64 && b <= 127)) return true;
                return a == 192 && b == 0 && c == 0;
            }
        }
        return false;
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        for (int i = 0; i < 10; i++) if (bytes[i] != 0) return false;
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    private static final class SafeDnsResolver implements DnsResolver {
        private final WebhookActionExecutor owner;

        private SafeDnsResolver(WebhookActionExecutor owner) { this.owner = owner; }

        @Override
        public InetAddress[] resolve(String host) {
            return owner.resolvePublicAddresses(owner.normalizeHost(host));
        }
    }

    public static final class PreparedRequest {
        private final URI uri;
        private final String method;
        private final Map<String, String> headers;
        private final String body;
        private final int timeoutMs;
        private final boolean idempotent;

        private PreparedRequest(URI uri, String method, Map<String, String> headers,
                                String body, int timeoutMs, boolean idempotent) {
            this.uri = uri;
            this.method = method;
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
            this.body = body;
            this.timeoutMs = timeoutMs;
            this.idempotent = idempotent;
        }

        public boolean isIdempotent() { return idempotent; }

        /** 用于审批预览/审计，绝不包含执行响应。 */
        public Map<String, Object> toAuditMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", ACTION_TYPE);
            map.put("method", method);
            map.put("url", uri.toString());
            Map<String, String> safeHeaders = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                safeHeaders.put(entry.getKey(), key.contains("authorization") || key.contains("token")
                        || key.contains("secret") || key.contains("signature") ? "***" : entry.getValue());
            }
            map.put("headers", safeHeaders);
            if (body != null) map.put("body", body);
            map.put("timeoutMs", timeoutMs);
            map.put("idempotent", idempotent);
            return map;
        }
    }

    public static final class WebhookResult {
        private final int status;
        private final String contentType;
        private final String body;
        private final long durationMs;

        private WebhookResult(int status, String contentType, String body, long durationMs) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
            this.durationMs = durationMs;
        }

        public Map<String, Object> toSummary() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("webhookStatus", status);
            if (contentType != null) result.put("webhookContentType", contentType);
            result.put("webhookResponseBody", body == null ? "" : body);
            result.put("webhookDurationMs", durationMs);
            return result;
        }
    }

    public static class WebhookCallException extends Exception {
        private final boolean unknownOutcome;
        private final int status;

        public WebhookCallException(String message, boolean unknownOutcome, int status) {
            super(message);
            this.unknownOutcome = unknownOutcome;
            this.status = status;
        }

        public WebhookCallException(String message, boolean unknownOutcome, int status, Throwable cause) {
            super(message, cause);
            this.unknownOutcome = unknownOutcome;
            this.status = status;
        }

        public boolean isUnknownOutcome() { return unknownOutcome; }
        public int getStatus() { return status; }
    }
}
