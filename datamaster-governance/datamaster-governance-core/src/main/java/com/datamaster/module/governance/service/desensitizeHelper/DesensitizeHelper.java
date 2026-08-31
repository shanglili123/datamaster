package com.datamaster.module.governance.service.desensitizeHelper;

import com.datamaster.module.governance.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;
import com.datamaster.module.governance.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
import com.datamaster.module.governance.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;
import com.datamaster.module.governance.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;
import com.datamaster.module.governance.service.desensitizeList.IStandardsDesensitizeAssetcolumnService;
import com.datamaster.module.governance.service.desensitizeRules.IStandardsDesensitizeRuleService;
import com.datamaster.module.governance.service.whitelist.IStandardsDesensitizeWhitelistService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 脱敏公共工具类 — 供数据资产、数据服务、AI问数、本体动作等模块复用
 * <p>
 * 用法:
 * <pre>
 *   DesensitizeResult result = desensitizeHelper.computeActions(columns, context);
 *   List<Map<String, Object>> masked = desensitizeHelper.applyMasking(data, result);
 * </pre>
 *
 * @author DATAMASTER
 */
@Component
public class DesensitizeHelper {

    @Resource
    private IStandardsDesensitizeAssetcolumnService bindingService;
    @Resource
    private IStandardsDesensitizeRuleService ruleService;
    @Resource
    private IStandardsDesensitizeWhitelistService whitelistService;

    /** 放行 */
    public static final int ACTION_PASS = 1;
    /** 区间替换 */
    public static final int ACTION_REPLACE = 2;
    /** 隐藏列 */
    public static final int ACTION_HIDE = 3;

    // ======================== 计算脱敏动作 ========================

    /**
     * 根据列元数据 + 用户上下文，计算每列的脱敏动作
     *
     * @param columns 列元数据列表
     * @param context 脱敏上下文（场景、用户、权限等级）
     * @return 脱敏结果（动作码 + 替换参数）
     */
    public DesensitizeResult computeActions(List<ColumnDesensitizeMeta> columns, DesensitizeContext context) {
        Map<String, Integer> columnActions = new LinkedHashMap<>();
        Map<String, DesensitizeResult.ReplaceParam> replaceParams = new HashMap<>();

        for (ColumnDesensitizeMeta col : columns) {
            // 1. 敏感等级检查：列敏感等级数字越小越敏感，用户权限等级数字越大权限越低
            if (context.getUserPermissionLevel() != null && col.getSensitiveLevelId() != null
                    && col.getSensitiveLevelId() < context.getUserPermissionLevel()) {
                columnActions.put(col.getColumnName(), ACTION_HIDE);
                continue;
            }

            // 2. 查绑定关系
            StandardsDesensitizeAssetcolumnDO binding = bindingService.getDgDesensitizeAssetcolumnByAid(col.getColumnId());
            if (binding == null) {
                columnActions.put(col.getColumnName(), ACTION_PASS);
                continue;
            }

            // 3. 查脱敏规则
            StandardsDesensitizeRuleDO rule = ruleService.getDgDesensitizeRuleByDataCategoryId(binding.getDataCategoryId());
            if (rule == null) {
                columnActions.put(col.getColumnName(), ACTION_PASS);
                continue;
            }

            // 4. 检查规则有效 + 应用场景匹配
            if (!rule.getValidFlag() || (context.getScene() != null && !rule.getApplicationScene().contains(context.getScene()))) {
                columnActions.put(col.getColumnName(), ACTION_PASS);
                continue;
            }

            // 5. 区间替换 or 隐藏列
            if (rule.getIntervalList() != null && rule.getIntervalList().size() > 0) {
                columnActions.put(col.getColumnName(), ACTION_REPLACE);
                replaceParams.put(col.getColumnName(), DesensitizeResult.ReplaceParam.builder()
                        .replaceContent(rule.getReplaceContent())
                        .intervals(rule.getIntervalList())
                        .build());
            } else {
                columnActions.put(col.getColumnName(), ACTION_HIDE);
            }

            // 6. 白名单检查：命中则覆盖为放行
            StandardsDesensitizeWhitelistDO white = whitelistService.getDgDesensitizeWhitelistByCategoryId(binding.getDataCategoryId());
            if (white != null && white.getValidFlag()) {
                Date now = new Date();
                boolean inTimeRange = !now.before(white.getStartTime()) && !now.after(white.getEndTime());
                boolean userInWhite = white.getUserList() != null
                        && white.getUserList().stream().anyMatch(rel -> rel.getUserId() == context.getUserId());
                if (inTimeRange && userInWhite) {
                    columnActions.put(col.getColumnName(), ACTION_PASS);
                    replaceParams.remove(col.getColumnName());
                }
            }
        }

        DesensitizeResult result = new DesensitizeResult();
        result.setColumnActions(columnActions);
        result.setReplaceParams(replaceParams);
        return result;
    }

