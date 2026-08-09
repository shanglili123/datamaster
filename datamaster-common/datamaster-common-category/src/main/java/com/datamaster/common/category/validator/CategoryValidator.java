package com.datamaster.common.category.validator;

public interface CategoryValidator {

    String catType();

    default void validateDelete(String code) {}

    default void validateDisable(String code) {}

    default void validateEnable(Long id, Long parentId) {}
}
