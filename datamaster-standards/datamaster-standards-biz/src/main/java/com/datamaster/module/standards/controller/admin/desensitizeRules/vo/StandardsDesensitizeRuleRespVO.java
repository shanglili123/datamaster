

package com.datamaster.module.standards.controller.admin.desensitizeRules.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.datamaster.common.annotation.Excel;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;

import java.util.Date;
import java.io.Serializable;
import java.util.List;

/**
 * 脱敏规则 Response VO 对象 STD_DESENSITIZE_RULE
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Schema(description = "脱敏规则 Response VO")
@Data
public class StandardsDesensitizeRuleRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "规则名称")
    @Schema(description = "规则名称", example = "")
    private String name;

    @Excel(name = "数据分类ID")
    @Schema(description = "数据分类ID", example = "")
    private Long dataCategoryId;
    @Excel(name = "数据分类名称")
    @Schema(description = "数据分类名称", example = "")
    private String dataCategoryName;

    @Excel(name = "应用场景;1：数据资产  2：数据查询  3：数据服务")
    @Schema(description = "应用场景;1：数据资产  2：数据查询  3：数据服务", example = "")
    private String applicationScene;

    @Excel(name = "脱敏方式;1：底层脱敏  2：展示脱敏")
    @Schema(description = "脱敏方式;1：底层脱敏  2：展示脱敏", example = "")
    private String maskType;

    @Excel(name = "替换规则")
    @Schema(description = "替换规则", example = "")
    private String replaceRule;

    @Excel(name = "替换内容")
    @Schema(description = "替换内容", example = "")
    private String replaceContent;

    @Excel(name = "脱敏区间")
    @Schema(description = "脱敏区间", example = "")
    private List<StandardsDesensitizeIntervalDO> intervalList;

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
