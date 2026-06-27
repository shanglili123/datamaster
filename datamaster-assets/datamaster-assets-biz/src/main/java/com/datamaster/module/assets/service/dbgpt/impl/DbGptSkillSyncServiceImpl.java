package com.datamaster.module.assets.service.dbgpt.impl;

import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.config.DbGptProperties;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillMapper;
import com.datamaster.module.assets.service.dbgpt.IDbGptClientService;
import com.datamaster.module.assets.service.dbgpt.IDbGptSkillSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class DbGptSkillSyncServiceImpl implements IDbGptSkillSyncService {

    @Resource
    private AiSkillMapper aiSkillMapper;
    @Resource
    private IDbGptClientService dbGptClientService;
    @Resource
    private DbGptProperties dbGptProperties;

    @Override
    public AjaxResult syncAllSkills() {
        ensureSkillSpace();
        int success = 0;
        int failed = 0;
        List<AiSkillDO> skills = aiSkillMapper.selectPublishedTableSkills();
        for (AiSkillDO skill : skills) {
            try {
                syncSkill(skill);
                success++;
            } catch (Exception e) {
                failed++;
                markFailed(skill, e.getMessage());
            }
        }
        return AjaxResult.success("DB-GPT Skill同步完成，成功 " + success + " 个，失败 " + failed + " 个");
    }

    @Override
    public AjaxResult syncSkillById(Long skillId) {
        AiSkillDO skill = aiSkillMapper.selectById(skillId);
        if (skill == null) {
            throw new ServiceException("Skill不存在");
        }
        if (!"PUBLISHED".equals(skill.getStatus())) {
            throw new ServiceException("只有已发布Skill可以同步到DB-GPT");
        }
        ensureSkillSpace();
        syncSkill(skill);
        return AjaxResult.success("Skill已同步到DB-GPT知识库：" + skill.getDbgptDocumentName());
    }

    private String ensureSkillSpace() {
        String spaceName = dbGptProperties.getSkillSpaceName();
        // 先尝试获取已有空间
        try {
            String spaceId = dbGptClientService.getKnowledgeSpaceId(spaceName);
            if (spaceId != null) {
                return spaceId;
            }
        } catch (Exception e) {
            log.warn("获取知识空间失败，将尝试创建: {}", e.getMessage());
        }
        // 不存在则创建
        try {
            String spaceId = dbGptClientService.createKnowledgeSpace(spaceName, "Chroma", dbGptProperties.getSkillSpaceOwner());
            if (spaceId != null) {
                return spaceId;
            }
        } catch (Exception e) {
            log.warn("创建知识空间失败: {}", e.getMessage());
        }
        // 创建后重试获取（可能创建实际成功但响应解析失败）
        try {
            String spaceId = dbGptClientService.getKnowledgeSpaceId(spaceName);
            if (spaceId != null) {
                return spaceId;
            }
        } catch (Exception e) {
            log.warn("重试获取知识空间失败: {}", e.getMessage());
        }
        throw new ServiceException("无法获取或创建DB-GPT知识空间: " + spaceName);
    }

    private void syncSkill(AiSkillDO skill) {
        String spaceId = ensureSkillSpace();
        String fileName = skill.getSkillCode() + "-v" + (skill.getVersion() == null ? 1 : skill.getVersion()) + ".md";
        String content = buildDocument(skill);
        String docId = dbGptClientService.uploadDocumentToKnowledge(spaceId, fileName, content);
        if (docId != null) {
            try {
                dbGptClientService.syncDocument(spaceId, docId);
            } catch (Exception e) {
                log.warn("文档向量化同步触发失败（文档已上传，稍后可在DB-GPT手动同步）: {}", e.getMessage());
            }
        }
        skill.setDbgptSpaceName(dbGptProperties.getSkillSpaceName());
        skill.setDbgptDocumentName(fileName);
        skill.setDbgptSyncStatus("SYNCED");
        skill.setDbgptSyncMessage("同步成功");
        aiSkillMapper.updateById(skill);
    }

    private String buildDocument(AiSkillDO skill) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(skill.getSkillName()).append("\n\n");
        builder.append("- Skill编码：").append(skill.getSkillCode()).append("\n");
        builder.append("- Skill类型：").append(skill.getSkillType()).append("\n");
        builder.append("- 关联对象：").append(StringUtils.defaultString(skill.getBizObjectType()))
                .append(" ").append(skill.getBizObjectId() == null ? "" : skill.getBizObjectId()).append("\n\n");
        builder.append(skill.getContent());
        return builder.toString();
    }

    private void markFailed(AiSkillDO skill, String message) {
        skill.setDbgptSyncStatus("FAILED");
        skill.setDbgptSyncMessage(message);
        aiSkillMapper.updateById(skill);
    }
}
