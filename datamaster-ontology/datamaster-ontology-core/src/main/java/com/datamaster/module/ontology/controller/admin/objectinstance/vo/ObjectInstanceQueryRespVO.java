package com.datamaster.module.ontology.controller.admin.objectinstance.vo;

import com.datamaster.module.ontology.api.dto.SemanticPropertyDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 对象实例查询 Response VO
 *
 * 对象实例 = 概念（对象类型）绑定物理表中的一行数据。
 * 返回对象集元信息 + 属性语义 + 表头列 + 实例行数据。
 */
@Schema(description = "对象实例查询 Response VO")
@Data
public class ObjectInstanceQueryRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "概念ID（对象类型ID）", example = "1")
    private Long conceptId;

    @Schema(description = "概念名称（对象类型名称）", example = "客户")
    private String conceptName;

    @Schema(description = "物理表绑定ID", example = "1")
    private Long tableBindingId;

    @Schema(description = "物理表名", example = "t_customer")
    private String tableName;

    @Schema(description = "列名列表（表头）")
    private List<String> columns;

    @Schema(description = "属性语义映射（含物理列，供前端展示语义列名）")
    private List<SemanticPropertyDTO> properties;

    @Schema(description = "实例行数据")
    private List<Map<String, Object>> rows;

    @Schema(description = "实例总数")
    private Long total;
}
