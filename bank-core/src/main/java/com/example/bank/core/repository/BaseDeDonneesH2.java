package com.example.bank.core.repository;

import com.example.bank.core.exception.technique.PersistanceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ouverture de la base et création du schéma.
 *
 * Volontairement minuscule : ce n'est ni un pool de connexions ni un
 * framework de migration. Le jour où le projet en aura besoin (phase G,
 * Spring), cette classe disparaîtra au profit d'une DataSource — d'ici là,
 * une connexion unique suffit à une application de bureau mono-utilisateur.
 *
 * Le driver H2 n'est PAS une dépendance de compilation du coeur : cette
 * classe ne connaît que {@code java.sql}. C'est l'assemblage (bank-swing)
 * qui fournit le driver au runtime.
 */
public final class BaseDeDonneesH2 {

    /** Emplacement par défaut du fichier de base de l'application. */
    public static final String URL_FICHIER = "jdbc:h2:./data/bank";

    /** Base jetable, vivante le temps du processus — pour les tests. */
    public static final String URL_MEMOIRE = "jdbc:h2:mem:bank;DB_CLOSE_DELAY=-1";

    private static final String SCHEMA = "/schema.sql";

    private BaseDeDonneesH2() {
    }

    /**
     * Ouvre une connexion et crée le schéma s'il n'existe pas encore.
     * L'appelant est responsable de la fermeture.
     */
    public static Connection ouvrir(String url) {
        try {
            Connection connexion = DriverManager.getConnection(url, "sa", "");
            creerSchema(connexion);
            return connexion;
        } catch (SQLException e) {
            throw new PersistanceException("Base de données injoignable.", e);
        }
    }

    /**
     * Rejoue {@code schema.sql}. Le script n'est fait que de
     * {@code CREATE ... IF NOT EXISTS} : le relancer sur une base déjà
     * peuplée ne détruit rien.
     */
    public static void creerSchema(Connection connexion) {
        try (Statement statement = connexion.createStatement()) {
            statement.execute(lireScript());
        } catch (SQLException e) {
            throw new PersistanceException("Création du schéma impossible.", e);
        }
    }

    private static String lireScript() {
        try (InputStream flux = BaseDeDonneesH2.class.getResourceAsStream(SCHEMA)) {
            if (flux == null) {
                throw new PersistanceException("Script de schéma introuvable : " + SCHEMA);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistanceException("Script de schéma illisible : " + SCHEMA, e);
        }
    }
}
