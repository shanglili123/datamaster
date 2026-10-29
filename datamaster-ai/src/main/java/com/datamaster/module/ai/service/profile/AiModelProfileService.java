package com.datamaster.module.ai.service.profile;

import com.alibaba.fastjson2.annotation.JSONField;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileAvailableRespVO;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileRespVO;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileSaveReqVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** OpenAI-compatible MODEL endpoints only; does not reconfigure DB-GPT's independent model service. */
@Service
public class AiModelProfileService {
    private static final String SUMMARY = "id, space_id, space_code, name, provider, model, api_url, enabled, active, "
            + "(api_key_ciphertext IS NOT NULL AND api_key_ciphertext <> '') AS has_api_key";
    private final JdbcTemplate jdbc;
    private final AiModelProfileCipher cipher;

    public AiModelProfileService(JdbcTemplate jdbc, AiModelProfileCipher cipher) {
        this.jdbc = jdbc;
        this.cipher = cipher;
    }

    public List<AiModelProfileRespVO> list(Long spaceId) {
        requireSpace(spaceId);
        try {
            return jdbc.query("SELECT " + SUMMARY + " FROM ai_model_profile WHERE space_id = ? ORDER BY active DESC, id", SUMMARY_MAPPER, spaceId);
        } catch (DataAccessException ignored) { throw storageError(); }
    }

    public List<AiModelProfileAvailableRespVO> available(Long spaceId) {
        requireSpace(spaceId);
        try {
            return jdbc.query("SELECT id, name, provider, model FROM ai_model_profile WHERE space_id = ? AND enabled = TRUE ORDER BY active DESC, id",
                    (rs, n) -> new AiModelProfileAvailableRespVO(rs.getLong("id"), rs.getString("name"), rs.getString("provider"), rs.getString("model")), spaceId);
        } catch (DataAccessException ignored) { throw storageError(); }
    }

    @Transactional
    public AiModelProfileRespVO create(Long spaceId, String spaceCode, AiModelProfileSaveReqVO request) {
        validate(request);
        if (blank(request.getApiKey())) throw new ServiceException("新模型配置必须提供API Key");
        try {
            lockSpace(spaceId);
            String encrypted = cipher.encrypt(request.getApiKey().trim(), spaceId);
            Long id = jdbc.queryForObject("INSERT INTO ai_model_profile (space_id, space_code, name, provider, model, api_url, enabled, active, api_key_ciphertext) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, ?) RETURNING id", Long.class,
                    spaceId, spaceCode, request.getName().trim(), request.getProvider().trim(), request.getModel().trim(),
                    request.getApiUrl().trim(), request.getEnabled(), encrypted);
            return requireProfile(id, spaceId);
        } catch (DuplicateKeyException ignored) { throw new ServiceException("当前空间已存在同名模型配置"); }
        catch (DataAccessException ignored) { throw storageError(); }
    }

    @Transactional
    public AiModelProfileRespVO update(Long id, Long spaceId, String spaceCode, AiModelProfileSaveReqVO request) {
        validate(request);
        try {
            lockSpace(spaceId);
            AiModelProfileRespVO existing = requireProfile(id, spaceId);
            if (Boolean.TRUE.equals(existing.getActive()) && !Boolean.TRUE.equals(request.getEnabled()))
                throw new ServiceException("不能停用当前默认模型，请先启用其他默认模型");
            // Never copy request/entity objects. A blank token updates metadata only.
            jdbc.update("UPDATE ai_model_profile SET space_code = ?, name = ?, provider = ?, model = ?, api_url = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND space_id = ?",
                    spaceCode, request.getName().trim(), request.getProvider().trim(), request.getModel().trim(),
                    request.getApiUrl().trim(), request.getEnabled(), id, spaceId);
            if (!blank(request.getApiKey()))
                jdbc.update("UPDATE ai_model_profile SET api_key_ciphertext = ? WHERE id = ? AND space_id = ?",
                        cipher.encrypt(request.getApiKey().trim(), spaceId), id, spaceId);
            return requireProfile(id, spaceId);
        } catch (DuplicateKeyException ignored) { throw new ServiceException("当前空间已存在同名模型配置"); }
        catch (DataAccessException ignored) { throw storageError(); }
    }

    @Transactional
    public AiModelProfileRespVO activate(Long id, Long spaceId) {
        try {
            // Lock the stable space row, including the first activation (there may be no active profile row).
            lockSpace(spaceId);
            AiModelProfileRespVO target = requireProfile(id, spaceId);
            if (!Boolean.TRUE.equals(target.getEnabled()) || !Boolean.TRUE.equals(target.getHasApiKey()))
                throw new ServiceException("只能启用已启用且配置凭据的模型作为默认模型");
            // Verify the key before switching defaults; failures must not silently fall back to another provider.
            resolve(spaceId, id);
            jdbc.update("UPDATE ai_model_profile SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE space_id = ? AND active = TRUE", spaceId);
            jdbc.update("UPDATE ai_model_profile SET active = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND space_id = ?", id, spaceId);
            return requireProfile(id, spaceId);
        } catch (DataAccessException ignored) { throw storageError(); }
    }

    @Transactional
    public void delete(Long id, Long spaceId) {
        try {
            lockSpace(spaceId);
            if (Boolean.TRUE.equals(requireProfile(id, spaceId).getActive()))
                throw new ServiceException("不能删除当前默认模型，请先启用其他默认模型");
            jdbc.update("DELETE FROM ai_model_profile WHERE id = ? AND space_id = ? AND active = FALSE", id, spaceId);
        } catch (DataAccessException ignored) { throw storageError(); }
    }

