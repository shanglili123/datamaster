package com.datamaster.module.assets.dal.dataobject.skill;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * AI Skill version DO.
 */
@Data
@TableName("AI_SKILL_VERSION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiSkillVersionDO extends BaseEntity {

    private Long skillId;

    private Integer version;

    private String content;

    private String changeType;

    private String changeRemark;
}
