package com.ministere.dossiers;

/**
 * Centralise les paramètres de connexion à LDAP et MariaDB.
 *
 * <p>La priorité est donnée aux propriétés Java, puis aux variables
 * d'environnement. Les paramètres sensibles n'ont volontairement pas de
 * valeur par défaut.</p>
 */
final class AppConfig {
    private static final String DEFAULT_HOST = "192.168.4.119";
    private static final String DEFAULT_BASE_DN = "dc=prmp,dc=local";

    /** Empêche l'instanciation : cette classe ne contient que des méthodes utilitaires. */
    private AppConfig() {
    }

    /** Retourne l'adresse du serveur LDAP, avec la configuration de démonstration par défaut. */
    static String ldapUrl() {
        return value("app.ldap.url", "LDAP_URL", "ldap://" + DEFAULT_HOST + ":389");
    }

    /** Retourne le DN racine de l'annuaire LDAP. */
    static String ldapBaseDn() {
        return value("app.ldap.base-dn", "LDAP_BASE_DN", DEFAULT_BASE_DN);
    }

    /** Retourne le DN de la branche qui contient les comptes utilisateurs. */
    static String ldapPeopleDn() {
        return value("app.ldap.people-dn", "LDAP_PEOPLE_DN", "ou=People," + ldapBaseDn());
    }

    /** Retourne le DN de la branche qui contient les groupes LDAP. */
    static String ldapGroupsDn() {
        return value("app.ldap.groups-dn", "LDAP_GROUPS_DN", "ou=Groups," + ldapBaseDn());
    }

    /** Retourne le nom du groupe LDAP dont les membres ont le rôle administrateur. */
    static String ldapAdminGroup() {
        return value("app.ldap.admin-group", "LDAP_ADMIN_GROUP", "Admin");
    }

    /** Retourne le DN utilisé par le compte de service pour interroger LDAP. */
    static String ldapBindDn() {
        return value("app.ldap.bind-dn", "LDAP_BIND_DN", "cn=admin," + ldapBaseDn());
    }

    /** Lit le mot de passe LDAP depuis les paramètres externes ou le coffre système. */
    static String ldapBindPassword() {
        return secret("app.ldap.bind-password", "LDAP_BIND_PASSWORD", CredentialVault.ldapAccount());
    }

    /** Retourne l'URL JDBC de MariaDB. */
    static String databaseUrl() {
        return value("app.db.url", "DB_URL",
                "jdbc:mariadb://" + DEFAULT_HOST + ":3306/gestion_dossiers");
    }

    /** Retourne le nom du compte MariaDB utilisé par l'application. */
    static String databaseUser() {
        return value("app.db.user", "DB_USER", "app_user");
    }

    /** Lit le mot de passe MariaDB depuis les paramètres externes ou le coffre système. */
    static String databasePassword() {
        return secret("app.db.password", "DB_PASSWORD", CredentialVault.databaseAccount());
    }

    /**
     * Résout un secret : propriété Java, variable d'environnement, puis coffre natif.
     * Le secret n'est jamais inclus dans le code ni dans l'installateur.
     */
    private static String secret(String property, String environmentVariable, String account) {
        String configured = System.getProperty(property);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(environmentVariable);
        }
        return configured == null || configured.isBlank() ? CredentialVault.get(account) : configured;
    }

    /** Indique si les deux secrets sont déjà fournis par propriétés Java ou variables d'environnement. */
    static boolean hasExternalCredentials() {
        return hasExternalValue("app.ldap.bind-password", "LDAP_BIND_PASSWORD")
                && hasExternalValue("app.db.password", "DB_PASSWORD");
    }

    private static boolean hasExternalValue(String property, String environmentVariable) {
        String configured = System.getProperty(property);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(environmentVariable);
        }
        return configured != null && !configured.isBlank();
    }

    /**
     * Résout un paramètre selon l'ordre suivant : propriété Java, variable
     * d'environnement, puis valeur par défaut.
     */
    private static String value(String property, String environmentVariable, String defaultValue) {
        String configured = System.getProperty(property);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(environmentVariable);
        }
        return configured == null || configured.isBlank() ? defaultValue : configured;
    }
}
