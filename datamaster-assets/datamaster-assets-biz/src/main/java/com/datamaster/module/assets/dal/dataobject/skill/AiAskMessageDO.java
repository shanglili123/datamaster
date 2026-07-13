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
 * AI ask-data conversation message.
 */
@Data
@TableName("AI_ASK_MESSAGE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiAskMessageDO extends BaseEntity {

    private Long sessionId;

    private Long userId;

    private Long projectId;

    private String role;

    private String content;

    private String displayContent;

    private String payloadJson;

    @TableLogic
    private Boolean delFlag;
}
