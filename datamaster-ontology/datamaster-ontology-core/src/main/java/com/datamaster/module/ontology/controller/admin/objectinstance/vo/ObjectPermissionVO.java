package com.datamaster.module.ontology.controller.admin.objectinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 对象权限 VO —— 权限血缘的实时摘要。
 *
 * <p>
 * 由资产统一权限入口（AssetsTableGovernanceApiService.resolveTable）实时计算：
 * 当前空间/用户对该对象支撑表的访问是否允许、以及字段级可见范围。
 */
@Schema(description = "对象权限 VO（权限血缘摘要）")
@Data
public class ObjectPermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否允许访问支撑表")
    private Boolean accessible;

    @Schema(description = "判定消息（拒绝时含原因）")
    private String message;

    @Schema(description = "被拒绝的字段（表级+字段级）")
    private List<String> deniedColumns;

    @Schema(description = "允许查询的字段清单；为空表示全量放行")
    private List<String> allowedColumns;
}
