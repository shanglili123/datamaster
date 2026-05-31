

package com.datamaster.module.modeling.controller.admin.dm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

import com.datamaster.common.core.domain.BaseEntity;

/**
 * 主题域管理 创建/修改 Request VO Modeling_THEME_DOMAIN
 *
 * @author FXB
 * @date 2026-03-24
 */
@Schema(description = "主题域管理 Response VO")
@Data
public class ModelingThemeDomainSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "层级编码", example = "")
    @Size(max = 256, message = "层级编码长度不能超过256个字符")
    private String code;

    @Schema(description = "名称", example = "")
    @Size(max = 256, message = "名称长度不能超过256个字符")
    private String name;

    @Schema(description = "英文缩写", example = "")
    @Size(max = 256, message = "英文缩写长度不能超过256个字符")
    private String engName;

    @Schema(description = "关联上级ID", example = "")
    private Long parentId;

    @Schema(description = "负责人ID", example = "")
    private Long ownerUserId;

    @Schema(description = "数仓分层ID", example = "")
    private Long dataLayerId;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;

    @Schema(description = "是否有效", example = "")
    private Boolean validFlag;
}
