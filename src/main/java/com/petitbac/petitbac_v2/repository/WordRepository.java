package com.petitbac.petitbac_v2.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

@Repository
public class WordRepository {

    // Lit le chemin depuis application.properties
    @Value("${app.db.path}")
    private String dbPath;

    private String getDbUrl() {
        return "jdbc:sqlite:" + System.getProperty("user.dir") + "/" + dbPath;
    }

    // Vérifie si un mot existe dans la BDD
    public boolean exists(String categorie, String mot) {
        if (mot == null || mot.trim().isEmpty()) return false;

        String query = "SELECT COUNT(*) FROM mots " +
                "WHERE LOWER(categorie) = LOWER(?) " +
                "AND LOWER(valeur) = LOWER(?)";

        try (Connection conn = DriverManager.getConnection(getDbUrl());
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, categorie.trim());
            stmt.setString(2, mot.trim());
            ResultSet rs = stmt.executeQuery();
            return rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.out.println("Erreur BDD : " + e.getMessage());
            return false;
        }
    }

    // Retourne tous les mots d'une catégorie
    public List<String> findByCategorie(String categorie) {
        List<String> mots = new ArrayList<>();
        String query = "SELECT valeur FROM mots " +
                "WHERE LOWER(categorie) = LOWER(?) " +
                "ORDER BY valeur";

        try (Connection conn = DriverManager.getConnection(getDbUrl());
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, categorie.trim());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mots.add(rs.getString("valeur"));
            }

        } catch (SQLException e) {
            System.out.println("Erreur BDD : " + e.getMessage());
        }
        return mots;
    }
}