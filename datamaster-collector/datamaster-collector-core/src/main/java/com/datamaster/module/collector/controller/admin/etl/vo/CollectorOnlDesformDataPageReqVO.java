

package com.datamaster.module.collector.controller.admin.etl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 在线单数据 Request VO 对象 COL_ONL_DESFORM_DATA
 *
 * @author DATAMASTER
 * @date 2025-04-09
 */
@Schema(description = "在线单数据 Request VO")
@Data
public class CollectorOnlDesformDataPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "表单编码", example = "")
    private String desformCode;

    @Schema(description = "表单名称", example = "")
    private String desformName;

    @Schema(description = "表单ID", example = "")
    private String desformId;

    @Schema(description = "表单数据", example = "")
    private String desformData;




}
