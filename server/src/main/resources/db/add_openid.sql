-- 为shop表添加openid字段（用于微信消息推送）
ALTER TABLE `shop` ADD COLUMN `openid` VARCHAR(100) COMMENT '微信openid' AFTER `deleted`;

-- 为driver表添加openid字段（用于微信消息推送）
ALTER TABLE `driver` ADD COLUMN `openid` VARCHAR(100) COMMENT '微信openid' AFTER `deleted`;

-- 为openid创建索引
CREATE INDEX `idx_openid` ON `shop` (`openid`);
CREATE INDEX `idx_openid` ON `driver` (`openid`);
