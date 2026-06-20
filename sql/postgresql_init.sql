CREATE DATABASE channel_bill;

\c channel_bill;

CREATE TABLE IF NOT EXISTS channel_bill (
  id BIGSERIAL PRIMARY KEY,
  merchant_order_no VARCHAR(64) NOT NULL,
  channel_order_no VARCHAR(64) NOT NULL,
  channel_code VARCHAR(32) NOT NULL,
  bill_amount NUMERIC(18,2) NOT NULL,
  currency VARCHAR(16) DEFAULT 'CNY',
  bill_status VARCHAR(32) NOT NULL,
  fee NUMERIC(18,2) DEFAULT 0,
  merchant_id VARCHAR(64),
  trade_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(256),
  deleted SMALLINT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_merchant_order_no ON channel_bill(merchant_order_no);
CREATE INDEX IF NOT EXISTS idx_channel_order_no ON channel_bill(channel_order_no);
CREATE INDEX IF NOT EXISTS idx_trade_time ON channel_bill(trade_time);
CREATE INDEX IF NOT EXISTS idx_bill_status ON channel_bill(bill_status);

COMMENT ON TABLE channel_bill IS '渠道账单表';
COMMENT ON COLUMN channel_bill.merchant_order_no IS '商户订单号';
COMMENT ON COLUMN channel_bill.channel_order_no IS '渠道订单号';
COMMENT ON COLUMN channel_bill.channel_code IS '渠道编码: ALIPAY, WECHAT';
COMMENT ON COLUMN channel_bill.bill_amount IS '账单金额';
COMMENT ON COLUMN channel_bill.bill_status IS '账单状态: SUCCESS, FAILED, PROCESSING, REFUND, CLOSED';
COMMENT ON COLUMN channel_bill.fee IS '手续费';

INSERT INTO channel_bill (merchant_order_no, channel_order_no, channel_code, bill_amount, bill_status, fee, merchant_id) VALUES
('ORD202401010001', 'ALI202401010001', 'ALIPAY', 100.00, 'TRADE_SUCCESS', 0.60, 'MCH001'),
('ORD202401010002', 'WX202401010002', 'WECHAT', 200.50, 'TRADE_SUCCESS', 1.20, 'MCH001'),
('ORD202401010003', 'ALI202401010003', 'ALIPAY', 50.00, 'TRADE_FAIL', 0, 'MCH002'),
('ORD202401010004', 'WX202401010004', 'WECHAT', 299.00, 'TRADE_SUCCESS', 1.80, 'MCH002'),
('ORD202401010006', 'ALI202401010006', 'ALIPAY', 88.00, 'TRADE_SUCCESS', 0.53, 'MCH003');
