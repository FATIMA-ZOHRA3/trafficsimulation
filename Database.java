package com.example.demo;

import java.sql.*;

public class Database {

    private static final String URL =
            "jdbc:mysql://localhost:3307/trafficsimulation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ Driver MySQL chargé.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ Impossible de charger le driver MySQL !", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
