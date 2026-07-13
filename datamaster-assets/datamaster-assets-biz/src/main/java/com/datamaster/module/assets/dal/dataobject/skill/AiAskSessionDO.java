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
 * AI ask-data conversation session.
 */
@Data
@TableName("AI_ASK_SESSION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiAskSessionDO extends BaseEntity {

    private Long userId;

    private Long projectId;

    private String projectCode;

    private String title;

    private String mode;

    private Long datasourceId;

    private String datasourceName;

    private Long skillId;

    private Long templateId;

    private Boolean returnSql;

    private Integer messageCount;

    @TableLogic
    private Boolean delFlag;
}
