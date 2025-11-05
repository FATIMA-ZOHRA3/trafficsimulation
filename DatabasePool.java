package com.example.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabasePool {

    private static final HikariConfig config = new HikariConfig();
    private static final HikariDataSource ds;

    static {
        config.setJdbcUrl("jdbc:mysql://localhost:3307/trafficsimulation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername("root");
        config.setPassword("");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        config.setLeakDetectionThreshold(15000);

        ds = new HikariDataSource(config);
        System.out.println("✅ HikariCP pool initialisé");
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
