-- Données facultatives pour illustrer les publications visibles et masquées.
-- Ce script ajoute les lignes à chaque exécution : ne le relancez pas pour éviter les doublons.
INSERT INTO posts (title, content, author_uid, author_name, is_hidden) VALUES
    ('Bienvenue sur le système',
     'Ceci est un message de bienvenue pour tous les utilisateurs.',
     'jdupont', 'Jean Dupont', FALSE),
    ('Note importante',
     'N''oubliez pas la réunion de demain à 10h.',
     'mmartin', 'Marie Martin', FALSE),
    ('Message masqué (test)',
     'Ce message ne doit être visible que par les admins.',
     'pdurand', 'Paul Durand', TRUE);
