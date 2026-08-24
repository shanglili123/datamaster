package com.datamaster.module.ontology.controller.admin.action.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "动作 Response VO")
@Data
public class ActionRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "本体ID", example = "1")
    @Excel(name = "本体ID")
    private Long ontologyId;

    @Schema(description = "动作名称", example = "创建客户")
    @Excel(name = "动作名称")
    private String name;

    @Schema(description = "动作类型", example = "CREATE")
    @Excel(name = "动作类型")
    private String actionType;

    @Schema(description = "绑定概念ID", example = "1")
    @Excel(name = "绑定概念ID")
    private Long conceptId;

    @Schema(description = "描述", example = "创建新客户记录")
    @Excel(name = "描述")
    private String description;

    @Schema(description = "创建者", example = "admin")
    @Excel(name = "创建者")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
