

package com.datamaster.module.modeling.controller.admin.businessCategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import com.datamaster.common.core.page.PageParam;

/**
 * 业务分类 Request VO 对象 Modeling_BUSINESS_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
@Schema(description = "业务分类 Request VO")
@Data
public class ModelingBusinessCategoryPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;

    @Excel(name = "层级编码")
    @Schema(description = "层级编码", example = "")
    private String code;

    @Schema(description = "业务分类名称", example = "")
    private String name;

    @Schema(description = "关联上级ID", example = "")
    private Long parentId;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "英文缩写名", example = "")
    private String engName;

    @Schema(description = "负责人手机号", example = "")
    private String ownerPhone;

    @Schema(description = "负责人ID", example = "")
    private Long ownerId;

    @Schema(description = "数据域ID", example = "")
    private Long domainId;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;


}
