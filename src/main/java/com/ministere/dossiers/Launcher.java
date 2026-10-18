package com.ministere.dossiers;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import javax.naming.AuthenticationException;
import javax.naming.NamingException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Construit les écrans JavaFX et relie les actions de l'utilisateur aux services métier. */
public class Launcher extends Application {
    // Identité et autorisations de la session après une connexion réussie.
    private String currentUserUid;
    private String currentUserName;
    private RoleManager.Role currentRole;

    // Références aux composants réutilisés par les actions de publication et d'administration.
    private VBox mainContainer;
    private TextArea postsArea;
    private TextField titleField;
    private TextArea contentField;
    private TextField postIdField;
    private TextField editTitleField;
    private TextArea editContentField;

    /** Prépare la fenêtre, charge le thème CSS puis affiche l'écran de connexion. */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Système de Gestion PRMP");
        // Charge l'icône depuis les ressources pour qu'elle fonctionne aussi depuis le JAR.
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/ic_1.png")));
        mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.getStyleClass().add("app-root");

        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("app-scroll");
        Scene scene = new Scene(scrollPane, 800, 650);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        primaryStage.setScene(scene);
        if (AppConfig.hasExternalCredentials()) {
            showLoginScreen(primaryStage);
        } else {
            try {
                if (CredentialVault.isConfigured()) {
                    showLoginScreen(primaryStage);
                } else {
                    showCredentialSetup();
                }
            } catch (IllegalStateException e) {
                showCredentialSetup(e.getMessage());
            }
        }
        primaryStage.show();
    }

    /** Demande une première fois les secrets et les conserve dans le coffre du système. */
    private void showCredentialSetup() {
        showCredentialSetup(null);
    }

    private void showCredentialSetup(String configurationError) {
        mainContainer.getChildren().clear();
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(32));

        Label brandIcon = new Label("P");
        brandIcon.getStyleClass().add("brand-mark");
        Label title = new Label("Configuration initiale");
        title.getStyleClass().add("login-title");
        Label description = new Label(
                "Saisissez les mots de passe de service. Ils seront conservés dans le coffre sécurisé de votre session.");
        description.setWrapText(true);
        description.getStyleClass().add("login-subtitle");

        PasswordField ldapPassword = new PasswordField();
        ldapPassword.setPromptText("Mot de passe du compte de service LDAP");
        PasswordField confirmLdapPassword = new PasswordField();
        confirmLdapPassword.setPromptText("Confirmer le mot de passe LDAP");
        PasswordField databasePassword = new PasswordField();
        databasePassword.setPromptText("Mot de passe du compte MariaDB");
        PasswordField confirmDatabasePassword = new PasswordField();
        confirmDatabasePassword.setPromptText("Confirmer le mot de passe MariaDB");

        Label status = new Label(configurationError == null ? "" : configurationError);
        status.setWrapText(true);
        status.getStyleClass().add("login-status");

        Button saveButton = new Button("Enregistrer et continuer");
        saveButton.getStyleClass().addAll("button", "button-primary", "login-button");
        saveButton.setOnAction(event -> {
            String ldapSecret = ldapPassword.getText();
            String databaseSecret = databasePassword.getText();
            if (ldapSecret.isBlank() || databaseSecret.isBlank()) {
                status.setText("Renseignez les deux mots de passe.");
                return;
            }
            if (!ldapSecret.equals(confirmLdapPassword.getText())
                    || !databaseSecret.equals(confirmDatabasePassword.getText())) {
                status.setText("Les confirmations ne correspondent pas aux mots de passe.");
                return;
            }

            saveButton.setDisable(true);
            status.setText("Enregistrement dans le coffre système…");
            try {
                CredentialVault.save(ldapSecret, databaseSecret);
                ldapPassword.clear();
                confirmLdapPassword.clear();
                databasePassword.clear();
                confirmDatabasePassword.clear();
                showLoginScreen((Stage) mainContainer.getScene().getWindow());
            } catch (IllegalStateException e) {
                status.setText(e.getMessage());
            } finally {
                saveButton.setDisable(false);
            }
        });

        VBox card = new VBox(12,
                brandIcon,
                title,
                description,
                fieldLabel("Mot de passe LDAP"),
                ldapPassword,
                confirmLdapPassword,
                fieldLabel("Mot de passe MariaDB"),
                databasePassword,
                confirmDatabasePassword,
                saveButton,
                status);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(500);
        StackPane centeredCard = new StackPane(card);
        centeredCard.setAlignment(Pos.CENTER);
        centeredCard.setMinHeight(560);
        VBox.setVgrow(centeredCard, Priority.ALWAYS);
        mainContainer.getChildren().add(centeredCard);
    }

    /** Construit l'écran de connexion et traite l'authentification LDAP. */
    private void showLoginScreen(Stage stage) {
        mainContainer.getChildren().clear();
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(32));

        Label brandIcon = new Label("P");
        brandIcon.getStyleClass().add("brand-mark");
        Label titleLabel = new Label("Bienvenue");
        titleLabel.getStyleClass().add("login-title");
        Label subtitleLabel = new Label("Connectez-vous à votre espace de gestion");
        subtitleLabel.getStyleClass().add("login-subtitle");

        TextField uidField = new TextField();
        uidField.setPromptText("Ex. : jdupont");
        uidField.getStyleClass().add("icon-input");
        uidField.setAccessibleText("Identifiant de connexion");
        HBox uidInput = iconInput(userIcon(), uidField, "Identifiant de connexion");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.getStyleClass().add("icon-input");
        passwordField.setAccessibleText("Mot de passe");
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Mot de passe");
        visiblePasswordField.getStyleClass().add("icon-input");
        visiblePasswordField.setAccessibleText("Mot de passe");
        // Les deux champs partagent le même texte : changer de mode ne perd pas la saisie.
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        Button visibilityButton = new Button("Afficher");
        visibilityButton.getStyleClass().add("password-visibility");
        visibilityButton.setGraphic(eyeIcon());
        visibilityButton.setAccessibleText("Afficher le mot de passe");
        // Les champs sont superposés ; un seul est visible et occupe l'espace à la fois.
        StackPane passwordControl = new StackPane(passwordField, visiblePasswordField);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        BorderPane passwordInput = new BorderPane();
        passwordInput.setLeft(lockIcon());
        passwordInput.setCenter(passwordControl);
        passwordInput.setRight(visibilityButton);
        passwordInput.getStyleClass().add("icon-input-row");
        passwordInput.setAccessibleText("Mot de passe");
        visiblePasswordField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (isFocused) {
                passwordInput.getStyleClass().add("focused");
            } else if (!passwordField.isFocused()) {
                passwordInput.getStyleClass().remove("focused");
            }
        });
        passwordField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (isFocused) {
                passwordInput.getStyleClass().add("focused");
            } else if (!visiblePasswordField.isFocused()) {
                passwordInput.getStyleClass().remove("focused");
            }
        });
        visibilityButton.setOnAction(event -> {
            boolean showPassword = !visiblePasswordField.isVisible();
            passwordField.setVisible(!showPassword);
            passwordField.setManaged(!showPassword);
            visiblePasswordField.setVisible(showPassword);
            visiblePasswordField.setManaged(showPassword);
            visibilityButton.setText(showPassword ? "Masquer" : "Afficher");
            visibilityButton.setAccessibleText(
                    showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe");
            TextField activeField = showPassword ? visiblePasswordField : passwordField;
            activeField.requestFocus();
            activeField.positionCaret(activeField.getText().length());
        });

        Label status = new Label();
        status.setWrapText(true);
        status.getStyleClass().add("login-status");

        Button loginButton = new Button("Se connecter");
        loginButton.setDefaultButton(true);
        loginButton.setGraphic(signInIcon());
        loginButton.getStyleClass().addAll("button", "button-primary", "login-button");
        loginButton.setOnAction(event -> {
            String uid = uidField.getText().trim();
            String password = passwordField.getText();
            if (uid.isEmpty() || password.isEmpty()) {
                status.setText("Veuillez renseigner l’identifiant et le mot de passe.");
                return;
            }

            loginButton.setDisable(true);
            status.setText("Connexion en cours…");
            try {
                // Après validation du mot de passe, le DN LDAP sert à déterminer les droits.
                LdapAuth.UserIdentity identity = LdapAuth.authenticate(uid, password);
                if (identity == null) {
                    status.setText("Identifiant introuvable dans l’annuaire.");
                    return;
                }
                RoleManager.Role role = RoleManager.getUserRole(identity.distinguishedName());
                currentUserUid = identity.uid();
                currentUserName = identity.displayName();
                currentRole = role;
                showMainInterface(stage);
            } catch (AuthenticationException e) {
                status.setText("Identifiant ou mot de passe incorrect.");
            } catch (NamingException | IllegalStateException e) {
                status.setText("Connexion LDAP impossible : " + usefulMessage(e));
            } finally {
                passwordField.clear();
                loginButton.setDisable(false);
            }
        });

        Label uidLabel = new Label("Identifiant");
        uidLabel.getStyleClass().add("field-label");
        Label passwordLabel = new Label("Mot de passe");
        passwordLabel.getStyleClass().add("field-label");

        VBox loginBox = new VBox(12,
                brandIcon, titleLabel, subtitleLabel,
                uidLabel, uidInput,
                passwordLabel, passwordInput,
                loginButton, status);
        loginBox.getStyleClass().add("login-card");
        loginBox.setMaxWidth(460);
        StackPane centeredCard = new StackPane(loginBox);
        centeredCard.setAlignment(Pos.CENTER);
        centeredCard.setMinHeight(560);
        VBox.setVgrow(centeredCard, Priority.ALWAYS);
        mainContainer.getChildren().add(centeredCard);
    }

    /** Affiche les publications et les commandes autorisées pour le rôle connecté. */
    private void showMainInterface(Stage stage) {
        mainContainer.getChildren().clear();
        mainContainer.setAlignment(Pos.TOP_LEFT);
        mainContainer.setPadding(new Insets(24));

        HBox header = new HBox(15);
        header.setPadding(new Insets(12));
        header.getStyleClass().add("app-header");
        Label userLabel = new Label("Utilisateur : " + currentUserName + " (" + currentUserUid + ")");
        userLabel.getStyleClass().add("header-user");
        Label roleLabel = new Label("Rôle : " + currentRole);
        roleLabel.getStyleClass().addAll("role-badge", "role-" + currentRole.name().toLowerCase());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logoutButton = new Button("Déconnexion");
        logoutButton.getStyleClass().addAll("button", "button-outline-light");
        logoutButton.setOnAction(event -> {
            // Efface la session en mémoire avant de revenir à l'écran de connexion.
            currentUserUid = null;
            currentUserName = null;
            currentRole = null;
            showLoginScreen(stage);
        });
        header.getChildren().addAll(userLabel, roleLabel, spacer, logoutButton);

        Label postsTitle = new Label("Publications");
        postsTitle.setGraphic(postsIcon());
        postsTitle.getStyleClass().add("section-title");
        postsArea = new TextArea();
        postsArea.setEditable(false);
        postsArea.setWrapText(true);
        postsArea.setPrefRowCount(10);
        postsArea.getStyleClass().add("posts-area");

        Button refreshButton = new Button("Actualiser");
        refreshButton.setGraphic(refreshIcon());
        refreshButton.getStyleClass().addAll("button", "button-secondary");
        refreshButton.setOnAction(event -> loadPosts());

        VBox createBox = new VBox(8);
        createBox.setPadding(new Insets(15));
        createBox.getStyleClass().add("content-card");
        Label createTitle = new Label("Créer une publication");
        createTitle.setGraphic(addIcon());
        createTitle.getStyleClass().add("card-title");
        titleField = new TextField();
        titleField.setPromptText("Titre");
        contentField = new TextArea();
        contentField.setPromptText("Contenu");
        contentField.setPrefRowCount(4);
        Button createButton = new Button("Publier");
        createButton.setGraphic(addIcon());
        createButton.getStyleClass().addAll("button", "button-primary");
        createButton.setOnAction(event -> createPost());
        createBox.getChildren().addAll(createTitle, titleField, contentField, createButton);

        mainContainer.getChildren().addAll(header, postsTitle, refreshButton, postsArea, createBox);
        // La modération est également contrôlée côté accès aux données, pas seulement masquée ici.
        if (currentRole == RoleManager.Role.ADMIN) {
            mainContainer.getChildren().add(createAdminBox());
        }
        loadPosts();
    }

    /** Crée les commandes de modération visibles uniquement par un administrateur. */
    private VBox createAdminBox() {
        VBox adminBox = new VBox(8);
        adminBox.setPadding(new Insets(15));
        adminBox.getStyleClass().add("admin-card");
        Label adminTitle = new Label("Administration");
        adminTitle.setGraphic(settingsIcon());
        adminTitle.getStyleClass().add("card-title");

        postIdField = new TextField();
        postIdField.setPromptText("ID de la publication");
        editTitleField = new TextField();
        editTitleField.setPromptText("Nouveau titre");
        editContentField = new TextArea();
        editContentField.setPromptText("Nouveau contenu");
        editContentField.setPrefRowCount(3);

        Button toggleButton = new Button("Masquer / démasquer");
        toggleButton.getStyleClass().addAll("button", "button-secondary");
        toggleButton.setOnAction(event -> togglePost());
        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().addAll("button", "button-danger");
        deleteButton.setOnAction(event -> deletePost());
        Button updateButton = new Button("Modifier");
        updateButton.getStyleClass().addAll("button", "button-accent");
        updateButton.setOnAction(event -> updatePost());

        HBox actions = new HBox(8, toggleButton, deleteButton, updateButton);
        adminBox.getChildren().addAll(
                adminTitle, postIdField, editTitleField, editContentField, actions);
        return adminBox;
    }

    /** Recharge les publications et applique le filtre correspondant au rôle courant. */
    private void loadPosts() {
        try {
            List<Post> posts = PostDAO.getPosts(currentRole == RoleManager.Role.ADMIN);
            if (posts.isEmpty()) {
                postsArea.setText("Aucune publication.");
                return;
            }

            StringBuilder text = new StringBuilder();
            for (Post post : posts) {
                text.append("────────────────────────────────────\n")
                        .append("ID : ").append(post.getId())
                        .append(" | Auteur : ").append(post.getAuthorName())
                        .append(" (").append(post.getAuthorUid()).append(")\n")
                        .append("Titre : ").append(post.getTitle()).append('\n')
                        .append(post.getContent()).append('\n')
                        .append("Date : ").append(post.getCreatedAt()).append('\n')
                        .append(post.isHidden() ? "Statut : masquée\n" : "Statut : visible\n");
            }
            postsArea.setText(text.toString());
        } catch (SQLException | IllegalStateException e) {
            postsArea.setText("Impossible de charger les publications : " + usefulMessage(e));
        }
    }

    /** Vérifie les champs puis enregistre une nouvelle publication pour l'utilisateur connecté. */
    private void createPost() {
        String title = titleField.getText().trim();
        String content = contentField.getText().trim();
        if (title.isEmpty() || content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Veuillez renseigner le titre et le contenu.");
            return;
        }

        try {
            PostDAO.createPost(title, content, currentUserUid, currentUserName);
            titleField.clear();
            contentField.clear();
            loadPosts();
            showAlert(Alert.AlertType.INFORMATION, "Publication créée.");
        } catch (SQLException | IllegalStateException e) {
            showAlert(Alert.AlertType.ERROR, "Échec de la création : " + usefulMessage(e));
        }
    }

    /** Masque ou rend visible la publication dont l'identifiant a été saisi. */
    private void togglePost() {
        Integer postId = readPostId();
        if (postId == null) {
            return;
        }
        try {
            if (!PostDAO.toggleHidden(postId, currentRole)) {
                showAlert(Alert.AlertType.WARNING, "Aucune publication ne correspond à cet ID.");
                return;
            }
            loadPosts();
        } catch (SQLException | IllegalStateException | SecurityException e) {
            showAlert(Alert.AlertType.ERROR, "Échec du changement de visibilité : " + usefulMessage(e));
        }
    }

    /** Demande confirmation avant la suppression définitive d'une publication. */
    private void deletePost() {
        Integer postId = readPostId();
        if (postId == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement la publication " + postId + " ?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("Confirmer la suppression");
        Optional<ButtonType> answer = confirmation.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }
        try {
            if (!PostDAO.deletePost(postId, currentRole)) {
                showAlert(Alert.AlertType.WARNING, "Aucune publication ne correspond à cet ID.");
                return;
            }
            loadPosts();
        } catch (SQLException | IllegalStateException | SecurityException e) {
            showAlert(Alert.AlertType.ERROR, "Échec de la suppression : " + usefulMessage(e));
        }
    }

    /** Modifie le titre et le contenu de la publication sélectionnée. */
    private void updatePost() {
        Integer postId = readPostId();
        if (postId == null) {
            return;
        }
        String title = editTitleField.getText().trim();
        String content = editContentField.getText().trim();
        if (title.isEmpty() || content.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Veuillez renseigner le nouveau titre et le nouveau contenu.");
            return;
        }

        try {
            if (!PostDAO.updatePost(postId, title, content, currentRole)) {
                showAlert(Alert.AlertType.WARNING, "Aucune publication ne correspond à cet ID.");
                return;
            }
            loadPosts();
        } catch (SQLException | IllegalStateException | SecurityException e) {
            showAlert(Alert.AlertType.ERROR, "Échec de la modification : " + usefulMessage(e));
        }
    }

    /** Lit un identifiant positif et signale une erreur si la saisie n'est pas valide. */
    private Integer readPostId() {
        String value = postIdField.getText().trim();
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Saisissez un ID de publication valide.");
            return null;
        }
    }

    /** Affiche un message d'information ou d'erreur dans une boîte de dialogue. */
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Assemble une icône et un champ dans un contrôle de saisie avec libellé accessible. */
    private static HBox iconInput(SVGPath icon, TextField field, String accessibleText) {
        icon.getStyleClass().add("field-icon");
        HBox input = new HBox(12, icon, field);
        input.setAlignment(Pos.CENTER_LEFT);
        input.getStyleClass().add("icon-input-row");
        field.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (isFocused) {
                input.getStyleClass().add("focused");
            } else {
                input.getStyleClass().remove("focused");
            }
        });
        HBox.setHgrow(field, Priority.ALWAYS);
        input.setAccessibleText(accessibleText);
        return input;
    }

    /** Crée un libellé de formulaire avec le style commun de l'écran de connexion. */
    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    /** Crée une icône vectorielle JavaFX, sans fichier image externe. */
    private static SVGPath icon(String path) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.getStyleClass().add("ui-icon");
        icon.setScaleX(0.78);
        icon.setScaleY(0.78);
        icon.setMouseTransparent(true);
        return icon;
    }

    /** Icône du compte utilisateur. */
    private static SVGPath userIcon() {
        return icon("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-4.42 0-8 2.24-8 5v1h16v-1c0-2.76-3.58-5-8-5z");
    }

    /** Icône du champ de mot de passe. */
    private static SVGPath lockIcon() {
        return icon("M18 8h-1V6a5 5 0 0 0-10 0v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zM9 6a3 3 0 0 1 6 0v2H9zm3 11a2 2 0 1 1 0-4 2 2 0 0 1 0 4z");
    }

    /** Icône du bouton d'affichage du mot de passe. */
    private static SVGPath eyeIcon() {
        return icon("M12 5C7 5 2.73 8.11 1 12c1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7zm0 11a4 4 0 1 1 0-8 4 4 0 0 1 0 8zm0-2a2 2 0 1 0 0-4 2 2 0 0 0 0 4z");
    }

    /** Icône de l'action de connexion. */
    private static SVGPath signInIcon() {
        return icon("M10 17l5-5-5-5v3H3v4h7zm9-14H5a2 2 0 0 0-2 2v3h2V5h14v14H5v-3H3v3a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2z");
    }

    /** Icône de la liste des publications. */
    private static SVGPath postsIcon() {
        return icon("M6 2h9l5 5v15H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm8 2v4h4M8 12h8v2H8zm0 4h8v2H8z");
    }

    /** Icône de l'action d'actualisation. */
    private static SVGPath refreshIcon() {
        return icon("M17.65 6.35A7.95 7.95 0 0 0 12 4a8 8 0 1 0 7.93 9h-2.02A6 6 0 1 1 12 6c1.66 0 3.14.69 4.22 1.78L13 11h7V4z");
    }

    /** Icône d'ajout d'une publication. */
    private static SVGPath addIcon() {
        return icon("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z");
    }

    /** Icône de la zone d'administration. */
    private static SVGPath settingsIcon() {
        return icon("M19.4 13a7.8 7.8 0 0 0 0-2l2-1.6-2-3.4-2.4 1a8 8 0 0 0-1.7-1L15 3h-4l-.4 3a8 8 0 0 0-1.7 1l-2.4-1-2 3.4 2 1.6a7.8 7.8 0 0 0 0 2l-2 1.6 2 3.4 2.4-1a8 8 0 0 0 1.7 1l.4 3h4l.4-3a8 8 0 0 0 1.7-1l2.4 1 2-3.4zM13 15a3 3 0 1 1 0-6 3 3 0 0 1 0 6z");
    }

    /** Fournit un texte d'erreur exploitable même si l'exception n'a pas de message. */
    private static String usefulMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    /** Permet le lancement de l'application par le plugin JavaFX de Maven. */
    public static void main(String[] args) {
        launch(args);
    }
}
