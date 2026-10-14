package com.ministere.dossiers;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;

/** Authentifie un compte utilisateur auprès de l'annuaire LDAP. */
public final class LdapAuth {
    /** L'authentification LDAP est fournie par les méthodes statiques de la classe. */
    private LdapAuth() {
    }

    /**
     * Recherche d'abord le compte avec le compte de service, puis tente une
     * connexion LDAP avec le DN et le mot de passe de l'utilisateur.
     *
     * @return l'identité LDAP si le compte existe et que le mot de passe est valide,
     *         ou {@code null} si aucun compte ne correspond à cet identifiant
     */
    public static UserIdentity authenticate(String uid, String password) throws NamingException {
        DirectoryUser user = findUserByUid(uid);
        if (user == null) {
            return null;
        }

        Hashtable<String, String> userEnvironment = new Hashtable<>();
        userEnvironment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        userEnvironment.put(Context.PROVIDER_URL, AppConfig.ldapUrl());
        userEnvironment.put(Context.SECURITY_AUTHENTICATION, "simple");
        userEnvironment.put(Context.SECURITY_PRINCIPAL, user.distinguishedName());
        userEnvironment.put(Context.SECURITY_CREDENTIALS, password);

        // Une connexion réussie avec le DN de l'utilisateur confirme son mot de passe.
        DirContext context = new InitialDirContext(userEnvironment);
        try {
            return new UserIdentity(uid, user.distinguishedName(), user.displayName());
        } finally {
            context.close();
        }
    }

    /**
     * Recherche un seul compte par identifiant et récupère son DN ainsi que
     * son nom d'affichage. Le filtre paramétré protège contre l'injection LDAP.
     */
    private static DirectoryUser findUserByUid(String uid) throws NamingException {
        DirContext context = openServiceContext();
        NamingEnumeration<SearchResult> results = null;
        try {
            results = context.search(AppConfig.ldapPeopleDn(), "(uid={0})",
                    new Object[]{uid}, searchControls());
            if (!results.hasMore()) {
                return null;
            }
            SearchResult result = results.next();
            // Un uid devrait être unique ; plusieurs résultats indiquent une configuration ambiguë.
            if (results.hasMore()) {
                throw new NamingException("L'identifiant LDAP correspond à plusieurs comptes.");
            }
            Attributes attributes = result.getAttributes();
            String displayName = attributes.get("cn") == null
                    ? uid
                    : attributes.get("cn").get().toString();
            return new DirectoryUser(result.getNameInNamespace(), displayName);
        } finally {
            close(context, results);
        }
    }

    /** Ouvre une session LDAP avec le compte de service configuré. */
    static DirContext openServiceContext() throws NamingException {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, AppConfig.ldapUrl());
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        environment.put(Context.SECURITY_PRINCIPAL, AppConfig.ldapBindDn());
        environment.put(Context.SECURITY_CREDENTIALS, AppConfig.ldapBindPassword());
        return new InitialDirContext(environment);
    }

    /** Prépare une recherche dans toute la sous-arborescence, limitée à deux résultats. */
    static SearchControls searchControls() {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setCountLimit(2);
        return controls;
    }

    /** Ferme les résultats et le contexte LDAP, y compris si la recherche échoue. */
    static void close(DirContext context, NamingEnumeration<?> results) throws NamingException {
        try {
            if (results != null) {
                results.close();
            }
        } finally {
            context.close();
        }
    }

    /** Informations utilisateur nécessaires après une authentification réussie. */
    public record UserIdentity(String uid, String distinguishedName, String displayName) {
    }

    /** Données minimales lues dans l'annuaire avant de vérifier le mot de passe. */
    private record DirectoryUser(String distinguishedName, String displayName) {
    }
}
