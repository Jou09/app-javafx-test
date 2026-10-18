package com.ministere.dossiers;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;

/** Enregistre les secrets dans le coffre natif associé à la session de l'utilisateur. */
final class CredentialVault {
    private static final String SERVICE = "PRMP Gestion des dossiers";
    private static final String LDAP_ACCOUNT = "ldap-service";
    private static final String DATABASE_ACCOUNT = "mariadb-app";

    private CredentialVault() {
    }

    /** Indique si les deux mots de passe requis sont déjà présents dans le coffre. */
    static boolean isConfigured() {
        try (Keyring keyring = openKeyring()) {
            keyring.getPassword(SERVICE, LDAP_ACCOUNT);
            keyring.getPassword(SERVICE, DATABASE_ACCOUNT);
            return true;
        } catch (PasswordAccessException e) {
            return false;
        } catch (BackendNotSupportedException e) {
            throw new IllegalStateException(
                    "Aucun coffre de mots de passe compatible n'est disponible sur ce système.", e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible de fermer le coffre du système : " + e.getMessage(), e);
        }
    }

    /** Enregistre les mots de passe des comptes de service LDAP et MariaDB. */
    static void save(String ldapPassword, String databasePassword) {
        try (Keyring keyring = openKeyring()) {
            keyring.setPassword(SERVICE, LDAP_ACCOUNT, ldapPassword);
            keyring.setPassword(SERVICE, DATABASE_ACCOUNT, databasePassword);
        } catch (BackendNotSupportedException | PasswordAccessException e) {
            throw new IllegalStateException(
                    "Impossible d'enregistrer les mots de passe dans le coffre du système : "
                            + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible de fermer le coffre du système : " + e.getMessage(), e);
        }
    }

    /** Récupère un mot de passe déjà enregistré ; une configuration absente reste une erreur explicite. */
    static String get(String account) {
        try (Keyring keyring = openKeyring()) {
            return keyring.getPassword(SERVICE, account);
        } catch (BackendNotSupportedException | PasswordAccessException e) {
            throw new IllegalStateException(
                    "Mot de passe absent ou coffre système indisponible. Relancez la configuration initiale.",
                    e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible de fermer le coffre du système : " + e.getMessage(), e);
        }
    }

    /** Crée le connecteur vers le trousseau pris en charge par le système courant. */
    private static Keyring openKeyring() throws BackendNotSupportedException {
        return Keyring.create();
    }

    static String ldapAccount() {
        return LDAP_ACCOUNT;
    }

    static String databaseAccount() {
        return DATABASE_ACCOUNT;
    }
}
