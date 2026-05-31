

package com.datamaster.module.standards.controller.admin.whitelist.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 脱敏白名单 Request VO 对象 STD_DESENSITIZE_WHITELIST
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Schema(description = "脱敏白名单 Request VO")
@Data
public class StandardsDesensitizeWhitelistPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "白名单名称", example = "")
    private String name;

    @Schema(description = "数据分类", example = "")
    private Long dataCategoryId;

    @Schema(description = "生效分类;1：用户 2：角色 3：部门", example = "")
    private String effectiveCategory;

    @Schema(description = "开始时间", example = "")
    private Date startTime;

    @Schema(description = "结束时间", example = "")
    private Date endTime;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;



}
