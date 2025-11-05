package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;

import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MapTest extends Application {

    // Paramètres de connexion MySQL
    private final String url = "jdbc:mysql://localhost:3307/trafficsimulation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private final String user = "root";
    private final String password = ""; // mettre ton mot de passe

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();

        // === Test simple pour vérifier le Pane ===
        Polyline testLine = new Polyline();
        testLine.getPoints().addAll(
                100.0, 100.0,
                200.0, 150.0,
                300.0, 200.0
        );
        testLine.setStroke(Color.BLUE);
        testLine.setStrokeWidth(3);
        root.getChildren().add(testLine);
        // ========================================

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT points FROM route");

            // Pour trouver les limites des coordonnées
            double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
            double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

            // 1️⃣ Lire toutes les routes pour calculer les limites
            JSONArray[] allRoutes = new JSONArray[1000]; // prévoir assez d’espace
            int routeIndex = 0;

            while (rs.next()) {
                String pointsJson = rs.getString("points");
                if (pointsJson == null || pointsJson.equals("[]")) continue;

                JSONArray pointsArray = new JSONArray(pointsJson);
                allRoutes[routeIndex++] = pointsArray;

                for (int i = 0; i < pointsArray.length(); i++) {
                    JSONObject point = pointsArray.getJSONObject(i);
                    double lat = point.getDouble("lat");
                    double lon = point.getDouble("lon");

                    if (lat < minLat) minLat = lat;
                    if (lat > maxLat) maxLat = lat;
                    if (lon < minLon) minLon = lon;
                    if (lon > maxLon) maxLon = lon;
                }
            }

            double sceneWidth = 800;
            double sceneHeight = 600;

            double scaleX = sceneWidth / (maxLon - minLon + 0.0001);
            double scaleY = sceneHeight / (maxLat - minLat + 0.0001);

            // 2️⃣ Dessiner toutes les routes
            for (int i = 0; i < routeIndex; i++) {
                JSONArray pointsArray = allRoutes[i];
                Polyline polyline = new Polyline();
                polyline.setStroke(Color.RED);
                polyline.setStrokeWidth(2);

                for (int j = 0; j < pointsArray.length(); j++) {
                    JSONObject point = pointsArray.getJSONObject(j);
                    double lat = point.getDouble("lat");
                    double lon = point.getDouble("lon");

                    // Adapter automatiquement les coordonnées à l'écran
                    double x = (lon - minLon) * scaleX;
                    double y = sceneHeight - (lat - minLat) * scaleY;

                    polyline.getPoints().addAll(x, y);
                }

                root.getChildren().add(polyline);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Test Routes");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
