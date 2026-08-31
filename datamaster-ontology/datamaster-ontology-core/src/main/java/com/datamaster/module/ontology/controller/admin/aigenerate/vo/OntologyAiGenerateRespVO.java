package com.datamaster.module.ontology.controller.admin.aigenerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 本体 AI 生成 Resp VO
 *
 * <p>preview 与 generate 共用：preview 仅返回 AI 生成的模型（不落库），
 * generate 返回落库后的实体 ID。</p>
 */
@Schema(description = "本体 AI 生成 Resp VO")
@Data
public class OntologyAiGenerateRespVO {

    @Schema(description = "生成的模型预览")
    private GeneratedModelRespVO model;

    @Schema(description = "落库后的本体ID（generate 时返回）")
    private Long ontologyId;

    @Schema(description = "落库后的概念ID（generate 时返回，单表场景）")
    private Long conceptId;

    @Schema(description = "落库后的概念表绑定ID（generate 时返回，单表场景）")
    private Long conceptTableId;

    @Schema(description = "落库后的属性ID列表（generate 时返回）")
    private List<Long> propertyIds;

    @Schema(description = "落库后的关系ID列表（generate 时返回，多表有表间关联时）")
    private List<Long> relationIds;

    @Schema(description = "落库后的概念列表（generate 时返回，含每个概念的 ID/表绑定/属性ID）")
    private List<GeneratedConceptRespVO> savedConcepts;

    @Schema(description = "落库后的关系列表（generate 时返回，含每个关系的 ID/来源目标概念/字段映射）")
    private List<GeneratedRelationRespVO> savedRelations;

    @Schema(description = "quality warning（AI 调用失败等提示）")
    private String qualityWarning;

    @Schema(description = "生成的模型对象")
    @Data
    public static class GeneratedModelRespVO {

        @Schema(description = "建议的本体名称", example = "客户本体")
        private String ontologyName;

        @Schema(description = "建议的本体编码", example = "customer")
        private String ontologyCode;

        @Schema(description = "建议的本体描述", example = "客户域本体模型")
        private String ontologyDescription;

        @Schema(description = "概念列表（单表通常为1个）")
        private List<GeneratedConceptRespVO> concepts;

        @Schema(description = "关系列表（多表之间由 AI 推断的关联，可能为空）")
        private List<GeneratedRelationRespVO> relations;
    }

    @Schema(description = "生成的概念")
    @Data
    public static class GeneratedConceptRespVO {

        @Schema(description = "概念名称", example = "客户")
        private String name;

        @Schema(description = "概念编码", example = "customer")
        private String code;

        @Schema(description = "对应的物理表名", example = "t_customer")
        private String tableName;

        @Schema(description = "概念描述", example = "客户概念定义")
        private String description;

        @Schema(description = "图标标识", example = "user")
        private String icon;

        @Schema(description = "颜色", example = "#409EFF")
        private String color;

        @Schema(description = "属性列表")
        private List<GeneratedPropertyRespVO> properties;

        @Schema(description = "落库后的概念ID（generate 时返回）")
        private Long conceptId;

        @Schema(description = "落库后的概念表绑定ID（generate 时返回）")
        private Long conceptTableId;

        @Schema(description = "落库后的属性ID列表（generate 时返回）")
        private List<Long> propertyIds;
    }

    @Schema(description = "生成的属性")
    @Data
    public static class GeneratedPropertyRespVO {

        @Schema(description = "属性名称", example = "客户名称")
        private String name;

        @Schema(description = "属性编码", example = "customer_name")
        private String code;

        @Schema(description = "数据类型：string/integer/decimal/date/boolean/text", example = "string")
        private String dataType;

        @Schema(description = "描述", example = "客户全称")
        private String description;

        @Schema(description = "是否主键属性", example = "false")
        private Boolean isPrimary;

        @Schema(description = "是否必填", example = "false")
        private Boolean isRequired;

        @Schema(description = "默认值", example = "未知")
        private String defaultValue;

        @Schema(description = "属性映射的物理列名", example = "customer_name")
        private String columnName;

        @Schema(description = "排序", example = "1")
        private Integer sortOrder;
    }

    @Schema(description = "生成的关系")
    @Data
    public static class GeneratedRelationRespVO {

        @Schema(description = "关系名称", example = "拥有订单")
        private String name;

        @Schema(description = "关系编码", example = "has_order")
        private String code;

        @Schema(description = "源概念编码", example = "customer")
        private String sourceConceptCode;

        @Schema(description = "目标概念编码", example = "order")
        private String targetConceptCode;

        @Schema(description = "关系类型：one_to_one/one_to_many/many_to_one/many_to_many", example = "one_to_many")
        private String relationType;

        @Schema(description = "描述", example = "客户与订单的一对多关系")
        private String description;

        @Schema(description = "源概念表字段映射的物理列名", example = "id")
        private String sourceColumn;

        @Schema(description = "目标概念表字段映射的物理列名", example = "customer_id")
        private String targetColumn;

        @Schema(description = "落库后的关系ID（generate 时返回）")
        private Long relationId;

        @Schema(description = "落库后的源概念ID（generate 时返回）")
        private Long sourceConceptId;

        @Schema(description = "落库后的目标概念ID（generate 时返回）")
        private Long targetConceptId;

        @Schema(description = "落库后的源概念表绑定ID（generate 时返回）")
        private Long sourceConceptTableId;

        @Schema(description = "落库后的目标概念表绑定ID（generate 时返回）")
        private Long targetConceptTableId;
    }
}