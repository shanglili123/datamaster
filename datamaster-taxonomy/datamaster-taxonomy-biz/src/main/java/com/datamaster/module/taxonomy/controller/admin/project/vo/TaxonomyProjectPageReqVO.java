

package com.datamaster.module.taxonomy.controller.admin.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 项目 Request VO 对象 TAX_PROJECT
 *
 * @author shu
 * @date 2025-01-20
 */
@Schema(description = "项目 Request VO")
@Data
public class TaxonomyProjectPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "项目名称", example = "")
    private String name;
    @Schema(description = "负责人", example = "")
    private Long managerId;






}
