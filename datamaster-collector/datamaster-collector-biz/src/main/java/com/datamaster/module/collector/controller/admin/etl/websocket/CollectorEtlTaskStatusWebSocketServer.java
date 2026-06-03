package com.datamaster.module.collector.controller.admin.etl.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket channel for ETL task status updates.
 */
@Slf4j
@Component
@ServerEndpoint("/websocket/etlTask/{userId}")
public class CollectorEtlTaskStatusWebSocketServer {

    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        String key = buildKey(userId, session.getId());
        SESSIONS.put(key, session);
        log.info("ETL任务状态WebSocket连接成功，key={}", key);
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        String key = buildKey(userId, session.getId());
        SESSIONS.remove(key);
        log.info("ETL任务状态WebSocket连接关闭，key={}", key);
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("userId") String userId) throws IOException {
        String key = buildKey(userId, session.getId());
        SESSIONS.remove(key);
        if (session != null && session.isOpen()) {
            session.close();
        }
        log.warn("ETL任务状态WebSocket连接异常，key={}", key, throwable);
    }

    public static void broadcast(CollectorEtlTaskStatusMessage message) {
        String payload = JSON.toJSONString(message);
        for (Session session : SESSIONS.values()) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.getBasicRemote().sendText(payload);
            } catch (IOException e) {
                log.warn("推送ETL任务状态失败，sessionId={}", session.getId(), e);
            }
        }
    }

    private static String buildKey(String userId, String sessionId) {
        return userId + "_" + sessionId;
    }
}
