package com.datamaster.module.service.controller.admin.auth;

import com.datamaster.common.annotation.Anonymous;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.module.service.config.auth.ServiceTokenService;
import com.datamaster.common.api.client.ClientApi;
import com.datamaster.common.api.client.dto.ClientRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Service模块 OAuth2 Client Credentials 鉴权控制器
 * <p>
 * 替代原 Sa-Token OAuth2 Server，提供 /oauth2/client_token 端点。
 *
 * @author Ming
 */
@Tag(name = "API服务-鉴权")
@RestController
@Slf4j
public class ServiceAuthController {

    private static final String CLIENT_CREDENTIALS = "client_credentials";

    @Resource
    private ClientApi clientApi;

    @Resource
    private ServiceTokenService serviceTokenService;

    /**
     * 处理 OAuth2 Client Credentials 请求
     */
    @Anonymous
    @Operation(
            summary = "应用鉴权接口",
            description = "该接口用于验证应用的身份，确保接口调用者具备足够的权限进行操作。"
    )
    @PostMapping("/oauth2/client_token")
    public Object request(
            @RequestParam(name = "grant_type") String grantType,
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "client_secret") String clientSecret
    ) {
        log.info("OAuth2 client_token request: clientId={}, grantType={}", clientId, grantType);

        if (!CLIENT_CREDENTIALS.equals(grantType)) {
            return AjaxResult.error("grant_type 仅支持 client_credentials");
        }

        ClientRespDTO client;
        try {
            client = clientApi.getClient(Long.parseLong(clientId));
        } catch (NumberFormatException e) {
            return AjaxResult.error("client_id 格式不正确");
        }
        if (client == null) {
            return AjaxResult.error("client_id 不存在");
        }
        if (!Boolean.TRUE.equals(client.getValidFlag())) {
            return AjaxResult.error("client 已被禁用");
        }
        if (!clientSecret.equals(client.getSecret())) {
            return AjaxResult.error("client_secret 不正确");
        }

        String token = serviceTokenService.generateToken(clientId);
        return AjaxResult.success("获取成功", token);
    }
}
