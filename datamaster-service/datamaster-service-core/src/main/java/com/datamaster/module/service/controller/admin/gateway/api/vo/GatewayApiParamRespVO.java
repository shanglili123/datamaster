package com.datamaster.module.service.controller.admin.gateway.api.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 数据服务-API网关-参数 Response VO  SVC_API_GATEWAY_PARAM
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "API网关-参数 Response VO")
@Data
public class GatewayApiParamRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "API id")
    @Schema(description = "API id", example = "")
    private Long apiId;

    @Excel(name = "父级id")
    @Schema(description = "父级id", example = "")
    private Long parentId;

    @Excel(name = "参数名称")
    @Schema(description = "参数名称", example = "")
    private String name;

    @Schema(description = "数据默认值", example = "")
    private String defaultValue;
    @Schema(description = "示例值", example = "")
    private String exampleValue;
    @Schema(description = "描述", example = "")
    private String description;

    @Excel(name = "参数类型")
    @Schema(description = "参数类型", example = "")
    private String type;

    @Excel(name = "是否必填")
    @Schema(description = "是否必填", example = "")
    private String requestFlag;

    @Excel(name = "字段类型")
    @Schema(description = "字段类型", example = "")
    private String columnType;

    @Excel(name = "是否有效")
    @Schema(description = "是否有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志")
    @Schema(description = "删除标志", example = "")
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

    @TableField(exist = false)
    private List<GatewayApiParamRespVO> gatewayApiParamList;
}
