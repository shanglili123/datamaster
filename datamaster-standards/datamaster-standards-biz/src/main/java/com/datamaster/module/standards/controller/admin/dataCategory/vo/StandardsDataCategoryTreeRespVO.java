

package com.datamaster.module.standards.controller.admin.dataCategory.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 数据分类 Response VO 对象 STD_DATA_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据分类 Response selectTree VO")
@Data
public class StandardsDataCategoryTreeRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "父id，只有类目有")
    private Long parentId;

    @Schema(description = "类目编码，只有类目有")
    private String catCode;

    @Schema(description = "类型 1:类目 2:分类")
    private String type;

    @Schema(description = "名称")
    private String name;

    /**
     * 脱敏配置（0:否 1:是）
     */
    @Schema(description = "脱敏配置（0:否 1:是）", example = "")
    private String desensitizationRulesFlag;

    @Schema(description = "子")
    private List<StandardsDataCategoryTreeRespVO> children;
}
