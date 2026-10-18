# Application de gestion PRMP

Application JavaFX de démonstration avec authentification LDAP, publications MariaDB et modération réservée aux membres du groupe LDAP `Admin`.

## Interface

L'interface utilise une palette professionnelle bleu (`#408ec6`), indigo (`#1e2761`) et bordeaux (`#7a2048`), avec des icônes vectorielles pour les champs et les actions. Sur l'écran de connexion, le bouton « Afficher » / « Masquer » révèle temporairement le mot de passe ; il reste masqué par défaut.

## Prérequis

- Java 25
- Maven 3.9 ou ultérieur
- Un serveur LDAP accessible et une base MariaDB `gestion_dossiers`
- Un compte LDAP de service autorisé à rechercher les utilisateurs et groupes
- Un compte MariaDB autorisé à lire et modifier la table `posts`

Au premier démarrage, l'application demande les mots de passe du compte de service LDAP et du compte MariaDB, puis les enregistre dans le coffre de mots de passe de la session utilisateur (Gestionnaire d'identifiants Windows ou coffre Secret Service/KWallet sous Linux). Ils ne sont ni intégrés à l'installateur ni écrits dans un fichier de configuration.

Les variables d'environnement et propriétés Java restent disponibles pour les environnements de développement ou de déploiement automatisé, et sont prioritaires sur les secrets du coffre :

```zsh
read -s "LDAP_BIND_PASSWORD?Mot de passe du compte LDAP de service : "; print
export LDAP_BIND_PASSWORD
read -s "DB_PASSWORD?Mot de passe MariaDB : "; print
export DB_PASSWORD
```

Les paramètres par défaut correspondent à l'infrastructure de démonstration. Ils peuvent être surchargés par des variables d'environnement ou des propriétés Java :

| Variable d'environnement | Propriété Java | Valeur par défaut |
| --- | --- | --- |
| `LDAP_URL` | `app.ldap.url` | `ldap://192.168.4.119:389` |
| `LDAP_BASE_DN` | `app.ldap.base-dn` | `dc=prmp,dc=local` |
| `LDAP_PEOPLE_DN` | `app.ldap.people-dn` | `ou=People,` suivi du DN de base |
| `LDAP_GROUPS_DN` | `app.ldap.groups-dn` | `ou=Groups,` suivi du DN de base |
| `LDAP_ADMIN_GROUP` | `app.ldap.admin-group` | `Admin` |
| `LDAP_BIND_DN` | `app.ldap.bind-dn` | `cn=admin,` suivi du DN de base |
| `LDAP_BIND_PASSWORD` | `app.ldap.bind-password` | Obligatoire |
| `DB_URL` | `app.db.url` | `jdbc:mariadb://192.168.4.119:3306/gestion_dossiers` |
| `DB_USER` | `app.db.user` | `app_user` |
| `DB_PASSWORD` | `app.db.password` | Obligatoire |

## Préparer la base

Depuis la machine où le fichier SQL est disponible, exécutez le schéma avec un compte MariaDB autorisé à créer la table :

```sh
mariadb -h 192.168.4.119 -u <compte_administrateur> -p gestion_dossiers < sql/schema.sql
```

Pour charger une fois les trois publications de démonstration, exécutez aussi :

```sh
mariadb -h 192.168.4.119 -u <compte_administrateur> -p gestion_dossiers < sql/demo-data.sql
```

Ce script ajoute les exemples à chaque exécution. Les utilisateurs peuvent aussi créer leurs propres publications depuis l'application.

Les administrateurs doivent être membres d'un groupe LDAP situé sous `LDAP_GROUPS_DN`, de type `groupOfNames`, dont le `cn` correspond à `LDAP_ADMIN_GROUP`. L'attribut `member` du groupe doit contenir le DN complet de l'utilisateur.

## Compiler et lancer

Depuis un terminal zsh, après avoir renseigné les deux variables de mot de passe :

```zsh
cd ~/mini-app-test
mvn clean package
mvn javafx:run
```

Un JAR avec dépendances est également créé dans `target/gestion-dossiers-desktop-1.0.0.jar`. Au premier démarrage, il utilise lui aussi l'assistant de configuration et le coffre système.

## Créer les applications natives

Les paquets natifs embarquent l'application et un runtime Java : l'utilisateur n'a pas besoin d'installer Java ou Maven. Le premier lancement demande les mots de passe de service et les conserve dans le coffre système. Le serveur LDAP, le serveur MariaDB et les comptes associés restent des prérequis réseau ; leurs secrets ne sont jamais intégrés aux exécutables.

### Kali Linux

Depuis Kali avec Java 25, Maven, `dpkg-deb` et un trousseau Secret Service ou KWallet de session :

```sh
./scripts/package-linux.sh
```

Le script produit l'application autonome et le paquet installable :

- `target/dist/kali/image/app-javafx-test/` : application autonome avec son runtime Java ;
- `target/dist/kali/deb/app-javafx-test_1.0.0_amd64.deb` : paquet Debian avec raccourci et icône.

Installez le paquet avec `sudo apt install ./target/dist/kali/deb/app-javafx-test_1.0.0_amd64.deb`.

### Windows

Sur Windows 10/11 x64, avec Java 25, Maven et WiX Toolset 3 dans le `PATH`, ouvrez PowerShell à la racine du projet et lancez :

```powershell
.\scripts\package-windows.ps1
```

L'installateur `.exe`, son runtime Java et son raccourci sont créés dans `target\dist\windows\`. Un workflow GitHub Actions (`Paquet Windows`) sait également le construire sur un runner Windows : lancez-le depuis l'onglet Actions du dépôt et téléchargez l'artefact `app-javafx-test-windows` une fois le workflow terminé.

L'installateur Windows n'est pas signé par un certificat éditeur ; Windows peut donc afficher un avertissement SmartScreen. Une signature nécessite un certificat de signature de code. Le coffre natif protège les secrets selon les garanties du compte et du système de l'utilisateur ; les exécutables ne peuvent pas protéger un secret contre un programme malveillant exécuté avec les mêmes droits utilisateur.

Utilisez LDAPS ou StartTLS pour les connexions LDAP en dehors d'un réseau de démonstration isolé.
