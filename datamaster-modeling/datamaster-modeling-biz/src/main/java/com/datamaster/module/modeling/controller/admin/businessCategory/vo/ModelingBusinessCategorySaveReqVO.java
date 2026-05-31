

package com.datamaster.module.modeling.controller.admin.businessCategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessDomainRelDO;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 业务分类 创建/修改 Request VO Modeling_BUSINESS_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
@Schema(description = "业务分类 Response VO")
@Data
public class ModelingBusinessCategorySaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Excel(name = "层级编码")
    @Schema(description = "层级编码", example = "")
    private String code;

    @Schema(description = "业务分类名称", example = "")
    @Size(max = 256, message = "业务分类名称长度不能超过256个字符")
    private String name;

    @Schema(description = "关联上级ID", example = "")
    private Long parentId;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "英文缩写名", example = "")
    @Size(max = 256, message = "英文缩写名长度不能超过256个字符")
    private String engName;

    @Schema(description = "负责人手机号", example = "")
    @Size(max = 256, message = "负责人手机号长度不能超过256个字符")
    private String ownerPhone;

    @Schema(description = "负责人ID", example = "")
    private Long ownerId;

    @Schema(description = "数据域ID", example = "")
    private Long domainId;

    @Schema(description = "数据域集合", example = "")
    private List<ModelingBusinessDomainRelDO> domainList;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;

}
