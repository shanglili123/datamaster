-- =============================================
-- 增量脚本: 本体关系关联表绑定 (PostgreSQL)
-- 新增表 ONT_RELATION_TABLE, 支持关系直接绑定关联表与字段
-- 执行前请备份数据库
-- 注意: 必须用应用连接账号执行 (dev 为 datamaster),
--       否则表 owner 错误会导致应用报 permission denied
-- 幂等: 可重复执行
-- 本文件为 UTF-8 编码, 执行前必须保证客户端按 UTF-8 读取:
--   psql    : 先执行 \encoding UTF8 (或 PGCLIENTENCODING=UTF8)
--             切勿用 PowerShell Get-Content 管道灌入(会按 GBK 误读导致中文注释乱码)
--   Navicat: 查询窗口编码选择 65001 (UTF-8)
-- =============================================
SET client_encoding = 'UTF8';

CREATE TABLE IF NOT EXISTS ONT_RELATION_TABLE (
    ID              BIGINT       PRIMARY KEY,
    RELATION_ID     BIGINT       NOT NULL,
    DATASOURCE_ID   BIGINT       NOT NULL,
    DATABASE_NAME   VARCHAR(100),
    TABLE_NAME      VARCHAR(100) NOT NULL,
    SCHEMA_NAME     VARCHAR(100),
    COLUMN_NAMES    TEXT,
    CREATOR_ID      BIGINT,
    CREATE_BY       VARCHAR(50),
    CREATE_TIME     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATER_ID      BIGINT,
    UPDATE_BY       VARCHAR(50),
    UPDATE_TIME     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    DEL_FLAG        SMALLINT     DEFAULT 0
);
COMMENT ON TABLE ONT_RELATION_TABLE IS '关系关联表绑定';
COMMENT ON COLUMN ONT_RELATION_TABLE.COLUMN_NAMES IS '已选物理字段名, JSON数组';
CREATE INDEX IF NOT EXISTS IDX_ONT_RTABLE_RID ON ONT_RELATION_TABLE(RELATION_ID);
