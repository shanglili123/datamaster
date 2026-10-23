package com.datamaster.module.collector.utils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

/** Delegates numeric IDs to the project's existing MyBatis-Plus generator. */
public final class IDGeneratorUtils {
    private IDGeneratorUtils() { }

    public static long getLongId() {
        return IdWorker.getId();
    }

    public static String getStringId() {
        return String.valueOf(getLongId());
    }
}
