package com.datamaster.module.catalog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采集模式枚举
 *
 * @author DATAMASTER
 * @date 2026-04-27
 */
@Getter
@AllArgsConstructor
public enum CollectionModeEnum {

    /**
     * 全量采集
     */
    FULL("0", "全量采集"),

    /**
     * 增量采集
     */
    INCREMENTAL("1", "增量采集");

    /**
     * 采集模式值
     */
    private final String mode;

    /**
     * 采集模式名称
     */
    private final String name;

    /**
     * 根据模式值获取枚举
     *
     * @param mode 模式值
     * @return 枚举对象
     */
    public static CollectionModeEnum getByMode(String mode) {
        if (mode == null) {
            return null;
        }
        for (CollectionModeEnum value : values()) {
            if (value.getMode().equals(mode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断是否为全量采集
     *
     * @param mode 模式值
     * @return 是否为全量采集
     */
    public static boolean isFull(String mode) {
        return FULL.getMode().equals(mode);
    }

    /**
     * 判断是否为增量采集
     *
     * @param mode 模式值
     * @return 是否为增量采集
     */
    public static boolean isIncremental(String mode) {
        return INCREMENTAL.getMode().equals(mode);
    }
}
