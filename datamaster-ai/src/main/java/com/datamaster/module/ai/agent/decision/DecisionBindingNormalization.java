package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ai.agent.decision.DecisionModels.Binding;

/** One effective binding representation for amendment, calculation and execution preview. */
final class DecisionBindingNormalization {
    private DecisionBindingNormalization() { }

    static Binding effective(Binding source, String proposalObjectKey) {
        if (source == null) return null;
        Binding binding = JSON.parseObject(JSON.toJSONString(source), Binding.class);
        String key = StringUtils.isBlank(binding.getObjectKey()) ? proposalObjectKey : binding.getObjectKey();
        binding.setObjectKey(StringUtils.isBlank(key) ? null : key.trim());
        if (binding.getType() != null) {
            binding.setType("COMPOSITE".equalsIgnoreCase(binding.getType()) ? "ACTION" : binding.getType().toUpperCase(java.util.Locale.ROOT));
        }
        binding.setInputParams(DecisionBindingResolver.canonical(DecisionBindingResolver.parseInput(binding.getInputParams())));
        return binding;
    }
}
