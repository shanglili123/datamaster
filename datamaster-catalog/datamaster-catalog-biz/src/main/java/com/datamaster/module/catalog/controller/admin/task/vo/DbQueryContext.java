package com.datamaster.module.catalog.controller.admin.task.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;

@Data
@AllArgsConstructor
public class DbQueryContext {

    /**
     * 已校验、可用的 DbQuery
     */
    private DbQuery dbQuery;

    /**
     * 当前库 / schema 对应的查询属性
     */
    private DbQueryProperty property;
}
