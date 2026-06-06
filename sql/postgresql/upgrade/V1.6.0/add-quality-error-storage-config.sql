-- 质量检测错误明细存储配置表
CREATE TABLE IF NOT EXISTS public.quality_error_storage_config (
    id              BIGINT NOT NULL,
    datasource_id   BIGINT NOT NULL,
    table_name      VARCHAR(128) NOT NULL DEFAULT 'quality_error_data',
    enabled         VARCHAR(1) NOT NULL DEFAULT '1',
    create_by       VARCHAR(64),
    creator_id      BIGINT,
    create_time     TIMESTAMP,
    update_by       VARCHAR(64),
    updater_id      BIGINT,
    update_time     TIMESTAMP,
    remark          VARCHAR(500),
    PRIMARY KEY (id)
);

COMMENT ON TABLE public.quality_error_storage_config IS '质量检测错误明细存储配置';
COMMENT ON COLUMN public.quality_error_storage_config.datasource_id IS '存储数据源ID';
COMMENT ON COLUMN public.quality_error_storage_config.table_name IS '存储表名';
COMMENT ON COLUMN public.quality_error_storage_config.enabled IS '是否启用 1-启用 0-禁用';
