package com.datamaster.module.assets.controller.admin.discovery.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.List;

/**
 *  / Request VO DA_DISCOVERY_TABLE
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Schema(description = " Response VO")
@Data
public class AssetsDiscoveryTableSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long taskId;

    @Schema(description = "id", example = "")
    private List<Long> taskIdList;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String tableName;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String tableComment;

    @Schema(description = "", example = "")
    private Long dataCount;

    @Schema(description = "", example = "")
    private Long fieldCount;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String changeFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String status;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String ignoreFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

    @Schema(description = "", example = "")
    @TableField(exist = false)
    private String catCode;

    @Schema(description = "id", example = "")
    @TableField(exist = false)
    private String themeId;

    @Schema(description = "", example = "")
    @TableField(exist = false)
    private String assetName;
}
