package com.datamaster.module.catalog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采集范围枚举
 *
 * @author DATAMASTER
 * @date 2026-04-27
 */
@Getter
@AllArgsConstructor
public enum CollectionScopeEnum {

    /**
     * 全部库
     */
    ALL("2", "全部库"),

    /**
     * 自定义库
     */
    CUSTOM("1", "自定义库");

    /**
     * 采集范围值
     */
    private final String scope;

    /**
     * 采集范围名称
     */
    private final String name;

    /**
     * 根据范围值获取枚举
     *
     * @param scope 范围值
     * @return 枚举对象
     */
    public static CollectionScopeEnum getByScope(String scope) {
        if (scope == null) {
            return null;
        }
        for (CollectionScopeEnum value : values()) {
            if (value.getScope().equals(scope)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断是否为全部库
     *
     * @param scope 范围值
     * @return 是否为全部库
     */
    public static boolean isAll(String scope) {
        return ALL.getScope().equals(scope);
    }

    /**
     * 判断是否为自定义库
     *
     * @param scope 范围值
     * @return 是否为自定义库
     */
    public static boolean isCustom(String scope) {
        return CUSTOM.getScope().equals(scope);
    }
}
