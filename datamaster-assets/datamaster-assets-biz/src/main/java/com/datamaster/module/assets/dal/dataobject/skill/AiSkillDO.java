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
 * AI Skill DO.
 */
@Data
@TableName("AI_SKILL")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiSkillDO extends BaseEntity {

    private String skillCode;

    private String skillName;

    private String skillType;

    private String status;

    private String sourceType;

    private String bizObjectType;

    private Long bizObjectId;

    private String content;

    private String contentHash;

    private Integer version;

    private String dbgptSpaceName;

    private String dbgptDocumentName;

    private String dbgptSyncStatus;

    private String dbgptSyncMessage;

    @TableLogic
    private Boolean delFlag;
}
