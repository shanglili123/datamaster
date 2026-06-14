-- ============================================================================
-- 修复 pg_attribute 系统表与 MDL_BUSINESS_CATEGORY 表结构不一致问题
-- 
-- 错误信息：
--   pg_attribute catalog is missing 1 attribute(s) for relation OID
--
-- 原因：BaseEntity 定义 remark 字段，但 pg_attribute 缺少该列元数据。
--       通常发生在 ALTER TABLE 迁移后 pg_attribute 未同步的场景。
-- ============================================================================

-- 1. 确保 remark 列存在
ALTER TABLE public.mdl_business_category
    ADD COLUMN IF NOT EXISTS remark varchar(512);

COMMENT ON COLUMN public.mdl_business_category.remark IS '备注';

-- 2. 确保 code 列存在（部分环境可能遗漏）
ALTER TABLE public.mdl_business_category
    ADD COLUMN IF NOT EXISTS code varchar(256);

-- 3. 确保 parent_id 列存在
ALTER TABLE public.mdl_business_category
    ADD COLUMN IF NOT EXISTS parent_id bigint;

-- 4. 确保 sort_order 列存在
ALTER TABLE public.mdl_business_category
    ADD COLUMN IF NOT EXISTS sort_order integer;

-- 5. 确保 owner_phone 列存在
ALTER TABLE public.mdl_business_category
    ADD COLUMN IF NOT EXISTS owner_phone varchar(32);

-- 6. 确保 domain_id 列存在
ALTER TABLE public.mdl_business_category
    ADD COLUMN IF NOT EXISTS domain_id bigint;

-- 7. 确保 project_id 列存在
ALTER TABLE public.mdl_business_category
    ADD COLUMN IF NOT EXISTS project_id bigint;

-- 8. 确保 project_code 列存在
ALTER TABLE public.mdl_business_category
    ADD COLUMN IF NOT EXISTS project_code varchar(64);

-- 9. 修复 pg_attribute 系统表元数据
VACUUM ANALYZE public.mdl_business_category;
