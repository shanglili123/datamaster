package com.datamaster.common.httpClient;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** 可独立运行的 Java 8 回归测试，不依赖外部 DolphinScheduler。 */
public class DsRequestUtilsTest {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> uri = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        server.createContext("/workflow-definition", exchange -> {
            method.set(exchange.getRequestMethod()); uri.set(exchange.getRequestURI().toString());
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(read(exchange.getRequestBody()));
            respond(exchange, 200, "{\"code\":0}");
        });
        server.createContext("/error", exchange -> respond(exchange, 414,
                "<h1>Bad Message</h1>password=regression-secret"));
        server.start();
        try {
            DsRequestUtils client = new DsRequestUtils();
            client.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort()); client.setToken("test-only");
            String largeJson = new String(new char[100000]).replace('\0', 'x');
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("name", "多节点 A+B & JOIN = 中文"); params.put("taskDefinitionJson", largeJson);
            for (String verb : new String[]{"POST", "PUT"}) {
                DsRequestUtils.requestForm("/workflow-definition", verb, params, JSONObject.class);
                check(verb.equals(method.get()), "method");
                check("/workflow-definition".equals(uri.get()), "payload must not appear in URI");
                check(contentType.get().startsWith("application/x-www-form-urlencoded"), "form content type");
                String decoded = URLDecoder.decode(body.get(), "UTF-8");
                check(decoded.contains("name=多节点 A+B & JOIN = 中文"), "unicode and special characters");
                check(decoded.contains("taskDefinitionJson=" + largeJson), "full large JSON body");
                System.out.println("PASS " + verb + " large form body; URI length=" + uri.get().length());
            }
            params.put("taskDefinitionJson", "small-query-value");
            DsRequestUtils.requestForm("/workflow-definition", "GET", params, JSONObject.class);
            check(uri.get().contains("taskDefinitionJson="), "GET query compatibility");
            check(body.get().isEmpty(), "GET has no form body");
            System.out.println("PASS GET query compatibility");
            try {
                DsRequestUtils.requestForm("/error?password=regression-secret", "POST", params, JSONObject.class);
                throw new AssertionError("expected HTTP failure");
            } catch (ServiceException ex) {
                check(ex.getMessage().contains("414"), "status remains visible");
                check(!ex.getMessage().contains("regression-secret"), "error credentials redacted");
                check(!ex.getMessage().contains(largeJson), "task payload hidden");
                System.out.println("PASS error URL and response redaction");
            }
        } finally {
            server.stop(0);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static String read(InputStream stream) throws java.io.IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int n;
        while ((n = stream.read(buffer)) != -1) bytes.write(buffer, 0, n);
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
    private static void respond(HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    }
}
