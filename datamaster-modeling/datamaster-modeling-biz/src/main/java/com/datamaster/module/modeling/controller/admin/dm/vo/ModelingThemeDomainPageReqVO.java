

package com.datamaster.module.modeling.controller.admin.dm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 主题域管理 Request VO 对象 Modeling_THEME_DOMAIN
 *
 * @author FXB
 * @date 2026-03-24
 */
@Schema(description = "主题域管理 Request VO")
@Data
public class ModelingThemeDomainPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "层级编码", example = "")
    private String code;

    @Schema(description = "名称", example = "")
    private String name;

    @Schema(description = "英文缩写", example = "")
    private String engName;

    @Schema(description = "关联上级ID", example = "")
    private Long parentId;

    @Schema(description = "负责人ID", example = "")
    private Long ownerUserId;

    @Schema(description = "数仓分层ID", example = "")
    private Long dataLayerId;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
