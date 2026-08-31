package com.datamaster.module.ontology.controller.admin.concepttable.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Schema(description = "概念表绑定数据预览 Response VO")
@Data
public class ConceptTablePreviewRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "物理表名", example = "t_customer")
    private String tableName;

    @Schema(description = "列名列表（表头）")
    private List<String> columns;

    @Schema(description = "预览数据行")
    private List<Map<String, Object>> rows;
}