    // ======================== 执行脱敏替换 ========================

    /**
     * 根据脱敏计算结果，对数据行执行脱敏操作
     *
     * @param data   原始数据行
     * @param result computeActions() 的返回值
     * @return 脱敏后的数据行（action=3 的列会被移除）
     */
    public List<Map<String, Object>> applyMasking(List<Map<String, Object>> data, DesensitizeResult result) {
        if (data == null || result == null) return data;

        Map<String, Integer> actions = result.getColumnActions();
        Map<String, DesensitizeResult.ReplaceParam> replaceParams = result.getReplaceParams();

        List<Map<String, Object>> out = new ArrayList<>(data.size());
        for (Map<String, Object> row : data) {
            Map<String, Object> masked = new LinkedHashMap<>(row.size());
            for (Map.Entry<String, Object> e : row.entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                Integer action = actions.get(key);

                if (action == null || action == ACTION_PASS) {
                    masked.put(key, val);
                } else if (action == ACTION_REPLACE) {
                    DesensitizeResult.ReplaceParam param = replaceParams.get(key);
                    if (param != null && val instanceof String) {
                        masked.put(key, desensitizeByInterval(
                                (String) val, param.getReplaceContent(), param.getIntervals()));
                    } else {
                        masked.put(key, val);
                    }
                }
                // ACTION_HIDE → 跳过，不放入结果
            }
            out.add(masked);
        }
        return out;
    }

    // ======================== 区间替换算法 ========================

    /**
     * 区间替换脱敏：按 intervalNo 排序后逐个替换
     *
     * @param originalStr  原始字符串
     * @param replaceStr   替换内容，如 "*"
     * @param intervalList 区间列表
     * @return 脱敏后的字符串
     */
    public static String desensitizeByInterval(String originalStr, String replaceStr, List<StandardsDesensitizeIntervalDO> intervalList) {
        if (originalStr == null || originalStr.isEmpty()) return originalStr;
        if (replaceStr == null || replaceStr.isEmpty()) return originalStr;
        if (intervalList == null || intervalList.isEmpty()) return originalStr;

        List<StandardsDesensitizeIntervalDO> sortedList = new ArrayList<>(intervalList);
        sortedList.sort(Comparator.comparing(StandardsDesensitizeIntervalDO::getIntervalNo));

        StringBuilder sb = new StringBuilder(originalStr);
        int offset = 0;
        for (StandardsDesensitizeIntervalDO interval : sortedList) {
            Long s = interval.getStartNum();
            Long e = interval.getEndNum();
            if (s == null || e == null) continue;
            int start = s.intValue() - 1;
            int end = e.intValue() - 1;
            int len = end - start + 1;
            if (len <= 0) continue;
            int replaceStart = Math.max(start - offset, 0);
            sb.replace(replaceStart, replaceStart + len, replaceStr);
            offset += (len - 1);
        }
        return sb.toString();
    }
}
