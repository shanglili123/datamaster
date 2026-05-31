package com.datamaster.flinkx.core.util;

import cn.hutool.core.util.IdUtil;

public class EtlIdGenerator {
    public static Long getLongId() {
        return IdUtil.getSnowflakeNextId();
    }
}
