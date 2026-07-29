-- 2026-07-28: rename menu display text from 数据质量 to 质量探查.
-- Run against the server PostgreSQL database after deploying the updated frontend/backend code.

UPDATE system_menu
SET
  menu_name = replace(menu_name, '数据质量', '质量探查'),
  remark = replace(remark, '数据质量', '质量探查')
WHERE
  menu_name LIKE '%数据质量%'
  OR remark LIKE '%数据质量%';

UPDATE system_dict_type
SET dict_name = replace(dict_name, '数据质量', '质量探查')
WHERE dict_name LIKE '%数据质量%';

UPDATE message_template
SET
  title = replace(title, '数据质量', '质量探查'),
  content = replace(content, '数据质量', '质量探查')
WHERE
  title LIKE '%数据质量%'
  OR content LIKE '%数据质量%';
