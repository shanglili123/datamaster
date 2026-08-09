package com.datamaster.metadata.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 探查任务-质量探查任务关联 DO 对象 CAT_TASK_QUALITY
 *
 * @author DATAMASTER
 * @date 2026-07-31
 */
@Data
@TableName(value = "CAT_TASK_QUALITY")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CatalogTaskQualityDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 探查任务ID */
    private Long catTaskId;

    /** 质量探查任务ID */
    private Long qualityTaskId;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;
}
