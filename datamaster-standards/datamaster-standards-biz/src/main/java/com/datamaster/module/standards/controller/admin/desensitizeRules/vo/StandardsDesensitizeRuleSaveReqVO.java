

package com.datamaster.module.standards.controller.admin.desensitizeRules.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;

/**
 * 脱敏规则 创建/修改 Request VO STD_DESENSITIZE_RULE
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Schema(description = "脱敏规则 Response VO")
@Data
public class StandardsDesensitizeRuleSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分级名称", example = "")
    @Size(max = 256, message = "分级名称长度不能超过256个字符")
    private String name;

    @Schema(description = "数据分类ID", example = "")
    private Long dataCategoryId;

    @Schema(description = "应用场景;1：数据资产  2：数据查询  3：数据服务", example = "")
    @Size(max = 256, message = "应用场景;1：数据资产  2：数据查询  3：数据服务长度不能超过256个字符")
    private String applicationScene;

    @Schema(description = "脱敏方式;1：底层脱敏  2：展示脱敏", example = "")
    @Size(max = 256, message = "脱敏方式;1：底层脱敏  2：展示脱敏长度不能超过256个字符")
    private String maskType;

    @Schema(description = "替换规则", example = "")
    @Size(max = 256, message = "替换规则长度不能超过256个字符")
    private String replaceRule;

    @Schema(description = "替换内容", example = "")
    @Size(max = 256, message = "替换内容长度不能超过256个字符")
    private String replaceContent;

    @Schema(description = "脱敏区间", example = "")
    private List<StandardsDesensitizeIntervalDO> intervalList;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;
}
