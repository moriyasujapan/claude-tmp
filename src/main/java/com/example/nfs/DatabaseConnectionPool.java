package com.example.nfs;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HikariCPを使用したデータベースコネクションプール
 * シングルトンパターンで実装
 */
public class DatabaseConnectionPool {
    private static final Logger logger = Logger.getLogger(DatabaseConnectionPool.class.getName());

    private static volatile DatabaseConnectionPool instance;
    private final HikariDataSource dataSource;

    /**
     * プライベートコンストラクタ
     */
    private DatabaseConnectionPool(Properties properties) {
        HikariConfig config = new HikariConfig();

        // JDBC接続設定
        config.setJdbcUrl(properties.getProperty("db.url"));
        config.setUsername(properties.getProperty("db.username"));
        config.setPassword(properties.getProperty("db.password"));

        // HikariCP設定
        config.setMaximumPoolSize(Integer.parseInt(
            properties.getProperty("db.pool.maxPoolSize", "10")));
        config.setMinimumIdle(Integer.parseInt(
            properties.getProperty("db.pool.minimumIdle", "2")));
        config.setConnectionTimeout(Long.parseLong(
            properties.getProperty("db.pool.connectionTimeout", "30000")));
        config.setIdleTimeout(Long.parseLong(
            properties.getProperty("db.pool.idleTimeout", "600000")));
        config.setMaxLifetime(Long.parseLong(
            properties.getProperty("db.pool.maxLifetime", "1800000")));

        // 接続テスト
        config.setConnectionTestQuery(
            properties.getProperty("db.pool.connectionTestQuery", "SELECT 1"));

        // プール名
        config.setPoolName("XMLProcessorPool");

        // データソースを作成
        this.dataSource = new HikariDataSource(config);

        logger.info("HikariCPコネクションプールを初期化しました: " + config.getJdbcUrl());
    }

    /**
     * インスタンスを取得（シングルトン）
     */
    public static DatabaseConnectionPool getInstance(Properties properties) {
        if (instance == null) {
            synchronized (DatabaseConnectionPool.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionPool(properties);
                }
            }
        }
        return instance;
    }

    /**
     * コネクションを取得
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * プールの状態を取得
     */
    public String getPoolStatus() {
        return String.format("Pool Status - Active: %d, Idle: %d, Total: %d, Waiting: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * プールをシャットダウン
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("HikariCPコネクションプールをシャットダウンします");
            dataSource.close();
        }
    }

    /**
     * プールが有効かチェック
     */
    public boolean isValid() {
        return dataSource != null && !dataSource.isClosed();
    }
}
