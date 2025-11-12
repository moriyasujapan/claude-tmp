-- =========================================
-- MySQL用 DDLスクリプト
-- XML処理履歴テーブルとサンプルデータテーブル
-- =========================================

-- データベース作成（必要に応じて）
-- CREATE DATABASE IF NOT EXISTS xmlprocessing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE xmlprocessing;

-- =========================================
-- XML処理履歴テーブル
-- =========================================
CREATE TABLE IF NOT EXISTS xml_processing_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    element_count INT NOT NULL DEFAULT 0,
    root_tag_name VARCHAR(100),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_processed_at (processed_at),
    INDEX idx_file_name (file_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='XML処理履歴を記録するテーブル';

-- =========================================
-- サンプル：書籍データテーブル
-- =========================================
CREATE TABLE IF NOT EXISTS xml_books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    book_id VARCHAR(50) NOT NULL,
    author VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    price DECIMAL(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_books_task_id (task_id),
    INDEX idx_books_book_id (book_id),
    INDEX idx_books_author (author)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='XMLから抽出された書籍データ';

-- =========================================
-- サンプル：社員データテーブル
-- =========================================
CREATE TABLE IF NOT EXISTS xml_employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    employee_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    department VARCHAR(100),
    position VARCHAR(100),
    salary DECIMAL(12, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_employees_task_id (task_id),
    INDEX idx_employees_employee_id (employee_id),
    INDEX idx_employees_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='XMLから抽出された社員データ';

-- =========================================
-- サンプル：商品データテーブル
-- =========================================
CREATE TABLE IF NOT EXISTS xml_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    brand VARCHAR(100),
    price DECIMAL(10, 2),
    currency VARCHAR(10),
    stock INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_products_task_id (task_id),
    INDEX idx_products_category (category),
    INDEX idx_products_brand (brand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='XMLから抽出された商品データ';

-- =========================================
-- サンプルデータ確認用ビュー
-- =========================================
CREATE OR REPLACE VIEW v_processing_summary AS
SELECT
    DATE(processed_at) as processing_date,
    root_tag_name,
    COUNT(*) as file_count,
    SUM(element_count) as total_elements,
    AVG(element_count) as avg_elements
FROM xml_processing_history
GROUP BY DATE(processed_at), root_tag_name
ORDER BY processing_date DESC, root_tag_name;

-- =========================================
-- テーブル確認
-- =========================================
-- SHOW TABLES;
-- DESCRIBE xml_processing_history;
-- SELECT * FROM v_processing_summary;
