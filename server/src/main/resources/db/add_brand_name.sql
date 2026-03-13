-- 添加品牌名称字段到 branch_manager 表
ALTER TABLE branch_manager ADD COLUMN brand_name VARCHAR(100) COMMENT '品牌名称' AFTER id;
