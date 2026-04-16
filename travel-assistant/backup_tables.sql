-- 备份现有数据库表的SQL脚本
-- 执行日期: 2026-04-14

USE travel_assistant;

-- 备份所有现有表（重命名为 _backup_YYYYMMDD 格式）
RENAME TABLE t_user TO _backup_t_user_20240414;
RENAME TABLE t_coupon_template TO _backup_t_coupon_template_20240414;
RENAME TABLE t_group_buy_activity TO _backup_t_group_buy_activity_20240414;
RENAME TABLE t_group_buy_member TO _backup_t_group_buy_member_20240414;
RENAME TABLE t_group_buy_team TO _backup_t_group_buy_team_20240414;
RENAME TABLE t_member_level_config TO _backup_t_member_level_config_20240414;
RENAME TABLE t_menu TO _backup_t_menu_20240414;
RENAME TABLE t_operation_log TO _backup_t_operation_log_20240414;
RENAME TABLE t_order TO _backup_t_order_20240414;
RENAME TABLE t_order_item TO _backup_t_order_item_20240414;
RENAME TABLE t_order_status_log TO _backup_t_order_status_log_20240414;
RENAME TABLE t_order_traveler TO _backup_t_order_traveler_20240414;
RENAME TABLE t_payment TO _backup_t_payment_20240414;
RENAME TABLE t_points_log TO _backup_t_points_log_20240414;
RENAME TABLE t_product_category TO _backup_t_product_category_20240414;
RENAME TABLE t_product_ext_flight TO _backup_t_product_ext_flight_20240414;
RENAME TABLE t_product_ext_hotel TO _backup_t_product_ext_hotel_20240414;
RENAME TABLE t_product_ext_ticket TO _backup_t_product_ext_ticket_20240414;
RENAME TABLE t_product_ext_tour TO _backup_t_product_ext_tour_20240414;
RENAME TABLE t_product_sku TO _backup_t_product_sku_20240414;
RENAME TABLE t_product_spu TO _backup_t_product_spu_20240414;
RENAME TABLE t_refund TO _backup_t_refund_20240414;
RENAME TABLE t_refund_rule TO _backup_t_refund_rule_20240414;
RENAME TABLE t_region TO _backup_t_region_20240414;
RENAME TABLE t_role TO _backup_t_role_20240414;
RENAME TABLE t_role_menu TO _backup_t_role_menu_20240414;
RENAME TABLE t_seckill_activity TO _backup_t_seckill_activity_20240414;
RENAME TABLE t_seckill_order TO _backup_t_seckill_order_20240414;
RENAME TABLE t_seckill_sku TO _backup_t_seckill_sku_20240414;
RENAME TABLE t_supplier TO _backup_t_supplier_20240414;

-- 显示备份完成的提示
SELECT 'Backup completed. All existing tables have been renamed with _backup_20240414 suffix.' AS Message;
