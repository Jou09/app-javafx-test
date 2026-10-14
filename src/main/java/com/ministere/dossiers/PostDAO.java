package com.ministere.dossiers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Regroupe les requêtes SQL qui lisent et modifient les publications. */
public final class PostDAO {
    /** Les requêtes sont exposées par les méthodes statiques de la classe. */
    private PostDAO() {
    }

    /**
     * Charge les publications les plus récentes.
     * Un administrateur voit aussi les publications masquées ; le personnel non.
     */
    public static List<Post> getPosts(boolean isAdmin) throws SQLException {
        String query = """
                SELECT id, title, content, author_uid, author_name, is_hidden, created_at
                FROM posts
                """ + (isAdmin ? "" : "WHERE is_hidden = FALSE\n")
                + "ORDER BY created_at DESC, id DESC";

        List<Post> posts = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(query)) {
            while (results.next()) {
                posts.add(new Post(
                        results.getInt("id"),
                        results.getString("title"),
                        results.getString("content"),
                        results.getString("author_uid"),
                        results.getString("author_name"),
                        results.getBoolean("is_hidden"),
                        results.getTimestamp("created_at")));
            }
        }
        return posts;
    }

    /** Enregistre une publication et son auteur dans MariaDB. */
    public static void createPost(String title, String content, String authorUid, String authorName)
            throws SQLException {
        String query = "INSERT INTO posts (title, content, author_uid, author_name) VALUES (?, ?, ?, ?)";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, authorUid);
            statement.setString(4, authorName);
            statement.executeUpdate();
        }
    }

    /** Inverse le statut masqué/visible d'une publication, après contrôle du rôle. */
    public static boolean toggleHidden(int postId, RoleManager.Role role) throws SQLException {
        requireAdmin(role);
        String query = "UPDATE posts SET is_hidden = NOT is_hidden WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, postId);
            return statement.executeUpdate() > 0;
        }
    }

    /** Supprime une publication après avoir vérifié que l'appelant est administrateur. */
    public static boolean deletePost(int postId, RoleManager.Role role) throws SQLException {
        requireAdmin(role);
        String query = "DELETE FROM posts WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, postId);
            return statement.executeUpdate() > 0;
        }
    }

    /** Modifie le titre et le contenu d'une publication après contrôle du rôle. */
    public static boolean updatePost(int postId, String title, String content, RoleManager.Role role)
            throws SQLException {
        requireAdmin(role);
        String query = "UPDATE posts SET title = ?, content = ? WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setInt(3, postId);
            return statement.executeUpdate() > 0;
        }
    }

    /** Refuse l'opération avant toute requête SQL si le rôle n'est pas ADMIN. */
    private static void requireAdmin(RoleManager.Role role) {
        if (role != RoleManager.Role.ADMIN) {
            throw new SecurityException("Cette opération est réservée aux administrateurs.");
        }
    }
}
