package com.datamaster.common.enums;

import lombok.Getter;

public enum CatType {

    ASSET("ASSET", "数据资产类目"),
    API("API", "数据服务类目"),
    TASK("TASK", "数据集成任务类目"),
    MODEL("MODEL", "逻辑模型类目"),
    DATA_ELEM("DATA_ELEM", "数据元类目"),
    DATA_DEV("DATA_DEV", "数据开发类目"),
    DOCUMENT("DOCUMENT", "标准信息分类"),
    QUALITY("QUALITY", "质量探查类目"),
    TAG("TAG", "标签类目"),
    CLEAN("CLEAN", "清洗规则类目");

    @Getter
    private final String value;

    @Getter
    private final String label;

    CatType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static CatType fromValue(String value) {
        for (CatType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
