-- 用户信息表
CREATE TABLE IF NOT EXISTS test_users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(200),
    age INTEGER,
    salary NUMERIC(12,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO test_users (name, email, age, salary) VALUES
('张三', 'zhangsan@example.com', 28, 15000.00),
('李四', 'lisi@example.com', 35, 22000.00),
('王五', 'wangwu@example.com', 42, 18000.50);

-- 订单表
CREATE TABLE IF NOT EXISTS test_orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES test_users(id),
    order_no VARCHAR(50) NOT NULL,
    product_name VARCHAR(200),
    quantity INTEGER DEFAULT 1,
    amount NUMERIC(12,2),
    status VARCHAR(20) DEFAULT 'pending',
    order_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO test_orders (user_id, order_no, product_name, quantity, amount, status) VALUES
(1, 'ORD20240001', '笔记本电脑', 1, 5999.00, 'completed'),
(1, 'ORD20240002', '机械键盘', 2, 1200.00, 'completed'),
(2, 'ORD20240003', '显示器', 1, 2499.00, 'pending'),
(3, 'ORD20240004', '鼠标', 3, 450.00, 'cancelled'),
(2, 'ORD20240005', '耳机', 1, 899.00, 'completed');

-- 产品分类表
CREATE TABLE IF NOT EXISTS test_categories (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    parent_id INTEGER REFERENCES test_categories(id),
    level INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO test_categories (code, name, parent_id, level) VALUES
('ELEC', '电子产品', NULL, 1),
('COMP', '电脑配件', 1, 2),
('PERI', '外设设备', 1, 2),
('SOFT', '软件服务', NULL, 1);

-- 日志表（分区表）
CREATE TABLE IF NOT EXISTS test_logs (
    id SERIAL,
    log_level VARCHAR(10) NOT NULL,
    message TEXT,
    source VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

CREATE TABLE IF NOT EXISTS test_logs_2024 PARTITION OF test_logs
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE IF NOT EXISTS test_logs_2025 PARTITION OF test_logs
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS test_logs_2026 PARTITION OF test_logs
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

INSERT INTO test_logs (log_level, message, source, created_at) VALUES
('INFO', '服务启动成功', 'api-server', '2025-01-15 10:00:00'),
('WARN', '数据库连接池使用率85%', 'db-pool', '2025-06-20 14:30:00'),
('ERROR', '连接超时', 'etl-worker', '2025-09-10 08:15:00'),
('INFO', '同步任务完成', 'sync-job', '2026-03-01 23:59:59'),
('DEBUG', '缓存命中率92%', 'cache-svc', '2026-05-15 16:45:00');

-- 大表（1万行）
CREATE TABLE IF NOT EXISTS test_large (
    id SERIAL PRIMARY KEY,
    col1 VARCHAR(50),
    col2 INTEGER,
    col3 NUMERIC(10,2),
    col4 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    col5 BOOLEAN DEFAULT false,
    col6 TEXT DEFAULT 'default_text',
    col7 VARCHAR(100)
);

INSERT INTO test_large (col1, col2, col3, col5, col7)
SELECT
    'val_' || i,
    i % 1000,
    random() * 10000,
    i % 2 = 0,
    'group_' || (i % 50)
FROM generate_series(1, 10000) i;

CREATE INDEX IF NOT EXISTS idx_large_col1 ON test_large(col1);
CREATE INDEX IF NOT EXISTS idx_large_col2 ON test_large(col2);
CREATE INDEX IF NOT EXISTS idx_large_col7 ON test_large(col7);

-- 验证
SELECT 'users' as tbl, COUNT(*) as cnt FROM test_users
UNION ALL SELECT 'orders', COUNT(*) FROM test_orders
UNION ALL SELECT 'categories', COUNT(*) FROM test_categories
UNION ALL SELECT 'logs', COUNT(*) FROM test_logs
UNION ALL SELECT 'large_table', COUNT(*) FROM test_large;
