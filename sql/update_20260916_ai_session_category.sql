-- 统一 AI 问数与自主 Data Agent 会话分类
ALTER TABLE IF EXISTS ai_ask_session
    ADD COLUMN IF NOT EXISTS category VARCHAR(32) DEFAULT 'ASK_DATA';

UPDATE ai_ask_session
SET category = 'ASK_DATA'
WHERE category IS NULL;

COMMENT ON COLUMN ai_ask_session.category IS '会话分类：ASK_DATA 或 AUTONOMOUS_AGENT';
