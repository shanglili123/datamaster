

package com.datamaster.quality.dal.dataobject.quality;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class ColumnCompare {

    private String leftField;
    private String operator;
    private String rightField;

    public String toExpression() {
        return String.format("%s%s%s", leftField, operator, rightField);
    }

    public String toExpressionIgnoreNullValue() {
        return String.format("(%s IS NULL OR %s IS NULL OR %s%s%s)", leftField, rightField, leftField, operator, rightField);
    }

    public static String toExpressions(List<ColumnCompare> compares, boolean ignoreNullValue) {
        if (ignoreNullValue) {
            return compares.stream().map(ColumnCompare::toExpressionIgnoreNullValue).collect(Collectors.joining(" AND "));
        }
        return compares.stream().map(ColumnCompare::toExpression).collect(Collectors.joining(" AND "));
    }

    public static String toExpressionsNeg(List<ColumnCompare> compares, boolean ignoreNullValue) {
        if (ignoreNullValue) {
            return compares.stream().map(it -> String.format("(NOT %s)", it.toExpression())).collect(Collectors.joining(" OR "));
        }
        return compares.stream().map(it -> String.format("(%s IS NULL OR %s IS NULL OR NOT %s)", it.getLeftField(), it.getRightField(), it.toExpression()))
                .collect(Collectors.joining(" OR "));
    }

}
