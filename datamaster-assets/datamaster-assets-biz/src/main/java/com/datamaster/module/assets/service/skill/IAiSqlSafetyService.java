package com.datamaster.module.assets.service.skill;

/**
 * AI SQL safety service interface.
 */
public interface IAiSqlSafetyService {

    /**
     * Validate SQL is read-only and safe to execute.
     *
     * @param sql SQL to validate
     * @return validated SQL (trimmed, semicolons removed)
     * @throws ServiceException if SQL is not safe
     */
    String validateReadOnly(String sql);

    /**
     * Check if SQL contains forbidden keywords.
     *
     * @param sql SQL to check
     * @return true if safe
     */
    boolean isSafe(String sql);
}
