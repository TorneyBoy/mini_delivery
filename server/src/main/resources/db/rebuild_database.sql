-- =====================================================
-- delivery 数据库重建脚本
-- 基于小程序数据存储需求，整合所有表结构
-- 执行此脚本将删除并重建整个数据库
-- =====================================================

-- 设置字符集为 utf8mb4
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 设置宽松模式，避免数据长度问题
SET GLOBAL sql_mode = 'NO_ENGINE_SUBSTITUTION';
SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';

-- 删除已存在的数据库（谨慎操作！）
DROP DATABASE IF EXISTS delivery;

-- 创建数据库
CREATE DATABASE delivery DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE delivery;

-- =====================================================
-- 1. 总管理表 (admin)
-- 用途：存储总管理员账号信息
-- =====================================================
CREATE TABLE `admin` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `phone` VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总管理表';

-- =====================================================
-- 2. 分管理表 (branch_manager)
-- 用途：存储分店管理员信息，管理下属店铺和司机
-- =====================================================
CREATE TABLE `branch_manager` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `brand_name` VARCHAR(100) COMMENT '品牌名称',
    `phone` VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分管理表';

-- =====================================================
-- 3. 店铺表 (shop)
-- 用途：存储店铺信息，店铺可下单购买商品
-- =====================================================
CREATE TABLE `shop` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `branch_manager_id` BIGINT NOT NULL COMMENT '分管理ID',
    `name` VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `address` VARCHAR(500) NOT NULL COMMENT '地址',
    `latitude` DOUBLE DEFAULT NULL COMMENT '纬度',
    `longitude` DOUBLE DEFAULT NULL COMMENT '经度',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `show_price` TINYINT DEFAULT 1 COMMENT '是否显示单价 1-显示 0-不显示',
    `price_type` TINYINT DEFAULT 1 COMMENT '价格类型 1-老用户价 2-新用户价',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `openid` VARCHAR(100) COMMENT '微信openid（用于消息推送）',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    INDEX `idx_branch_manager_id` (`branch_manager_id`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`),
    INDEX `idx_openid` (`openid`),
    CONSTRAINT `fk_shop_branch_manager` FOREIGN KEY (`branch_manager_id`) REFERENCES `branch_manager` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- =====================================================
-- 4. 司机表 (driver)
-- 用途：存储司机信息，负责拣货和送货
-- =====================================================
CREATE TABLE `driver` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `branch_manager_id` BIGINT NOT NULL COMMENT '分管理ID',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `openid` VARCHAR(100) COMMENT '微信openid（用于消息推送）',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    INDEX `idx_branch_manager_id` (`branch_manager_id`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`),
    INDEX `idx_openid` (`openid`),
    CONSTRAINT `fk_driver_branch_manager` FOREIGN KEY (`branch_manager_id`) REFERENCES `branch_manager` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机表';

