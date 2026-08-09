

package com.datamaster.module.governance.controller.admin.space.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间 Response VO 对象 TAX_SPACE
 *
 * @author shu
 * @date 2025-01-20
 */
@Schema(description = "空间 Response VO")
@Data
public class TaxonomySpaceRespVO implements Serializable {

    private static final long serialVersionUID = 1L;


    @Excel(name = "ID")
    @Schema(description = "ID", example = "")
    private Long id;

    @Excel(name = "空间名称")
    @Schema(description = "空间名称", example = "")
    private String name;

    @Excel(name = "空间编码")
    @Schema(description = "空间编码", example = "")
    private String code;

    @Excel(name = "DS 专属工作组")
    @Schema(description = "DS 专属工作组", example = "")
    private String workerGroup;

    @Excel(name = "空间管理员 ID")
    @Schema(description = "空间管理员 ID", example = "")
    private Long managerId;
    @Excel(name = "空间管理员")
    @Schema(description = "空间管理员", example = "")
    private String nickName;

    @Excel(name = "空间管理员电话")
    @Schema(description = "空间管理员电话", example = "")
    private String managerPhone;

    @Excel(name = "空间描述")
    @Schema(description = "空间描述", example = "")
    private String description;

    @Excel(name = "是否有效")
    @Schema(description = "是否有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志")
    @Schema(description = "删除标志", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人 ID")
    @Schema(description = "创建人 ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人 ID")
    @Schema(description = "更新人 ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

}
