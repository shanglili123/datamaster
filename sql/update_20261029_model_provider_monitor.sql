-- 先执行 update_20261028_ai_model_invocation.sql，再执行本迁移。历史供应商未知不回填猜测值。
BEGIN;
ALTER TABLE public.ai_model_invocation ADD COLUMN IF NOT EXISTS provider varchar(64);
CREATE INDEX IF NOT EXISTS idx_ai_invocation_provider ON public.ai_model_invocation(space_id, provider, model, created_at DESC);
-- 独立模型配置权限：挂在现有服务监控页面下，不猜固定菜单ID，不默认授权普通角色。
DO $$
DECLARE parent bigint; next_id bigint; item record;
BEGIN
 SELECT min(menu_id) INTO parent FROM public.system_menu WHERE perms='monitor:server:list' AND menu_type='C';
 IF parent IS NULL THEN
   RAISE NOTICE '未找到服务监控菜单，模型配置权限按钮需管理员手工登记';
 ELSE
   LOCK TABLE public.system_menu IN SHARE ROW EXCLUSIVE MODE;
   FOR item IN SELECT * FROM (VALUES ('ai:model-config:list','模型配置查看'),('ai:model-config:add','新增模型配置'),('ai:model-config:edit','修改切换模型'),('ai:model-config:remove','删除模型配置')) AS x(perm,title)
   LOOP
     IF NOT EXISTS(SELECT 1 FROM public.system_menu WHERE perms=item.perm) THEN
       SELECT coalesce(max(menu_id),0)+1 INTO next_id FROM public.system_menu;
       INSERT INTO public.system_menu(menu_id,menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
       VALUES(next_id,item.title,parent,95,'#',1,0,'F','0','0',item.perm,'#','system',CURRENT_TIMESTAMP);
     END IF;
   END LOOP;
 END IF;
END $$;
-- 仅精确替换原AI问数菜单标题，不改DB-GPT兼容路由。
UPDATE public.system_menu SET menu_name='数据分析' WHERE perms='ai:ask-data:query' AND menu_type='C' AND menu_name IN ('AI问数','决策智能体','DBGPT','DB-GPT');
COMMIT;
