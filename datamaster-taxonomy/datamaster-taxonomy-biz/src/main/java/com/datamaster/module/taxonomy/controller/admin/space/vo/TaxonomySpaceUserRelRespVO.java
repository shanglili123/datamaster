

package com.datamaster.module.taxonomy.controller.admin.space.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 空间与用户关联关系 Response VO 对象 TAX_SPACE_USER_REL
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Schema(description = "空间与用户关联关系 Response VO")
@Data
public class TaxonomySpaceUserRelRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "空间 ID")
    @Schema(description = "空间 ID", example = "")
    private Long spaceId;

    @Excel(name = "用户 ID")
    @Schema(description = "用户 ID", example = "")
    private Long userId;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人 ID")
    @Schema(description = "更新人 ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "")
    private String description;

    @Excel(name = "用户名称")
    @Schema(description = "用户名称", example = "")
    private String userName;

    @Excel(name = "手机号码")
    @Schema(description = "手机号码", example = "")
    private String phoneNumber;

    @Excel(name = "部门名称")
    @Schema(description = "部门名称", example = "")
    private String deptName;

    @Excel(name = "用户昵称")
    @Schema(description = "用户昵称", example = "")
    private String nickName;

    @Schema(description = "用户 ID 集合", example = "")
    private List<Long> userIdList;

    @Schema(description = "角色 ID 集合", example = "")
    private List<Long> roleIdList;

    /** 用户角色名称，多个用逗号分隔 */
    @Schema(description = "用户角色名称，多个用逗号分隔")
    private String roleStr;
}
