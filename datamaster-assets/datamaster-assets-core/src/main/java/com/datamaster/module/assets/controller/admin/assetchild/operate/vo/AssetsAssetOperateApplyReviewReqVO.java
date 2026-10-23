package com.datamaster.module.assets.controller.admin.assetchild.operate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 数据资产操作申请审批请求。 */
@Data
@Schema(description = "数据资产操作申请审批请求")
public class AssetsAssetOperateApplyReviewReqVO {

    @NotNull
    @Schema(description = "申请 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Size(max = 1000, message = "审批意见不能超过1000个字符")
    @Schema(description = "审批意见")
    private String comment;
}
