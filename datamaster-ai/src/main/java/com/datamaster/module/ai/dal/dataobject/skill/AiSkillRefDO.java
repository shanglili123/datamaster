package com.datamaster.module.ai.dal.dataobject.skill;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * AI Skill reference DO.
 */
@Data
@TableName("AI_SKILL_REF")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiSkillRefDO extends BaseEntity {

    private Long skillId;

    private String refType;

    private Long refId;

    private String refCode;

    private String refName;
}

