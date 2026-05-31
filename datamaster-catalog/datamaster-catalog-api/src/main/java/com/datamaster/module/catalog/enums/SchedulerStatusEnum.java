package com.datamaster.module.catalog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调度器状态枚举
 *
 * @author DATAMASTER
 * @date 2026-05-11
 */
@Getter
@AllArgsConstructor
public enum SchedulerStatusEnum {

    /**
     * 禁用/下线状态
     */
    DISABLED("0", "禁用"),

    /**
     * 启用/上线状态
     */
    ENABLED("1", "启用");

    /**
     * 状态值
     */
    private final String value;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 根据状态值获取枚举
     *
     * @param value 状态值
     * @return 枚举对象
     */
    public static SchedulerStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (SchedulerStatusEnum status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为禁用状态
     *
     * @param value 状态值
     * @return 是否为禁用
     */
    public static boolean isDisabled(String value) {
        return DISABLED.getValue().equals(value);
    }

    /**
     * 判断是否为启用状态
     *
     * @param value 状态值
     * @return 是否为启用
     */
    public static boolean isEnabled(String value) {
        return ENABLED.getValue().equals(value);
    }
}
