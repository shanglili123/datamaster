package com.datamaster.module.assets.dal.dataobject.skill;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * AI Skill report template DO.
 */
@Data
@TableName("AI_SKILL_REPORT_TEMPLATE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiSkillReportTemplateDO extends BaseEntity {

    private Long skillId;

    private String templateCode;

    private String templateName;

    private String templateContent;

    private String status;

    private Boolean defaultFlag;

    private Integer version;

    @TableLogic
    private Boolean delFlag;
}