-- =====================================================
-- 5. 商品表 (product)
-- 用途：存储商品信息，支持老用户价和新用户价
-- =====================================================
CREATE TABLE `product` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `branch_manager_id` BIGINT NOT NULL COMMENT '分管理ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `category` VARCHAR(50) COMMENT '类别',
    `old_price` DECIMAL(10,2) COMMENT '老用户价格',
    `new_price` DECIMAL(10,2) COMMENT '新用户价格',
    `unit` VARCHAR(20) COMMENT '单位（斤/箱）',
    `description` TEXT COMMENT '商品说明',
    `image_url` VARCHAR(500) COMMENT '图片URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1-正常 0-下架',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    INDEX `idx_branch_manager_id` (`branch_manager_id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    CONSTRAINT `fk_product_branch_manager` FOREIGN KEY (`branch_manager_id`) REFERENCES `branch_manager` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- =====================================================
-- 6. 订单表 (order)
-- 用途：存储订单信息
-- 状态流转：待支付(0) -> 待分配(1) -> 待拣货(2) -> 待送货(3) -> 已完成(4) / 已取消(5)
-- =====================================================
CREATE TABLE `order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    `shop_id` BIGINT NOT NULL COMMENT '店铺ID',
    `driver_id` BIGINT COMMENT '司机ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0-待支付 1-待分配 2-待拣货 3-待送货 4-已完成 5-已取消',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `delivery_date` DATE NOT NULL COMMENT '收货日期',
    `received_at` DATETIME COMMENT '收货时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    INDEX `idx_order_no` (`order_no`),
    INDEX `idx_shop_id` (`shop_id`),
    INDEX `idx_driver_id` (`driver_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_delivery_date` (`delivery_date`),
    CONSTRAINT `fk_order_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`),
    CONSTRAINT `fk_order_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- =====================================================
-- 7. 订单商品关联表 (order_item)
-- 用途：存储订单中的商品明细
-- =====================================================
CREATE TABLE `order_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称（冗余存储）',
    `quantity` DECIMAL(10,2) NOT NULL COMMENT '数量',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价（下单时的价格）',
    `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_product_id` (`product_id`),
    CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品关联表';

-- =====================================================
-- 8. 账单表 (bill)
-- 用途：存储店铺账单信息，用于结算
-- =====================================================
CREATE TABLE `bill` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `bill_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '账单号',
    `shop_id` BIGINT NOT NULL COMMENT '店铺ID',
    `bill_date` DATE NOT NULL COMMENT '账单日期',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0-待支付 1-已支付',
    `send_status` TINYINT DEFAULT 0 COMMENT '发送状态 0-未发送 1-已发送',
    `sent_at` DATETIME COMMENT '发送时间',
    `paid_at` DATETIME COMMENT '支付时间',
    `wechat_transaction_id` VARCHAR(100) COMMENT '微信交易号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    INDEX `idx_bill_no` (`bill_no`),
    INDEX `idx_shop_id` (`shop_id`),
    INDEX `idx_bill_date` (`bill_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_send_status` (`send_status`),
    CONSTRAINT `fk_bill_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单表';

-- =====================================================
-- 9. 账单订单关联表 (bill_order)
-- 用途：关联账单和订单的多对多关系
-- =====================================================
CREATE TABLE `bill_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `bill_id` BIGINT NOT NULL COMMENT '账单ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_bill_id` (`bill_id`),
    INDEX `idx_order_id` (`order_id`),
    UNIQUE KEY `uk_bill_order` (`bill_id`, `order_id`),
    CONSTRAINT `fk_bill_order_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bill_order_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单订单关联表';

-- =====================================================
-- 10. 拣货清单表 (picking_list)
-- 用途：存储司机拣货清单，汇总需要拣货的商品
-- =====================================================
CREATE TABLE `picking_list` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `driver_id` BIGINT NOT NULL COMMENT '司机ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称（冗余存储）',
    `total_quantity` DECIMAL(10,2) NOT NULL COMMENT '总数量',
    `picked_quantity` DECIMAL(10,2) DEFAULT 0 COMMENT '已拣数量',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0-待拣货 1-已完成',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_driver_id` (`driver_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    CONSTRAINT `fk_picking_list_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
    CONSTRAINT `fk_picking_list_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拣货清单表';

-- =====================================================
-- 11. 送货清单表 (delivery_list)
-- 用途：存储司机送货清单，管理送货任务
-- =====================================================
CREATE TABLE `delivery_list` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `driver_id` BIGINT NOT NULL COMMENT '司机ID',
    `shop_id` BIGINT NOT NULL COMMENT '店铺ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0-待送货 1-已完成',
    `delivery_photo` VARCHAR(500) DEFAULT NULL COMMENT '送达照片URL',
    `completed_at` DATETIME COMMENT '完成时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_driver_id` (`driver_id`),
    INDEX `idx_shop_id` (`shop_id`),
    INDEX `idx_status` (`status`),
    CONSTRAINT `fk_delivery_list_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
    CONSTRAINT `fk_delivery_list_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='送货清单表';

-- =====================================================
-- 12. 送货清单订单关联表 (delivery_order)
-- 用途：关联送货清单和订单的多对多关系
-- =====================================================
CREATE TABLE `delivery_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `delivery_list_id` BIGINT NOT NULL COMMENT '送货清单ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_delivery_list_id` (`delivery_list_id`),
    INDEX `idx_order_id` (`order_id`),
    UNIQUE KEY `uk_delivery_order` (`delivery_list_id`, `order_id`),
    CONSTRAINT `fk_delivery_order_delivery_list` FOREIGN KEY (`delivery_list_id`) REFERENCES `delivery_list` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_delivery_order_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='送货清单订单关联表';

-- =====================================================
-- 13. 任务日志表 (task_log)
-- 用途：记录自动任务的执行日志
-- =====================================================
CREATE TABLE `task_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型',
    `executed_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    `status` TINYINT COMMENT '状态 0-失败 1-成功',
    `detail` TEXT COMMENT '执行详情',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_task_type` (`task_type`),
    INDEX `idx_executed_at` (`executed_at`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务日志表';

-- =====================================================
-- 14. 司机上传商品图片请求表 (product_image_request)
-- 用途：司机上传商品图片，等待分管理审核
-- =====================================================
CREATE TABLE `product_image_request` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `driver_id` BIGINT NOT NULL COMMENT '司机ID',
    `branch_manager_id` BIGINT NOT NULL COMMENT '分管理ID',
    `product_id` BIGINT COMMENT '商品ID（可选，如果是对现有商品上传图片）',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `category` VARCHAR(50) COMMENT '类别',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
    `description` TEXT COMMENT '描述说明',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0-待审核 1-已通过 2-已拒绝',
    `reject_reason` VARCHAR(255) COMMENT '拒绝原因',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `reviewed_by` BIGINT COMMENT '审核人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    INDEX `idx_driver_id` (`driver_id`),
    INDEX `idx_branch_manager_id` (`branch_manager_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    CONSTRAINT `fk_product_image_request_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`id`),
    CONSTRAINT `fk_product_image_request_branch_manager` FOREIGN KEY (`branch_manager_id`) REFERENCES `branch_manager` (`id`),
    CONSTRAINT `fk_product_image_request_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机上传商品图片请求表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入默认总管理账号（密码：123456，使用BCrypt加密）
INSERT INTO `admin` (`phone`, `password`) VALUES
('13800000000', '$2a$10$a5ZLLimxPmXSs2irg.JxH.DOYA3dPolxpPLEkn/8bBBDJ5hQc0rKK');

-- 插入测试分管理账号（密码：123456）
INSERT INTO `branch_manager` (`brand_name`, `phone`, `password`, `status`) VALUES
('测试分店', '13800000001', '$2a$10$a5ZLLimxPmXSs2irg.JxH.DOYA3dPolxpPLEkn/8bBBDJ5hQc0rKK', 1);

-- 插入测试司机账号（密码：123456，关联分管理ID=1）
INSERT INTO `driver` (`branch_manager_id`, `name`, `phone`, `password`, `status`) VALUES
(1, '测试司机', '13800000002', '$2a$10$a5ZLLimxPmXSs2irg.JxH.DOYA3dPolxpPLEkn/8bBBDJ5hQc0rKK', 1);

-- 插入测试店铺账号（密码：123456，关联分管理ID=1）
INSERT INTO `shop` (`branch_manager_id`, `name`, `address`, `phone`, `password`, `show_price`, `price_type`, `status`) VALUES
(1, '测试店铺', '测试地址123号', '13800000003', '$2a$10$a5ZLLimxPmXSs2irg.JxH.DOYA3dPolxpPLEkn/8bBBDJ5hQc0rKK', 1, 1, 1);

-- 插入测试商品数据（关联分管理ID=1）
INSERT INTO `product` (`branch_manager_id`, `name`, `category`, `old_price`, `new_price`, `unit`, `description`, `status`) VALUES
(1, '苹果', '水果', 5.00, 4.50, '斤', '新鲜红富士苹果', 1),
(1, '香蕉', '水果', 3.50, 3.00, '斤', '新鲜香蕉', 1),
(1, '西红柿', '蔬菜', 4.00, 3.50, '斤', '新鲜西红柿', 1),
(1, '黄瓜', '蔬菜', 3.00, 2.50, '斤', '新鲜黄瓜', 1),
(1, '猪肉', '肉类', 25.00, 22.00, '斤', '新鲜猪肉', 1);

-- =====================================================
-- 数据库重建完成
-- =====================================================
