package com.datamaster.module.ontology.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据到达触发本体动作的跨模块请求。
 */
@Data
public class ActionDataArrivalTriggerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 动作定义上的触发标识，通常为 Kafka topic。 */
    private String triggerRef;

    /** 数据事件原始业务载荷 JSON。 */
    private String inputParams;

    /** 目标对象稳定主键；联合主键使用规范化 JSON。 */
    private String objectKey;

    /** 来源事件唯一编号，Kafka 场景为 topic:partition:offset。 */
    private String eventId;

    /** 数据写入时所属空间上下文。 */
    private Long spaceId;

    /** 数据写入时所属空间编码。 */
    private String spaceCode;
}
