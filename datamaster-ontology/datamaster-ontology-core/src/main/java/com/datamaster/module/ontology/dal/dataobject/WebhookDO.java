package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Webhook 配置 DO
 *
 * <p>对应表 ONT_WEBHOOK。Webhook 是执行动作/决策后回写源系统（数据输出）的核心途径，
 * 回调内容基于「动作/决策执行记录」生成。</p>
 *
 * <p>绑定规则：<ul>
 *   <li>actionId 非空 —— 仅当某动作执行时触发（绑定单个动作）；</li>
 *   <li>actionId 为空且 ontologyId 非空 —— 该本体下所有动作执行时触发（本体级兜底）。</li>
 *   <li>enabled=false 时静默跳过。</li>
 * </ul></p>
 */
@Data
@TableName("ONT_WEBHOOK")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebhookDO extends BaseEntity {

    /** Webhook 名称 */
    private String name;

    /** 所属本体ID（可选，与 actionId 二选一或组合） */
    private Long ontologyId;

    /** 绑定的动作ID（可选，绑定单个动作执行） */
    private Long actionId;

    /** 回调地址 */
    private String url;

    /** 请求方法：POST/PUT，默认 POST */
    private String method;

    /** 请求头（JSON，可选），如 {"Authorization":"Bearer xxx"} */
    private String headers;

    /** 请求体模板（JSON 字符串，可选，空时按执行记录生成默认结构） */
    private String payloadTemplate;

    /** 回调密钥（可选，签名校验或 basic 鉴权） */
    private String secret;

    /** 是否启用：true 启用，false 停用 */
    private Boolean enabled;

    /** 失败最大重试次数（0 表示不重试） */
    private Integer maxRetry;

    /** 描述 */
    private String description;

    @TableLogic
    private Integer delFlag;
}