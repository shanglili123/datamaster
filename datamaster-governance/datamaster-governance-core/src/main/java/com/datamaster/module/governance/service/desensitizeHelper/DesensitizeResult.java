package com.datamaster.module.governance.service.desensitizeHelper;

import com.datamaster.module.governance.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 脱敏计算结果 — computeActions() 返回
 *
 * @author DATAMASTER
 */
@Data
public class DesensitizeResult {

    /**
     * 列名 → 脱敏动作码:
     *   1 = 放行（不脱敏）
     *   2 = 区间替换
     *   3 = 隐藏列（整列不返回）
     */
    private Map<String, Integer> columnActions;

    /**
     * 需要区间替换的列 → 替换参数（仅 action=2 时有值）
     */
    private Map<String, ReplaceParam> replaceParams;

    @Data
    @Builder
    public static class ReplaceParam {
        /** 替换内容，如 "*" */
        private String replaceContent;
        /** 区间列表 */
        private List<StandardsDesensitizeIntervalDO> intervals;
    }
}
