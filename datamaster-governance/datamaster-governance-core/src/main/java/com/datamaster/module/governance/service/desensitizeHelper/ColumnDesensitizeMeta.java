package com.datamaster.module.governance.service.desensitizeHelper;

import lombok.Builder;
import lombok.Data;

/**
 * 单列脱敏元数据 — 调用方传入每列的基本信息
 *
 * @author DATAMASTER
 */
@Data
@Builder
public class ColumnDesensitizeMeta {

    /** 列名（用于匹配结果 Map 的 key，也是数据行中的字段名） */
    private String columnName;

    /** 资产字段ID（用于查 STD_DESENSITIZE_ASSETCOLUMN 绑定关系） */
    private Long columnId;

    /** 列敏感等级ID（可选，优先级高于脱敏规则：等级比用户权限高则直接隐藏） */
    private Long sensitiveLevelId;
}
