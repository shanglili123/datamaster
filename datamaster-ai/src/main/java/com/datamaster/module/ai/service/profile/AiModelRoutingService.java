package com.datamaster.module.ai.service.profile;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.config.AiModelRoutingProperties;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingSaveReqVO;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Read current policy per client creation; never cache credentials or call providers. */
@Service
public class AiModelRoutingService {
    private final JdbcTemplate jdbc;
    private final AiModelProfileService profiles;
    private final AiModelRoutingProperties defaults;

    public AiModelRoutingService(JdbcTemplate jdbc, AiModelProfileService profiles, AiModelRoutingProperties defaults) {
        this.jdbc = jdbc; this.profiles = profiles; this.defaults = defaults;
    }

    public AiModelRoutingRespVO get(Long spaceId, String spaceCode) {
        require(spaceId != null && spaceId > 0, "请先选择当前空间");
        try {
            List<AiModelRoutingRespVO> rows = jdbc.query("SELECT * FROM ai_model_routing WHERE space_id = ?", (rs, n) -> {
                AiModelRoutingRespVO value = new AiModelRoutingRespVO();
                value.setSpaceId(rs.getLong("space_id")); value.setSpaceCode(rs.getString("space_code"));
                value.setEnabled(rs.getBoolean("enabled"));
                value.setEconomyProfileId(rs.getObject("economy_profile_id", Long.class));
                value.setAdvancedProfileId(rs.getObject("advanced_profile_id", Long.class));
                value.setSemanticDecisionEnabled(rs.getBoolean("semantic_decision_enabled"));
                value.setDecisionProfileId(rs.getObject("decision_profile_id", Long.class));
                value.setStrictJsonSchema(rs.getBoolean("strict_json_schema"));
                value.setComplexInputCharacters(rs.getInt("complex_input_characters"));
                value.setRevision(rs.getInt("revision")); value.setSource("DATABASE"); return value;
            }, spaceId);
            if (!rows.isEmpty()) {
                AiModelRoutingRespVO value = rows.get(0);
                require(spaceCode == null || spaceCode.equals(value.getSpaceCode()), "路由配置空间标识不一致");
                return value;
            }
            return deployment(spaceId, spaceCode);
        } catch (DataAccessException ignored) {
            throw new ServiceException("模型路由存储不可用，请先执行ai_model_routing迁移");
        }
    }

    /** A disabled stored override must not fall back to an enabled YAML route. */
    public AiModelRoutingRespVO effective(Long spaceId) {
        return spaceId == null ? null : get(spaceId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiModelRoutingRespVO save(Long spaceId, String spaceCode, Long userId, AiModelRoutingSaveReqVO request) {
        validate(request);
        require(spaceId != null && spaceId.equals(request.getSpaceId()) && userId != null, "路由配置空间或用户无效");
        try {
            List<Long> ids = jdbc.query("SELECT id FROM tax_space WHERE id = ? AND del_flag = '0' FOR UPDATE", (rs, n) -> rs.getLong(1), spaceId);
            require(ids.size() == 1, "空间不存在或已删除");
            AiModelRoutingRespVO current = get(spaceId, spaceCode);
            require(request.getRevision().equals(current.getRevision()), "路由配置已修改，请刷新后重试");
            require(current.getRevision() < Integer.MAX_VALUE, "路由配置版本已达上限");
            if (request.getEnabled()) {
                require(request.getEconomyProfileId() != null && request.getAdvancedProfileId() != null, "启用路由必须选择经济模型和高级模型");
            }
            // Same-space/enabled/decryptable validation, no endpoint probe or model invocation.
            for (Long id : new java.util.LinkedHashSet<>(java.util.Arrays.asList(request.getEconomyProfileId(), request.getAdvancedProfileId(), request.getDecisionProfileId()))) {
                if (id != null) profiles.resolve(spaceId, id);
            }
            int changed;
            Object[] values = {spaceCode, request.getEnabled(), request.getEconomyProfileId(), request.getAdvancedProfileId(),
                    request.getSemanticDecisionEnabled(), request.getDecisionProfileId(), request.getStrictJsonSchema(),
                    request.getComplexInputCharacters(), userId, spaceId, current.getRevision()};
            if (current.getRevision() == 0) {
                changed = jdbc.update("INSERT INTO ai_model_routing (space_code,enabled,economy_profile_id,advanced_profile_id,semantic_decision_enabled,decision_profile_id,strict_json_schema,complex_input_characters,updated_by,space_id,revision) VALUES (?,?,?,?,?,?,?,?,?,?,1) ON CONFLICT(space_id) DO NOTHING",
                        java.util.Arrays.copyOf(values, 10));
            } else {
                changed = jdbc.update("UPDATE ai_model_routing SET space_code=?,enabled=?,economy_profile_id=?,advanced_profile_id=?,semantic_decision_enabled=?,decision_profile_id=?,strict_json_schema=?,complex_input_characters=?,updated_by=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE space_id=? AND revision=?", values);
            }
            require(changed == 1, "路由配置保存失败或已修改，请刷新");
            return get(spaceId, spaceCode);
        } catch (DataAccessException ignored) {
            throw new ServiceException("模型路由存储不可用，请确认迁移及模型引用有效");
        }
    }

    static void validate(AiModelRoutingSaveReqVO value) {
        require(value != null && value.getEnabled() != null && value.getSemanticDecisionEnabled() != null && value.getStrictJsonSchema() != null, "请明确路由开关状态");
        require(value.getRevision() != null && value.getRevision() >= 0, "缺少路由revision");
        require(value.getComplexInputCharacters() != null && value.getComplexInputCharacters() >= 1 && value.getComplexInputCharacters() <= 16000, "复杂输入阈值必须在1到16000之间");
        for (Long id : java.util.Arrays.asList(value.getEconomyProfileId(), value.getAdvancedProfileId(), value.getDecisionProfileId()))
            require(id == null || id > 0, "模型配置ID无效");
    }

    private AiModelRoutingRespVO deployment(Long spaceId, String spaceCode) {
        AiModelRoutingRespVO value = new AiModelRoutingRespVO();
        AiModelRoutingProperties.SpaceRoutes route = defaults.getSpaces().get(spaceId);
        value.setSpaceId(spaceId); value.setSpaceCode(spaceCode); value.setRevision(0); value.setSource("DEPLOYMENT");
        value.setEnabled(defaults.isEnabled() && route != null);
        value.setComplexInputCharacters(defaults.getComplexInputCharacters());
        value.setSemanticDecisionEnabled(route != null && route.isSemanticDecisionEnabled());
        value.setStrictJsonSchema(route != null && route.isStrictJsonSchema());
        if (route != null) {
            value.setEconomyProfileId(route.getEconomyProfileId()); value.setAdvancedProfileId(route.getAdvancedProfileId()); value.setDecisionProfileId(route.getDecisionProfileId());
        }
        return value;
    }
    private static void require(boolean ok, String message) { if (!ok) throw new ServiceException(message); }
}
