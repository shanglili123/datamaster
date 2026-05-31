package com.datamaster.flinkx.core.util;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class EtlLog {
    public static void write(Params params, String message) {
        // no-op bridge for the new FlinkX runtime
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Params {
        private JSONObject rabbitmq;
        private Long processInstanceId;
        private Long taskInstanceId;
    }
}
