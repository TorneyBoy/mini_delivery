-- 为bill表添加发送状态相关字段
-- 执行此脚本前请确保已备份数据库

USE delivery;

-- 添加发送状态字段
ALTER TABLE `bill` ADD COLUMN `send_status` TINYINT DEFAULT 0 COMMENT '发送状态 0-未发送 1-已发送' AFTER `status`;

-- 添加发送时间字段
ALTER TABLE `bill` ADD COLUMN `sent_at` DATETIME COMMENT '发送时间' AFTER `send_status`;

-- 添加索引
ALTER TABLE `bill` ADD INDEX `idx_send_status` (`send_status`);

-- 验证修改
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'delivery' AND TABLE_NAME = 'bill';
