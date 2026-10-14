package com.ministere.dossiers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** Fournit les connexions MariaDB partagées par les fonctions de l'application. */
public class DbConnection {
    /** Effectue une requête simple pour vérifier que la base répond. */
    public static String testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 'Connexion à MariaDB réussie !' AS message")) {
            if (rs.next()) {
                return "✅ " + rs.getString("message");
            }
            return "⚠️ Connexion établie, mais aucun résultat.";
        } catch (Exception e) {
            return "❌ Échec de la connexion BDD : " + e.getMessage();
        }
    }

    /** Ouvre une connexion en utilisant les paramètres configurés hors du code source. */
    static Connection getConnection() throws java.sql.SQLException {
        return DriverManager.getConnection(
                AppConfig.databaseUrl(),
                AppConfig.databaseUser(),
                AppConfig.databasePassword());
    }
}
