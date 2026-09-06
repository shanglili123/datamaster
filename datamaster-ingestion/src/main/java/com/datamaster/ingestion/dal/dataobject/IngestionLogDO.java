package com.datamaster.ingestion.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 数据接收处理日志 DO
 *
 * <p>对应表 INGESTION_LOG。记录每条 Kafka 消息的冲突检查与写入数仓结果，用于回溯与排查。</p>
 */
@Data
@TableName("INGESTION_LOG")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IngestionLogDO extends BaseEntity {

    /** 来源 Kafka topic */
    private String topic;

    /** 消息分区 */
    private Integer partition;

    /** 消息偏移量（字段名避开 PostgreSQL 保留字 OFFSET） */
    private Long msgOffset;

    /** 目标写入表 */
    private String targetTable;

    /** 处理状态：SUCCESS / CONFLICT_SKIP / CONFLICT_ERROR / FAILED */
    private String status;

    /** 冲突策略应用结果：INSERT / UPSERT / SKIP / ERROR */
    private String conflictAction;

    /** 记录主键值（冲突判定依据，JSON） */
    private String keyValue;

    /** 入参消息体（JSON，截断存储） */
    private String messageBody;

    /** 响应说明/错误信息 */
    private String message;

    @TableLogic
    private Integer delFlag;
}