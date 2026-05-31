

package com.datamaster.spark.etl.transition;

import com.datamaster.common.exception.ServiceException;

import java.util.Optional;

/**
 * <P>
 * 用途:转换工厂
 * </p>
 *
 * @author: FXB
 * @create: 2025-06-20 09:10
 **/
public class TransitionFactory {

    private static final TransitionRegistry COMPONENT_ITEM_REGISTRY = new TransitionRegistry();

    public TransitionFactory() {
    }

    public static Transition getTransition(String code) {
        return Optional.ofNullable(COMPONENT_ITEM_REGISTRY.getTransition(code)).orElseThrow(() -> new ServiceException(String.format("%s not supported.", code)));
    }
}
