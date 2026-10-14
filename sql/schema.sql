-- Crée la table des publications si elle n'existe pas déjà.
CREATE TABLE IF NOT EXISTS posts (
    -- Identifiant unique généré automatiquement.
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author_uid VARCHAR(100) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    -- Les publications sont visibles par défaut ; seuls les administrateurs voient celles qui sont masquées.
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- MariaDB actualise ce champ lors de chaque modification de la ligne.
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
