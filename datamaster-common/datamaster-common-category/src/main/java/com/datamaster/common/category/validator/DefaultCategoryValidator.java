package com.datamaster.common.category.validator;

import org.springframework.stereotype.Component;

/**
 * 默认分类校验器，无校验逻辑，仅用于满足 Spring List 注入要求。
 * 业务模块可通过实现 {@link CategoryValidator} 接口并注册为 Bean 来覆盖各 catType 的校验逻辑。
 */
@Component
public class DefaultCategoryValidator implements CategoryValidator {

    @Override
    public String catType() {
        return "";
    }
}
