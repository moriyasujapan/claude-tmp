-- =========================================
-- PostgreSQL用 DDLスクリプト
-- XML処理履歴テーブルとサンプルデータテーブル
-- =========================================

-- データベース作成（必要に応じて）
-- CREATE DATABASE xmlprocessing;
-- \c xmlprocessing;

-- =========================================
-- XML処理履歴テーブル
-- =========================================
CREATE TABLE IF NOT EXISTS xml_processing_history (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    element_count INTEGER NOT NULL DEFAULT 0,
    root_tag_name VARCHAR(100),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- インデックス
CREATE INDEX idx_task_id ON xml_processing_history(task_id);
CREATE INDEX idx_processed_at ON xml_processing_history(processed_at);
CREATE INDEX idx_file_name ON xml_processing_history(file_name);

-- コメント
COMMENT ON TABLE xml_processing_history IS 'XML処理履歴を記録するテーブル';
COMMENT ON COLUMN xml_processing_history.task_id IS 'タスクID（UUID）';
COMMENT ON COLUMN xml_processing_history.file_name IS 'ファイル名';
COMMENT ON COLUMN xml_processing_history.file_path IS 'ファイルの絶対パス';
COMMENT ON COLUMN xml_processing_history.element_count IS 'XMLファイル内の要素数';
COMMENT ON COLUMN xml_processing_history.root_tag_name IS 'ルート要素のタグ名';
COMMENT ON COLUMN xml_processing_history.processed_at IS '処理日時';

-- =========================================
-- サンプル：書籍データテーブル
-- =========================================
CREATE TABLE IF NOT EXISTS xml_books (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    book_id VARCHAR(50) NOT NULL,
    author VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    price DECIMAL(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- インデックス
CREATE INDEX idx_books_task_id ON xml_books(task_id);
CREATE INDEX idx_books_book_id ON xml_books(book_id);
CREATE INDEX idx_books_author ON xml_books(author);

-- コメント
COMMENT ON TABLE xml_books IS 'XMLから抽出された書籍データ';
COMMENT ON COLUMN xml_books.task_id IS 'タスクID（処理履歴と紐づけ）';
COMMENT ON COLUMN xml_books.book_id IS '書籍ID（XML内のid属性）';

-- =========================================
-- サンプル：社員データテーブル
-- =========================================
CREATE TABLE IF NOT EXISTS xml_employees (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    employee_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    department VARCHAR(100),
    position VARCHAR(100),
    salary DECIMAL(12, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- インデックス
CREATE INDEX idx_employees_task_id ON xml_employees(task_id);
CREATE INDEX idx_employees_employee_id ON xml_employees(employee_id);
CREATE INDEX idx_employees_department ON xml_employees(department);

-- =========================================
-- サンプル：商品データテーブル
-- =========================================
CREATE TABLE IF NOT EXISTS xml_products (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    brand VARCHAR(100),
    price DECIMAL(10, 2),
    currency VARCHAR(10),
    stock INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- インデックス
CREATE INDEX idx_products_task_id ON xml_products(task_id);
CREATE INDEX idx_products_category ON xml_products(category);
CREATE INDEX idx_products_brand ON xml_products(brand);

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

COMMENT ON VIEW v_processing_summary IS 'XML処理のサマリービュー';

-- =========================================
-- テーブル確認
-- =========================================
-- \dt
-- \d xml_processing_history
-- SELECT * FROM v_processing_summary;
