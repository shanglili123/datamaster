

package com.datamaster.module.modeling.controller.admin.businessCategory.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务分类数据域关联关系 Response VO 对象 Modeling_BUSINESS_DOMAIN_REL
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Schema(description = "业务分类数据域关联关系 Response VO")
@Data
public class ModelingBusinessDomainRelRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "业务分类ID")
    @Schema(description = "业务分类ID", example = "")
    private Long businessCategoryId;

    @Excel(name = "数据域ID")
    @Schema(description = "数据域ID", example = "")
    private Long dataDomainId;

    @Excel(name = "业务分类名称")
    @Schema(description = "业务分类名称", example = "")
    private String businessCategoryName;

    @Excel(name = "数据域名称")
    @Schema(description = "数据域名称", example = "")
    private String dataDomainName;

    @Excel(name = "排序")
    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "")
    private String description;

    @Excel(name = "是否有效;0：无效，1：有效")
    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志;1：已删除，0：未删除")
    @Schema(description = "删除标志;1：已删除，0：未删除", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人id")
    @Schema(description = "创建人id", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人id")
    @Schema(description = "更新人id", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
