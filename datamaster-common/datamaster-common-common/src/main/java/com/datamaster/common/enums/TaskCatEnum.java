package com.datamaster.common.enums;

import lombok.Getter;

import java.util.Locale;

public enum TaskCatEnum {

    TAX_TASK_CAT("1", "离线数据集成任务", CatType.TASK),
    CAT("2", "实时任务", CatType.TASK),
    TAX_DATA_DEV_CAT("3", "数据开发任务", CatType.DATA_DEV),
    TAX_JOB_CAT("4", "作业任务", CatType.DATA_DEV);

    @Getter
    private final String type;

    @Getter
    private final String name;

    @Getter
    private final CatType catType;

    TaskCatEnum(String type, String name, CatType catType) {
        this.type = type;
        this.name = name;
        this.catType = catType;
    }

    public static TaskCatEnum findEnumByType(String type) {
        for (TaskCatEnum taskCatEnum : TaskCatEnum.values()) {
            if (taskCatEnum.getType().toUpperCase(Locale.ROOT).equals(type.toUpperCase(Locale.ROOT))) {
                return taskCatEnum;
            }
        }
        return null;
    }

}
