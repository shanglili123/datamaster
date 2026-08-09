package com.datamaster.module.service.config.auth;

import cn.hutool.core.util.IdUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 客户端 Token 服务（替代 Sa-Token OAuth2 client_credentials）
 * <p>
 * 使用 Redis 管理 Token 的生成、校验和撤销。
 *
 * @author DATAMASTER
 */
@Component
public class ServiceTokenService {

    private static final String TOKEN_KEY_PREFIX = "service:client_token:";

    /** Token 默认有效期（秒），与原 Sa-Token 配置一致：2 小时 */
    private static final long TOKEN_EXPIRE_SECONDS = 7200;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成客户端 Token 并存储到 Redis
     *
     * @param clientId 客户端 ID
     * @return 生成的 Token
     */
    public String generateToken(String clientId) {
        String token = IdUtil.fastSimpleUUID();
        String key = TOKEN_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, clientId, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return token;
    }

    /**
     * 校验 Token 是否有效，有效则返回对应的 clientId
     *
     * @param token 客户端 Token
     * @return clientId，无效时返回 null
     */
    public String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String key = TOKEN_KEY_PREFIX + token;
        Object clientId = redisTemplate.opsForValue().get(key);
        return clientId != null ? clientId.toString() : null;
    }

    /**
     * 撤销 Token
     *
     * @param token 客户端 Token
     */
    public void revokeToken(String token) {
        if (token != null && !token.isEmpty()) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        }
    }
}
