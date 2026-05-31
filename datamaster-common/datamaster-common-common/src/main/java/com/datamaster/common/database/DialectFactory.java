

package com.datamaster.common.database;

import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.dialect.DialectRegistry;

/**
 * 方言工厂类
 *
 * @author QianTongDC
 * @date 2022-11-14
 */
public class DialectFactory {

    private static final DialectRegistry DIALECT_REGISTRY = new DialectRegistry();

    public static DbDialect getDialect(DbType dbType) {
        return DIALECT_REGISTRY.getDialect(dbType);
    }
}
