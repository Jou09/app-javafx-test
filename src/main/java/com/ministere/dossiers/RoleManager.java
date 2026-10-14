package com.ministere.dossiers;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

/** Détermine le rôle d'un utilisateur à partir de son appartenance aux groupes LDAP. */
public final class RoleManager {
    /** Rôles utilisés pour adapter l'affichage et protéger les actions de modération. */
    public enum Role {
        ADMIN, PERSONNEL
    }

    /** Cette classe ne contient que des opérations statiques. */
    private RoleManager() {
    }

    /**
     * Vérifie si le DN de l'utilisateur figure dans le groupe LDAP d'administration.
     * Toute personne qui n'appartient pas à ce groupe reçoit le rôle PERSONNEL.
     */
    public static Role getUserRole(String userDn) throws NamingException {
        DirContext context = LdapAuth.openServiceContext();
        NamingEnumeration<SearchResult> results = null;
        try {
            results = context.search(
                    AppConfig.ldapGroupsDn(),
                    "(&(objectClass=groupOfNames)(cn={0})(member={1}))",
                    new Object[]{AppConfig.ldapAdminGroup(), userDn},
                    LdapAuth.searchControls());
            return results.hasMore() ? Role.ADMIN : Role.PERSONNEL;
        } finally {
            LdapAuth.close(context, results);
        }
    }
}
