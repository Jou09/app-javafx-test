package com.ministere.dossiers;

import java.sql.Timestamp;

/** Représente une publication telle qu'elle est enregistrée dans la table posts. */
public final class Post {
    private final int id;
    private final String title;
    private final String content;
    private final String authorUid;
    private final String authorName;
    private final boolean hidden;
    private final Timestamp createdAt;

    /** Crée une représentation en mémoire d'une ligne de la table posts. */
    public Post(int id, String title, String content, String authorUid, String authorName,
                boolean hidden, Timestamp createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.hidden = hidden;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorUid() {
        return authorUid;
    }

    public String getAuthorName() {
        return authorName;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
