package com.example.demo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private WebEngine webEngine;
    private ComboBox<String> cityComboBox;
    private ComboBox<String> routeComboBox;
    private Label infoLabel; // Panel جانبي لعرض معلومات إضافية
    private Connection connection;

    @Override
    public void start(Stage stage) {

        // 🔹 Connexion à la base
        try {
            connection = Database.getConnection();
            System.out.println("✅ Connexion réussie à la base de données.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 🔹 ComboBox villes/routes
        cityComboBox = new ComboBox<>();
        cityComboBox.setPromptText("Sélectionner une ville");

        routeComboBox = new ComboBox<>();
        routeComboBox.setPromptText("Sélectionner une route");

        // 🔹 Panel جانبي لمعلومات route
        infoLabel = new Label("Sélectionnez une route pour voir les infos...");
        VBox sidePanel = new VBox(10, infoLabel);
        sidePanel.setPadding(new Insets(10));

        cityComboBox.setOnAction(e -> {
            String city = cityComboBox.getValue();
            if (city != null) {
                loadRoutes(city);
                centerMapOnCity(city);
            }
        });

        routeComboBox.setOnAction(e -> {
            String route = routeComboBox.getValue();
            if (route != null) showRoute(route);
        });

        HBox topBar = new HBox(10, cityComboBox, routeComboBox);
        topBar.setPadding(new Insets(10));

        // 🔹 WebView carte
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.loadContent(getMapHTML());

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(webView);
        root.setRight(sidePanel);

        stage.setScene(new Scene(root, 1300, 720));
        stage.setTitle("Simulation du Trafic - Maroc");
        stage.show();

        loadCities();
    }

    private void loadCities() {
        cityComboBox.getItems().clear();
        String sql = "SELECT nom FROM ville ORDER BY nom ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cityComboBox.getItems().add(rs.getString("nom"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadRoutes(String cityName) {
        routeComboBox.getItems().clear();
        String sql = """
                SELECT r.nom FROM route r
                JOIN route_ville rv ON r.id = rv.route_id
                JOIN ville v ON v.id = rv.ville_id
                WHERE v.nom = ?
                ORDER BY rv.ordre
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cityName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) routeComboBox.getItems().add(rs.getString("nom"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void centerMapOnCity(String cityName) {
        String sql = "SELECT lat, lon FROM ville WHERE nom = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cityName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double lat = rs.getDouble("lat");
                    double lon = rs.getDouble("lon");
                    webEngine.executeScript("centerMap(" + lat + "," + lon + ");");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showRoute(String routeName) {
        String sql = """
                SELECT rp.lat, rp.lon FROM route_points rp
                JOIN route r ON rp.route_id = r.id
                WHERE r.nom = ?
                ORDER BY rp.point_index
                """;
        List<String> points = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, routeName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) points.add(String.format("{lat:%f,lng:%f}", rs.getDouble("lat"), rs.getDouble("lon")));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (!points.isEmpty()) {
            String pointsJSArray = "[" + String.join(",", points) + "]";
            infoLabel.setText("Route sélectionnée: " + routeName + "\nPoints: " + points.size());
            webEngine.executeScript("showRoute('" + routeName + "', " + pointsJSArray + ");");
        } else {
            infoLabel.setText("Aucun point trouvé pour la route: " + routeName);
        }
    }

    private String getMapHTML() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8"/>
                    <link rel="stylesheet" href="https://unpkg.com/leaflet/dist/leaflet.css"/>
                    <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>
                    <style>html, body, #map { height:100%; margin:0; }</style>
                </head>
                <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([31.63, -8.0], 6);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{ maxZoom:19 }).addTo(map);

                    var routeLayer = null;
                    var marker = null;
                    var animationInterval = null;

                    function centerMap(lat, lng) {
                        map.setView([lat, lng], 12);
                    }

                    function showRoute(routeName, points) {
                        if(!points || points.length === 0) return;

                        if(routeLayer) map.removeLayer(routeLayer);
                        if(marker) map.removeLayer(marker);
                        if(animationInterval) clearInterval(animationInterval);

                        var latlngs = points.map(p => [p.lat, p.lng]);
                        routeLayer = L.polyline(latlngs, {color:'red', weight:4}).addTo(map);
                        map.fitBounds(routeLayer.getBounds());

                        // 🔹 marker animé
                        marker = L.circleMarker(latlngs[0], {radius:6, color:'blue'}).addTo(map);
                        marker.bindPopup("Route: " + routeName + "<br>Points: " + latlngs.length).openPopup();

                        var i = 0;
                        animationInterval = setInterval(function() {
                            marker.setLatLng(latlngs[i]);
                            i++;
                            if(i >= latlngs.length) i = 0; // boucle continue
                        }, 500);

                        // 🔹 zoom automatique début route
                        map.setView(latlngs[0], 14);
                    }
                </script>
                </body>
                </html>
                """;
    }

    @Override
    public void stop() throws Exception {
        if (connection != null && !connection.isClosed()) connection.close();
        super.stop();
    }

    public static void main(String[] args) { launch(); }
}
