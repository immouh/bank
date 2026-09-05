package com.example.bank.core.service.securite;

import com.example.bank.core.exception.technique.PersistanceException;
import com.example.bank.core.repository.BaseDeDonneesH2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration du répertoire de mots de passe JDBC, sur une base H2 EN
 * MÉMOIRE — c'est cette implémentation-là qui tourne en production, pas
 * {@link RepertoireMotsDePasseEnMemoire}.
 *
 * CE QUI EST VÉRIFIÉ — la fidélité de l'aller-retour sur
 * {@code clients.mot_de_passe} (un haché BCrypt fait 60 caractères et contient
 * {@code $}, {@code /} et {@code .} : il ne doit ni être tronqué ni être
 * réinterprété), et le comportement sur un RIB absent. Le hachage lui-même est
 * l'affaire de {@code BCryptHachageStrategyTest}.
 *
 * Une base neuve par test (nom aléatoire) : aucun test ne dépend de ce qu'un
 * autre a laissé derrière lui.
 */
@DisplayName("Répertoire de mots de passe JDBC (H2)")
class JdbcRepertoireMotsDePasseTest {

    private static final int RIB = 123;
    private static final int RIB_AUTRE = 456;
    private static final int RIB_INCONNU = 999;

    private Connection connexion;
    private JdbcRepertoireMotsDePasse repertoire;

    @BeforeEach
    void ouvrirUneBaseNeuve() throws SQLException {
        connexion = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        BaseDeDonneesH2.creerSchema(connexion);
        creerClient(RIB, "Mouh");
        creerClient(RIB_AUTRE, "amine");
        repertoire = new JdbcRepertoireMotsDePasse(connexion);
    }

    @AfterEach
    void fermer() throws SQLException {
        connexion.close();
    }

