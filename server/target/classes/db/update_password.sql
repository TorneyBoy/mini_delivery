-- 更新总管理密码为 123456
-- BCrypt加密后的密码
UPDATE admin SET password = '$2a$10$a5ZLLimxPmXSs2irg.JxH.DOYA3dPolxpPLEkn/8bBBDJ5hQc0rKK' WHERE id = 1;

-- 验证更新
SELECT id, phone, password FROM admin WHERE id = 1;
