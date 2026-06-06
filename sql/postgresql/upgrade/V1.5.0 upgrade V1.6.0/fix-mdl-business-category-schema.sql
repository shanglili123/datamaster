-- ============================================================================
-- 数据主数据 V1.5.0 → V1.6.0 建模业务分类表结构修复
-- mdL_business_category 的 owner_user_id → owner_id，补齐缺失列
-- ============================================================================

ALTER TABLE public.mdl_business_category RENAME COLUMN owner_user_id TO owner_id;

ALTER TABLE public.mdl_business_category ADD COLUMN IF NOT EXISTS code varchar;
ALTER TABLE public.mdl_business_category ADD COLUMN IF NOT EXISTS parent_id bigint;
ALTER TABLE public.mdl_business_category ADD COLUMN IF NOT EXISTS sort_order integer;
ALTER TABLE public.mdl_business_category ADD COLUMN IF NOT EXISTS owner_phone varchar;
ALTER TABLE public.mdl_business_category ADD COLUMN IF NOT EXISTS domain_id bigint;
