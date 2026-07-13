package com.datamaster.module.assets.service.skill.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillReportTemplateRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillReportTemplateSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillReportTemplateDO;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillMapper;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillReportTemplateMapper;
import com.datamaster.module.assets.service.skill.IAiSkillReportTemplateService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * AI Skill report template service implementation.
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AiSkillReportTemplateServiceImpl implements IAiSkillReportTemplateService {

    private static final String STATUS_DRAFT = "DRAFT";

    @Resource
    private AiSkillMapper aiSkillMapper;
    @Resource
    private AiSkillReportTemplateMapper reportTemplateMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<AiSkillReportTemplateRespVO> listBySkillId(Long skillId) {
        requireSkill(skillId);
        if (!reportTemplateTableExists()) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(reportTemplateMapper.selectListBySkillId(skillId), AiSkillReportTemplateRespVO.class);
    }

    @Override
    public AiSkillReportTemplateRespVO getTemplate(Long skillId, Long templateId) {
        ensureReportTemplateTableReady();
        return BeanUtils.toBean(requireTemplate(skillId, templateId), AiSkillReportTemplateRespVO.class);
    }

    @Override
    public Long createTemplate(Long skillId, AiSkillReportTemplateSaveReqVO reqVO) {
        requireSkill(skillId);
        ensureReportTemplateTableReady();
        validateTemplateContent(reqVO.getTemplateContent());
        if (selectBySkillIdAndCodeReady(skillId, reqVO.getTemplateCode()) != null) {
            throw new ServiceException("当前Skill下模板编码已存在");
        }
        AiSkillReportTemplateDO template = BeanUtils.toBean(reqVO, AiSkillReportTemplateDO.class);
        template.setSkillId(skillId);
        template.setStatus(defaultText(reqVO.getStatus(), STATUS_DRAFT));
        template.setDefaultFlag(Boolean.TRUE.equals(reqVO.getDefaultFlag()));
        template.setVersion(1);
        template.setDelFlag(Boolean.FALSE);
        if (Boolean.TRUE.equals(template.getDefaultFlag())) {
            clearDefault(skillId);
        }
        reportTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public Integer updateTemplate(Long skillId, Long templateId, AiSkillReportTemplateSaveReqVO reqVO) {
        ensureReportTemplateTableReady();
        AiSkillReportTemplateDO old = requireTemplate(skillId, templateId);
        validateTemplateContent(reqVO.getTemplateContent());
        AiSkillReportTemplateDO sameCode = selectBySkillIdAndCodeReady(skillId, reqVO.getTemplateCode());
        if (sameCode != null && !sameCode.getId().equals(templateId)) {
            throw new ServiceException("当前Skill下模板编码已存在");
        }
        AiSkillReportTemplateDO template = BeanUtils.toBean(reqVO, AiSkillReportTemplateDO.class);
        template.setId(templateId);
        template.setSkillId(skillId);
        template.setStatus(defaultText(reqVO.getStatus(), old.getStatus()));
        template.setDefaultFlag(Boolean.TRUE.equals(reqVO.getDefaultFlag()));
        template.setVersion(old.getVersion() == null ? 1 : old.getVersion() + 1);
        if (Boolean.TRUE.equals(template.getDefaultFlag())) {
            clearDefault(skillId);
        }
        return reportTemplateMapper.updateById(template);
    }

    @Override
    public Integer deleteTemplate(Long skillId, Long templateId) {
        ensureReportTemplateTableReady();
        requireTemplate(skillId, templateId);
        return reportTemplateMapper.deleteById(templateId);
    }

    @Override
    public Integer setDefaultTemplate(Long skillId, Long templateId) {
        ensureReportTemplateTableReady();
        AiSkillReportTemplateDO template = requireTemplate(skillId, templateId);
        clearDefault(skillId);
        template.setDefaultFlag(Boolean.TRUE);
        return reportTemplateMapper.updateById(template);
    }

    private AiSkillDO requireSkill(Long skillId) {
        if (skillId == null) {
            throw new ServiceException("Skill ID不能为空");
        }
        AiSkillDO skill = aiSkillMapper.selectById(skillId);
        if (skill == null) {
            throw new ServiceException("Skill不存在");
        }
        return skill;
    }

    private AiSkillReportTemplateDO requireTemplate(Long skillId, Long templateId) {
        requireSkill(skillId);
        if (templateId == null) {
            throw new ServiceException("模板ID不能为空");
        }
        AiSkillReportTemplateDO template = reportTemplateMapper.selectById(templateId);
        if (template == null || !skillId.equals(template.getSkillId())) {
            throw new ServiceException("报告模板不存在");
        }
        return template;
    }

    private void validateTemplateContent(String templateContent) {
        if (StringUtils.isBlank(templateContent)) {
            throw new ServiceException("模板内容不能为空");
        }
        JSONObject object;
        try {
            object = JSON.parseObject(templateContent);
        } catch (Exception e) {
            throw new ServiceException("模板内容必须是合法JSON");
        }
        if (object.getJSONObject("dataSchema") == null) {
            throw new ServiceException("模板缺少dataSchema");
        }
        if (object.getJSONObject("layout") == null) {
            throw new ServiceException("模板缺少layout");
        }
        if (object.getJSONObject("style") == null) {
            throw new ServiceException("模板缺少style");
        }
    }

    private void clearDefault(Long skillId) {
        AiSkillReportTemplateDO update = new AiSkillReportTemplateDO();
        update.setDefaultFlag(Boolean.FALSE);
        reportTemplateMapper.update(update, new LambdaQueryWrapperX<AiSkillReportTemplateDO>()
                .eq(AiSkillReportTemplateDO::getSkillId, skillId)
                .eq(AiSkillReportTemplateDO::getDefaultFlag, Boolean.TRUE));
    }

    private AiSkillReportTemplateDO selectBySkillIdAndCodeReady(Long skillId, String templateCode) {
        try {
            return reportTemplateMapper.selectBySkillIdAndCode(skillId, templateCode);
        } catch (RuntimeException e) {
            if (isReportTemplateTableMissing(e)) {
                throw new ServiceException("报告模板表刚刚初始化，请重新保存模板");
            }
            throw e;
        }
    }

    private void ensureReportTemplateTableReady() {
        if (!reportTemplateTableExists()) {
            initReportTemplateTable();
        }
    }

    private boolean reportTemplateTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() "
                        + "AND lower(table_name) = 'ai_skill_report_template'",
                Integer.class);
        return count != null && count > 0;
    }

    private void initReportTemplateTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS AI_SKILL_REPORT_TEMPLATE ("
                + "ID BIGINT PRIMARY KEY,"
                + "SKILL_ID BIGINT NOT NULL,"
                + "TEMPLATE_CODE VARCHAR(128) NOT NULL,"
                + "TEMPLATE_NAME VARCHAR(255) NOT NULL,"
                + "TEMPLATE_CONTENT TEXT NOT NULL,"
                + "STATUS VARCHAR(32) NOT NULL DEFAULT 'DRAFT',"
                + "DEFAULT_FLAG BOOLEAN DEFAULT FALSE,"
                + "VERSION INTEGER DEFAULT 1,"
                + "CREATOR_ID BIGINT,"
                + "CREATE_BY VARCHAR(64),"
                + "CREATE_TIME TIMESTAMP,"
                + "UPDATER_ID BIGINT,"
                + "UPDATE_BY VARCHAR(64),"
                + "UPDATE_TIME TIMESTAMP,"
                + "REMARK VARCHAR(500),"
                + "DEL_FLAG BOOLEAN DEFAULT FALSE"
                + ")");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS IDX_AI_SKILL_REPORT_TEMPLATE_CODE "
                + "ON AI_SKILL_REPORT_TEMPLATE (SKILL_ID, TEMPLATE_CODE, DEL_FLAG)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_AI_SKILL_REPORT_TEMPLATE_SKILL "
                + "ON AI_SKILL_REPORT_TEMPLATE (SKILL_ID, DEFAULT_FLAG, STATUS)");
    }

    private boolean isReportTemplateTableMissing(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase().contains("ai_skill_report_template")
                    && (message.toLowerCase().contains("does not exist")
                    || message.contains("不存在"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }
}
