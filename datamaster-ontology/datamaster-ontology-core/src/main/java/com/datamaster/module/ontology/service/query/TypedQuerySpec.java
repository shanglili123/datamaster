package com.datamaster.module.ontology.service.query;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类型化查询说明（查询层的结构化 JSON 骨架，等价 Palantir 对象集/FQL 概念的落地形态）
 *
 * 一个可编译为安全 SQL 的查询描述：
 *   - groups：逻辑组。组内为扁平条件列表（每条条件自带 且/或/非 连接符），组 = 括号，
 *     组间按 group.connector（AND/OR）连接。NOT 表达在条件级 connector 上，不再有组级 negate。
 *   - orderBy：排序 [ {field, dir(asc|desc)} ]，field 须命中列白名单
 *   - columns：列投影（白名单），为空则默认全部
 *   - keyword ：跨字段整表模糊搜索关键字（仅对白名单内文本列 LOWER(col) LIKE LOWER(:kw)）
 *
 * 不可变、经白名单校验后由 {@link TypedQueryBuilder} 编译。
 *
 * @author datamaster
 */
@Data
public class TypedQuerySpec implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 逻辑组（括号）。组内为扁平条件，组间按 connector 连接 */
    private List<FilterGroup> groups;

    /** 排序：[{field, dir}]，field 须命中列白名单 */
    private List<OrderByField> orderBy;

    /** 列投影（白名单），为空默认取全部列 */
    private List<String> columns;

    /** 跨字段模糊搜索关键字 */
    private String keyword;

    /**
     * 逻辑过滤组（括号语义）：组内为扁平条件列表，组间按 connector 连接。
     */
    @Data
    public static class FilterGroup implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 组间连接符：AND / OR（首个组忽略） */
        private String connector;

        /** 组内扁平条件列表，每条自带 且/或/非 连接符 */
        private List<TypedFilter> filters;
    }

    /**
     * 排序字段
     */
    @Data
    public static class OrderByField implements Serializable {

        private static final long serialVersionUID = 1L;

        private String field;

        /** asc / desc */
        private String dir;
    }
}
