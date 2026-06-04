

package com.datamaster.module.modeling.controller.admin.businessCategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 业务分类数据域关联关系 Request VO 对象 Modeling_BUSINESS_DOMAIN_REL
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Schema(description = "业务分类数据域关联关系 Request VO")
@Data
public class ModelingBusinessDomainRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "业务分类ID", example = "")
    private Long businessCategoryId;

    @Schema(description = "数据域ID", example = "")
    private Long dataDomainId;

    @Schema(description = "业务分类名称", example = "")
    private String businessCategoryName;

    @Schema(description = "数据域名称", example = "")
    private String dataDomainName;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
