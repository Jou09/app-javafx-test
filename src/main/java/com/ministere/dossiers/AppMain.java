package com.ministere.dossiers;

import javafx.application.Application;

/** Point d'entrée Java standard utilisé par le JAR exécutable. */
public final class AppMain {
    /** Cette classe sert uniquement à démarrer JavaFX et ne doit pas être instanciée. */
    private AppMain() {
    }

    /** Démarre la fenêtre principale de l'application JavaFX. */
    public static void main(String[] args) {
        Application.launch(Launcher.class, args);
    }
}
