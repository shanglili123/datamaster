package com.datamaster.module.ai.service.dbgpt.impl;

import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ai.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.ai.dal.mapper.skill.AiSkillMapper;
import com.datamaster.module.ai.service.dbgpt.IDbGptClientService;
import com.datamaster.module.ai.service.dbgpt.IDbGptSkillSyncService;
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

    @Override
    public AjaxResult syncAllSkills() {
        int success = 0;
        int failed = 0;
        List<AiSkillDO> skills = aiSkillMapper.selectPublishedSkills();
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
        try {
            syncSkill(skill);
        } catch (Exception e) {
            markFailed(skill, e.getMessage());
            throw e;
        }
        return AjaxResult.success("Skill已上传到DB-GPT Skill库：" + skill.getDbgptDocumentName());
    }

    private void syncSkill(AiSkillDO skill) {
        String fileName = skill.getSkillCode() + "-v" + (skill.getVersion() == null ? 1 : skill.getVersion()) + ".md";
        String content = buildDocument(skill);
        String dbgptSkillId = dbGptClientService.uploadSkill(fileName, content);
        if (StringUtils.isBlank(dbgptSkillId)) {
            throw new ServiceException("DB-GPT Skill上传后未返回标识：" + fileName);
        }
        skill.setDbgptSpaceName("DB-GPT Skill");
        skill.setDbgptDocumentName(skill.getSkillCode());
        skill.setDbgptSyncStatus("SYNCED");
        skill.setDbgptSyncMessage("Skill上传成功，file=" + fileName + "，skillId=" + dbgptSkillId);
        aiSkillMapper.updateById(skill);
    }

    private String buildDocument(AiSkillDO skill) {
        return StringUtils.defaultString(skill.getContent());
    }

    private void markFailed(AiSkillDO skill, String message) {
        skill.setDbgptSyncStatus("FAILED");
        skill.setDbgptSyncMessage(message);
        aiSkillMapper.updateById(skill);
    }
}

