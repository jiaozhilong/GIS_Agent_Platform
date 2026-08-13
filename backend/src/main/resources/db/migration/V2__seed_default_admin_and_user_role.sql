INSERT INTO platform_roles (id, code, name, description, built_in) VALUES
('00000000-0000-0000-0000-000000000004', 'USER', '普通用户', '注册后默认角色，可查看基础项目、知识库和方案内容', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM platform_roles r CROSS JOIN platform_permissions p
WHERE r.code = 'USER' AND p.code IN ('dashboard:view', 'project:view', 'knowledge:view', 'proposal:view')
ON CONFLICT DO NOTHING;

INSERT INTO platform_users (id, username, display_name, email, department, password_hash, status)
SELECT
  '00000000-0000-0000-0000-000000000100',
  'admin',
  '系统管理员',
  'admin@gis-agent.local',
  '平台管理部',
  crypt(encode(gen_random_bytes(32), 'hex'), gen_salt('bf', 12)),
  'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM platform_users
  WHERE lower(username) = 'admin' OR lower(email) = 'admin@gis-agent.local'
);

UPDATE platform_users
SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE lower(username) = 'admin' AND status <> 'ACTIVE';

INSERT INTO platform_user_roles (user_id, role_id)
SELECT u.id, r.id FROM platform_users u CROSS JOIN platform_roles r
WHERE lower(u.username) = 'admin' AND r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
