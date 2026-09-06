package com.datamaster.module.ontology.service.query;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 单个类型化过滤条件
 *
 * 对应 Palantir FQL/对象浏览器概念在服务端的结构化查询骨架（不做独立 DSL 语言）：
 * 组内为扁平条件列表，每条条件自带连接符（connector），形态 = 属性 + 运算符 + 值（区间/IN 用 values），
 * 与前端"属性选择 + 运算符选择 + 连接符（且/或/非）"一一对应。
 *
 * 运算符（op）：
 *   eq / ne / gt / ge / lt / le / like / in / between / isNull / isNotNull
 *
 * 列名经白名单校验后才进入 SQL，值一律走 NamedParameter 占位符防注入。
 *
 * @author datamaster
 */
@Data
public class TypedFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 与上一条条件的连接符：AND(且) / OR(或) / NOT(非)。组内首条条件忽略该值 */
    private String connector;

    /** 物理列名（语义层调用方先将属性编码解析为物理列，或直接传物理列） */
    private String field;

    /** 运算符：eq/ne/gt/ge/lt/le/like/in/between/isNull/isNotNull */
    private String op;

    /** 单值运算符使用的值（eq/ne/gt/ge/lt/le/like） */
    private Object value;

    /** in/between 运算符使用的值集合（in=[...]，between=[min,max]） */
    private List<Object> values;
}
