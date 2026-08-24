package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 动作定义 DO
 *
 * 对应表 ONT_ACTION
 */
@Data
@TableName("ONT_ACTION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionDO extends BaseEntity {

    /** 所属本体ID */
    private Long ontologyId;

    /** 动作名称 */
    private String name;

    /** 动作类型：CREATE/UPDATE/DELETE/QUERY */
    private String actionType;

    /** 绑定概念ID */
    private Long conceptId;

    /** 描述 */
    private String description;

    @TableLogic
    private Integer delFlag;
}
