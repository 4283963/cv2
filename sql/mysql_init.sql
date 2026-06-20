CREATE DATABASE IF NOT EXISTS `payment_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `payment_order`;

CREATE TABLE IF NOT EXISTS `payment_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_order_no` varchar(64) NOT NULL COMMENT '商户订单号',
  `order_no` varchar(64) NOT NULL COMMENT '系统订单号',
  `amount` decimal(18,2) NOT NULL COMMENT '订单金额',
  `currency` varchar(16) DEFAULT 'CNY' COMMENT '币种',
  `status` varchar(32) NOT NULL COMMENT '订单状态: SUCCESS, FAILED, PROCESSING, REFUND, CLOSED',
  `pay_channel` varchar(32) DEFAULT NULL COMMENT '支付渠道',
  `merchant_id` varchar(64) DEFAULT NULL COMMENT '商户ID',
  `product_name` varchar(128) DEFAULT NULL COMMENT '商品名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `success_time` datetime DEFAULT NULL COMMENT '成功时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_order_no` (`merchant_order_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

CREATE TABLE IF NOT EXISTS `reconcile_diff` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_order_no` varchar(64) NOT NULL COMMENT '商户订单号',
  `diff_type` varchar(32) NOT NULL COMMENT '差异类型: ORDER_MISSING, CHANNEL_MISSING, DATA_DIFF',
  `order_amount` decimal(18,2) DEFAULT NULL COMMENT '订单侧金额',
  `channel_amount` decimal(18,2) DEFAULT NULL COMMENT '渠道侧金额',
  `order_status` varchar(32) DEFAULT NULL COMMENT '订单侧状态',
  `channel_status` varchar(32) DEFAULT NULL COMMENT '渠道侧状态',
  `diff_field` varchar(64) DEFAULT NULL COMMENT '差异字段',
  `diff_detail` varchar(512) DEFAULT NULL COMMENT '差异详情',
  `reconcile_batch_no` varchar(64) DEFAULT NULL COMMENT '对账批次号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_order_no` (`merchant_order_no`),
  KEY `idx_diff_type` (`diff_type`),
  KEY `idx_reconcile_batch_no` (`reconcile_batch_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账差异表';

CREATE TABLE IF NOT EXISTS `reconcile_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID(对外)',
  `task_status` varchar(32) NOT NULL COMMENT '任务状态: PENDING, PROCESSING, SUCCESS, FAILED',
  `task_type` varchar(32) DEFAULT 'BATCH' COMMENT '任务类型: SINGLE, BATCH',
  `start_date` varchar(16) DEFAULT NULL COMMENT '对账开始日期',
  `end_date` varchar(16) DEFAULT NULL COMMENT '对账结束日期',
  `reconcile_batch_no` varchar(64) DEFAULT NULL COMMENT '对账批次号',
  `total_count` int(11) DEFAULT 0 COMMENT '总记录数',
  `success_count` int(11) DEFAULT 0 COMMENT '一致数量',
  `diff_count` int(11) DEFAULT 0 COMMENT '差异数量',
  `failed_count` int(11) DEFAULT 0 COMMENT '处理失败数量',
  `progress_pct` decimal(5,2) DEFAULT 0.00 COMMENT '进度百分比',
  `error_msg` varchar(512) DEFAULT NULL COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `finish_time` datetime DEFAULT NULL COMMENT '任务结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账任务状态表';

INSERT INTO `payment_order` (`merchant_order_no`, `order_no`, `amount`, `status`, `pay_channel`, `merchant_id`, `product_name`) VALUES
('ORD202401010001', 'SYS202401010001', 100.00, 'SUCCESS', 'ALIPAY', 'MCH001', 'iPhone 15'),
('ORD202401010002', 'SYS202401010002', 200.50, 'SUCCESS', 'WECHAT', 'MCH001', 'MacBook Pro'),
('ORD202401010003', 'SYS202401010003', 50.00, 'FAILED', 'ALIPAY', 'MCH002', 'AirPods'),
('ORD202401010004', 'SYS202401010004', 300.00, 'SUCCESS', 'WECHAT', 'MCH002', 'iPad'),
('ORD202401010005', 'SYS202401010005', 150.00, 'PROCESSING', 'ALIPAY', 'MCH001', 'Apple Watch');
