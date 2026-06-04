

package com.datamaster.module.taxonomy.controller.admin.rule.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import com.datamaster.common.core.page.PageParam;

/**
 * 清洗规则 Request VO 对象 TAX_CLEAN_RULE
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Schema(description = "清洗规则 Request VO")
@Data
public class TaxonomyCleanRulePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "规则名称", example = "")
    private String name;

    @Schema(description = "规则编码", example = "")
    private String code;

    @Schema(description = "规则类型", example = "")
    private String type;
    @Schema(description = "规则级别", example = "")
    private String level;
    @Schema(description = "策略标识", example = "")
    private String strategyKey;
    @Excel(name = "是否有效")
    @Schema(description = "是否有效", example = "")
    private Boolean validFlag;



    /** 类目编码 */
    private String catCode;

    @TableField(exist = false)
    private String catID;

    @TableField(exist = false)
    private String catName;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
