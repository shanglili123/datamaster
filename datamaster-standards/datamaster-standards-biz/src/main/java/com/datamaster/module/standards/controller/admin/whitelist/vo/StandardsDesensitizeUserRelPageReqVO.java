

package com.datamaster.module.standards.controller.admin.whitelist.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 脱敏白名单与用户关联关系 Request VO 对象 STD_DESENSITIZE_USER_REL
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Schema(description = "脱敏白名单与用户关联关系 Request VO")
@Data
public class StandardsDesensitizeUserRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "脱敏白名单ID", example = "")
    private Long desensitizeId;

    @Schema(description = "用户ID", example = "")
    private Long userId;

    @Schema(description = "白名单名称", example = "")
    private String desensitizeName;

    @Schema(description = "用户名称", example = "")
    private String userName;



    @Schema(description = "生效分类;1：用户 2：角色 3：部门", example = "")
    private String effectiveCategory;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
