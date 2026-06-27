package com.datamaster.module.assets.service.skill.impl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.service.skill.IAiSqlSafetyService;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * AI SQL safety service implementation.
 * Ensures SQL is read-only and safe to execute.
 */
@Service
public class AiSqlSafetyServiceImpl implements IAiSqlSafetyService {

    private static final String[] FORBIDDEN_KEYWORDS = {
            "insert", "update", "delete", "create", "drop", "alter",
            "truncate", "exec", "execute", "merge", "call", "grant", "revoke"
    };

    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'[^']*'");

    @Override
    public String validateReadOnly(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new ServiceException("SQL不能为空");
        }

        String trimmed = sql.trim();

        // Remove trailing semicolons
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        // Check for multiple statements
        if (trimmed.contains(";")) {
            throw new ServiceException("仅支持单条SQL语句，不允许包含多个语句");
        }

        // Must start with SELECT (case-insensitive)
        if (!trimmed.toLowerCase().startsWith("select")) {
            throw new ServiceException("仅允许执行SELECT查询语句");
        }

        // Check for forbidden keywords
        if (!isSafe(trimmed)) {
            throw new ServiceException("SQL中包含不允许的操作标识");
        }

        return trimmed;
    }

    @Override
    public boolean isSafe(String sql) {
        if (StringUtils.isBlank(sql)) {
            return false;
        }

        // Remove string literals to avoid false positives
        String withoutStringLiterals = STRING_LITERAL_PATTERN.matcher(sql).replaceAll("");

        String lowerSql = withoutStringLiterals.toLowerCase();

        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (Pattern.compile("\\b" + keyword + "\\b").matcher(lowerSql).find()) {
                return false;
            }
        }
        return true;
    }
}
