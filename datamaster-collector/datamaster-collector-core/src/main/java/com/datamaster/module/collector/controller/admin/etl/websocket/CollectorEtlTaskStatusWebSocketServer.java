package com.datamaster.module.collector.controller.admin.etl.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.utils.spring.SpringUtils;
import com.datamaster.security.web.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
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

    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final int MAX_AUTH_FRAME_LENGTH = 8192;
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<Session, AuthenticatedSession> AUTHENTICATED_SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        String key = buildKey(userId, session.getId());
        SESSIONS.put(key, session);
        log.info("ETL任务状态WebSocket连接成功，key={}", key);
    }

    @OnMessage
    public void onMessage(String frame, Session session, @PathParam("userId") String userId) {
        synchronized (session) {
            try {
                if (frame == null || frame.length() > MAX_AUTH_FRAME_LENGTH) {
                    closeUnauthenticated(session);
                    return;
                }
                JSONObject message = JSON.parseObject(frame);
                if (message == null || !"authenticate".equals(message.get("type"))) {
                    closeUnauthenticated(session);
                    return;
                }
                Object suppliedToken = message.get("token");
                if (!(suppliedToken instanceof String) || ((String) suppliedToken).isEmpty()
                        || ((String) suppliedToken).length() > MAX_TOKEN_LENGTH) {
                    closeUnauthenticated(session);
                    return;
                }
                String token = (String) suppliedToken;
                LoginUser loginUser = SpringUtils.getBean(TokenService.class).getLoginUserByToken(token);
                if (loginUser == null || loginUser.getUserId() == null
                        || !loginUser.getUserId().toString().equals(userId)) {
                    closeUnauthenticated(session);
                    return;
                }
                if (session.isOpen() && SESSIONS.get(buildKey(userId, session.getId())) == session) {
                    AUTHENTICATED_SESSIONS.put(session, new AuthenticatedSession(token, loginUser.getUserId()));
                }
            } catch (Exception ignored) {
                closeUnauthenticated(session);
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        synchronized (session) {
            AUTHENTICATED_SESSIONS.remove(session);
            SESSIONS.remove(buildKey(userId, session.getId()));
        }
        log.info("ETL任务状态WebSocket连接关闭，key={}", buildKey(userId, session.getId()));
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("userId") String userId) throws IOException {
        if (session == null) {
            return;
        }
        String key = buildKey(userId, session.getId());
        synchronized (session) {
            AUTHENTICATED_SESSIONS.remove(session);
            SESSIONS.remove(key);
            if (session.isOpen()) {
                session.close();
            }
        }
        // Do not log parser/container exceptions, which may contain an authentication frame.
        log.warn("ETL任务状态WebSocket连接异常，key={}", key);
    }

    /** Legacy events retain their existing unauthenticated broadcast behavior. */
    public static void broadcast(CollectorEtlTaskStatusMessage message) {
        String payload = JSON.toJSONString(message);
        for (Session session : SESSIONS.values()) {
            synchronized (session) {
                if (!session.isOpen()) {
                    continue;
                }
                try {
                    session.getBasicRemote().sendText(payload);
                } catch (IOException e) {
                    log.warn("推送ETL任务状态失败，sessionId={}", session.getId(), e);
                }
            }
        }
    }

    /** Component events require a current login and explicit membership in the event's space. */
    public static void broadcastComponent(CollectorEtlTaskStatusMessage message) {
        if (message == null || message.getSpaceId() == null || message.getSpaceCode() == null
                || message.getSpaceCode().trim().isEmpty()) {
            return;
        }
        String payload = JSON.toJSONString(message);
        for (Session session : AUTHENTICATED_SESSIONS.keySet()) {
            synchronized (session) {
                AuthenticatedSession authenticated = AUTHENTICATED_SESSIONS.get(session);
                if (authenticated == null) {
                    continue;
                }
                if (!session.isOpen()) {
                    AUTHENTICATED_SESSIONS.remove(session);
                    continue;
                }
                try {
                    LoginUser loginUser = SpringUtils.getBean(TokenService.class)
                            .getLoginUserByToken(authenticated.token);
                    if (loginUser == null || !authenticated.userId.equals(loginUser.getUserId())) {
                        closeUnauthenticated(session);
                        continue;
                    }
                    if (!SpringUtils.getBean(ISpaceApi.class).hasUserSpace(loginUser.getUserId(),
                            message.getSpaceId(), message.getSpaceCode())) {
                        continue;
                    }
                    session.getBasicRemote().sendText(payload);
                } catch (Exception ignored) {
                    // Redis, membership lookup and send failures must never allow delivery.
                    closeUnauthenticated(session);
                }
            }
        }
    }

    private static void closeUnauthenticated(Session session) {
        AUTHENTICATED_SESSIONS.remove(session);
        SESSIONS.values().remove(session);
        try {
            if (session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication required"));
            }
        } catch (IOException ignored) {
            // Keep failures sanitized and leave the session excluded from component broadcasts.
        }
    }

    private static String buildKey(String userId, String sessionId) {
        return userId + "_" + sessionId;
    }

    private static final class AuthenticatedSession {
        private final String token;
        private final Long userId;

        private AuthenticatedSession(String token, Long userId) {
            this.token = token;
            this.userId = userId;
        }
    }
}