    private void creerClient(int rib, String nom) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement(
                "INSERT INTO clients (rib, nom, mot_de_passe) VALUES (?, ?, 'a-remplacer')")) {
            requete.setInt(1, rib);
            requete.setString(2, nom);
            requete.executeUpdate();
        }
    }

    /** Colonne lue directement en SQL, sans passer par le répertoire. */
    private String colonneEnBase(int rib) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement(
                "SELECT mot_de_passe FROM clients WHERE rib = ?")) {
            requete.setInt(1, rib);
            try (ResultSet lignes = requete.executeQuery()) {
                return lignes.next() ? lignes.getString(1) : null;
            }
        }
    }

    @Nested
    @DisplayName("Aller-retour du haché")
    class AllerRetour {

        @Test
        @DisplayName("Un haché enregistré est relu à l'identique")
        void hacheReluFidelement() {
            repertoire.enregistrer(RIB, "$2a$10$abcdefghijklmnopqrstuv");

            assertEquals(Optional.of("$2a$10$abcdefghijklmnopqrstuv"), repertoire.hachePour(RIB));
        }

        /**
         * Le format BCrypt réel : 60 caractères, avec {@code $}, {@code /} et
         * {@code .}. C'est ce qui sera vraiment stocké en production.
         */
        @Test
        @DisplayName("Un haché BCrypt complet traverse la base sans être tronqué ni altéré")
        void hacheBcryptComplet() {
            String hache = new BCryptHachageStrategy(4).hacher("tata");

            repertoire.enregistrer(RIB, hache);

            String relu = repertoire.hachePour(RIB).orElseThrow();
            assertEquals(hache, relu);
            assertEquals(hache.length(), relu.length());
            assertTrue(new BCryptHachageStrategy(4).verifier("tata", relu),
                    "Le haché relu doit encore vérifier le mot de passe d'origine.");
        }

        @Test
        @DisplayName("Le haché est relu depuis la base, pas depuis l'objet qui l'a écrit")
        void hacheReluDepuisLaBase() throws SQLException {
            repertoire.enregistrer(RIB, "haché-1");

            assertEquals("haché-1", colonneEnBase(RIB));
            assertEquals(Optional.of("haché-1"),
                    new JdbcRepertoireMotsDePasse(connexion).hachePour(RIB));
        }

        @Test
        @DisplayName("Les hachés de deux clients ne se mélangent pas")
        void hachesIndependants() {
            repertoire.enregistrer(RIB, "haché-mouh");
            repertoire.enregistrer(RIB_AUTRE, "haché-amine");

            assertEquals(Optional.of("haché-mouh"), repertoire.hachePour(RIB));
            assertEquals(Optional.of("haché-amine"), repertoire.hachePour(RIB_AUTRE));
        }
    }

    @Nested
    @DisplayName("Changement de mot de passe")
    class MiseAJour {

        @Test
        @DisplayName("Enregistrer un second haché remplace le premier")
        void secondHacheRemplaceLePremier() {
            repertoire.enregistrer(RIB, "ancien");

            repertoire.enregistrer(RIB, "nouveau");

            assertEquals(Optional.of("nouveau"), repertoire.hachePour(RIB));
        }

        @Test
        @DisplayName("Après changement, l'ancien mot de passe ne vérifie plus le haché stocké")
        void ancienMotDePasseNeVerifiePlus() {
            BCryptHachageStrategy hachage = new BCryptHachageStrategy(4);
            repertoire.enregistrer(RIB, hachage.hacher("tata"));

            repertoire.enregistrer(RIB, hachage.hacher("nouveau-secret"));

            String relu = repertoire.hachePour(RIB).orElseThrow();
            assertTrue(hachage.verifier("nouveau-secret", relu));
            assertFalse(hachage.verifier("tata", relu));
        }

        @Test
        @DisplayName("Un changement ne touche pas le haché des autres clients")
        void changementIsole() {
            repertoire.enregistrer(RIB, "haché-mouh");
            repertoire.enregistrer(RIB_AUTRE, "haché-amine");

            repertoire.enregistrer(RIB, "haché-mouh-2");

            assertEquals(Optional.of("haché-amine"), repertoire.hachePour(RIB_AUTRE));
        }

        @Test
        @DisplayName("Le répertoire écrase la valeur posée à la création du client")
        void ecraseLaValeurInitiale() throws SQLException {
            assertEquals("a-remplacer", colonneEnBase(RIB));

            repertoire.enregistrer(RIB, "haché");

            assertNotEquals("a-remplacer", colonneEnBase(RIB));
        }
    }

    @Nested
    @DisplayName("Client absent de la base")
    class ClientAbsent {

        @Test
        @DisplayName("Un RIB inconnu ne rend rien plutôt que null")
        void ribInconnuRendVide() {
            assertTrue(repertoire.hachePour(RIB_INCONNU).isEmpty());
        }

        /**
         * Écrire sur un client inexistant ne doit pas passer INAPERÇU :
         * l'{@code UPDATE} ne toucherait aucune ligne et le mot de passe
         * serait perdu en silence — le client suivant se verrait refuser sa
         * connexion sans que rien n'ait signalé le problème.
         */
        @Test
        @DisplayName("Enregistrer pour un RIB inconnu est refusé explicitement")
        void enregistrementSurRibInconnuRefuse() {
            PersistanceException levee = assertThrows(PersistanceException.class,
                    () -> repertoire.enregistrer(RIB_INCONNU, "haché"));

            assertTrue(levee.getMessage().contains(String.valueOf(RIB_INCONNU)),
                    levee.getMessage());
        }

        @Test
        @DisplayName("Un client existant qui n'est jamais passé par le répertoire rend quand même sa colonne")
        void clientJamaisEnregistreRendLaColonne() {
            // La colonne est NOT NULL : un client en base a toujours une
            // valeur, celle posée à sa création. C'est ce qui distingue
            // « client inconnu » (vide) de « client connu » (une valeur).
            assertEquals(Optional.of("a-remplacer"), repertoire.hachePour(RIB));
        }
    }

    @Nested
    @DisplayName("Contrat technique")
    class Technique {

        @Test
        @DisplayName("Une connexion nulle est refusée à la construction")
        void connexionObligatoire() {
            assertThrows(NullPointerException.class, () -> new JdbcRepertoireMotsDePasse(null));
        }

        @Test
        @DisplayName("Une connexion fermée remonte une PersistanceException, pas une SQLException")
        void connexionFermeeRemontePersistance() throws SQLException {
            connexion.close();

            assertThrows(PersistanceException.class, () -> repertoire.hachePour(RIB));
            assertThrows(PersistanceException.class, () -> repertoire.enregistrer(RIB, "h"));
        }
    }
}
