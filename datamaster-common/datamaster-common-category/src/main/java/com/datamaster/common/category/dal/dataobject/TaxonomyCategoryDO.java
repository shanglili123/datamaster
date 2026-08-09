package com.datamaster.common.category.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

@Data
@TableName(value = "TAX_CATEGORY")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaxonomyCategoryDO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String catType;

    private String name;

    private Long parentId;

    private Long sortOrder;

    private String description;

    private String code;

    private Long spaceId;

    private String spaceCode;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;

}
