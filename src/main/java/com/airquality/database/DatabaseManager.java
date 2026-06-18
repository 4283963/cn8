package com.airquality.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:./data/air_quality;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static Connection connection;

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    public static void initialize() {
        try {
            Class.forName("org.h2.Driver");
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS sensor_data (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "sensor_id VARCHAR(50) NOT NULL, " +
                        "location VARCHAR(100), " +
                        "formaldehyde DOUBLE NOT NULL, " +
                        "pm25 DOUBLE NOT NULL, " +
                        "timestamp TIMESTAMP NOT NULL)";
                stmt.execute(createTableSQL);
            }
            System.out.println("H2 数据库初始化成功");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("关闭数据库连接失败: " + e.getMessage());
        }
    }
}