    public Resolved resolve(Long spaceId) { return resolve(spaceId, null); }

    /** Caller must supply a space validated on the authenticated request thread, not a model-generated scope. */
    public Resolved resolve(Long spaceId, Long profileId) {
        if (spaceId == null) return null;
        requireSpace(spaceId);
        if (profileId != null && profileId <= 0) throw new ServiceException("模型配置无效");
        try {
            List<Resolved> rows = jdbc.query("SELECT id, provider, model, api_url, api_key_ciphertext, enabled FROM ai_model_profile WHERE space_id = ? AND "
                            + (profileId == null ? "active = TRUE" : "id = ?"),
                    (rs, n) -> {
                        if (!rs.getBoolean("enabled")) throw new ServiceException("模型配置未启用");
                        return new Resolved(rs.getLong("id"), rs.getString("provider"), rs.getString("model"), rs.getString("api_url"),
                                cipher.decrypt(rs.getString("api_key_ciphertext"), spaceId), true);
                    }, profileId == null ? new Object[]{spaceId} : new Object[]{spaceId, profileId});
            if (rows.size() > 1) throw new ServiceException("当前空间默认模型配置冲突");
            if (rows.isEmpty()) {
                if (profileId != null) throw new ServiceException("当前空间没有可用的指定模型配置");
                return null;
            }
            return rows.get(0);
        } catch (DataAccessException ignored) { throw storageError(); }
    }

    private void lockSpace(Long spaceId) {
        requireSpace(spaceId);
        List<Long> ids = jdbc.query("SELECT id FROM tax_space WHERE id = ? AND del_flag = '0' FOR UPDATE", (rs, n) -> rs.getLong(1), spaceId);
        if (ids.isEmpty()) throw new ServiceException("空间不存在或已删除");
    }

    private AiModelProfileRespVO requireProfile(Long id, Long spaceId) {
        if (id == null || id <= 0) throw new ServiceException("模型配置无效");
        List<AiModelProfileRespVO> rows = jdbc.query("SELECT " + SUMMARY + " FROM ai_model_profile WHERE id = ? AND space_id = ?", SUMMARY_MAPPER, id, spaceId);
        if (rows.isEmpty()) throw new ServiceException("当前空间没有该模型配置");
        return rows.get(0);
    }

    static void validate(AiModelProfileSaveReqVO request) {
        if (request == null) throw new ServiceException("模型配置不能为空");
        if (blank(request.getName()) || request.getName().length() > 100 || hasControls(request.getName())) throw new ServiceException("模型名称无效");
        if (request.getProvider() == null || !request.getProvider().trim().matches("[A-Z][A-Z0-9_]{0,63}")) throw new ServiceException("供应商编码无效");
        if (request.getModel() == null || !request.getModel().trim().matches("[A-Za-z0-9][A-Za-z0-9._:/@+\\-]{0,99}")) throw new ServiceException("模型标识无效");
        if (request.getEnabled() == null) throw new ServiceException("请指定模型启用状态");
        if (request.getApiKey() != null && (request.getApiKey().length() > 8192 || hasControls(request.getApiKey()))) throw new ServiceException("API Key格式无效");
        validateUrl(request.getApiUrl());
    }

    static void validateUrl(String url) {
        try {
            if (blank(url) || url.length() > 500 || hasControls(url)) throw new IllegalArgumentException();
            URI uri = new URI(url.trim());
            if ((!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getPort() < -1 || uri.getPort() == 0 || uri.getPort() > 65535) throw new IllegalArgumentException();
        } catch (Exception ignored) { throw new ServiceException("API地址必须为不含账号、查询参数或片段的HTTP(S)地址"); }
    }

    private static boolean hasControls(String value) {
        for (int i = 0; i < value.length(); i++) if (Character.isISOControl(value.charAt(i))) return true;
        return false;
    }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static void requireSpace(Long spaceId) { if (spaceId == null || spaceId <= 0) throw new ServiceException("请提供有效空间"); }
    private static ServiceException storageError() { return new ServiceException("模型配置存储不可用，请确认已部署ai_model_profile迁移"); }

    private static final RowMapper<AiModelProfileRespVO> SUMMARY_MAPPER = new RowMapper<AiModelProfileRespVO>() {
        @Override public AiModelProfileRespVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            AiModelProfileRespVO result = new AiModelProfileRespVO();
            result.setId(rs.getLong("id")); result.setSpaceId(rs.getLong("space_id")); result.setSpaceCode(rs.getString("space_code"));
            result.setName(rs.getString("name")); result.setProvider(rs.getString("provider")); result.setModel(rs.getString("model"));
            result.setApiUrl(rs.getString("api_url")); result.setEnabled(rs.getBoolean("enabled")); result.setActive(rs.getBoolean("active"));
            result.setHasApiKey(rs.getBoolean("has_api_key"));
            return result;
        }
    };

    /** Internal-only credentials. No toString, no bean-copy, never return this object from a controller. */
    public static final class Resolved {
        private final Long id;
        private final String provider;
        private final String model;
        private final String apiUrl;
        private final String apiKey;
        private final boolean enabled;
        private Resolved(Long id, String provider, String model, String apiUrl, String apiKey, boolean enabled) {
            this.id = id; this.provider = provider; this.model = model; this.apiUrl = apiUrl; this.apiKey = apiKey; this.enabled = enabled;
        }
        public Long getId() { return id; }
        public String getProvider() { return provider; }
        public String getModel() { return model; }
        public String getApiUrl() { return apiUrl; }
        @JsonIgnore @JSONField(serialize = false)
        public String getApiKey() { return apiKey; }
        public boolean getEnabled() { return enabled; }
    }
}
