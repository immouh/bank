package com.example.bank.core.service.securite;

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
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration du registre de tentatives JDBC, sur une base H2 EN
 * MÉMOIRE — c'est cette implémentation-là qui tourne en production, pas
 * {@link RegistreTentativesEnMemoire}.
 *
 * CE QUI SE JOUE ICI, ET PAS AILLEURS — le SQL. L'arithmétique du délai
 * appartient à {@code Verrouillage} et {@code VerrouillageTest} la couvre
 * déjà ; ces tests vérifient que la table dit bien ce que cette
 * arithmétique calcule, et que le compteur SURVIT à l'écriture (chaque
 * lecture repasse par la base, jamais par un champ de l'objet).
 *
 * Une base neuve par test (nom aléatoire) : aucun test ne dépend de ce qu'un
 * autre a laissé derrière lui.
 */
@DisplayName("Registre de tentatives JDBC (H2)")
class JdbcRegistreTentativesTest {

    private static final int RIB = 123;
    private static final int RIB_AUTRE = 456;

    private Connection connexion;
    private Instant maintenant;
    private JdbcRegistreTentatives registre;

    @BeforeEach
    void ouvrirUneBaseNeuve() throws SQLException {
        connexion = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        BaseDeDonneesH2.creerSchema(connexion);
        // tentatives_connexion référence clients(rib) : sans ces lignes, le
        // moindre MERGE violerait la clé étrangère.
        creerClient(RIB, "Mouh");
        creerClient(RIB_AUTRE, "amine");

        maintenant = Instant.parse("2026-09-02T10:00:00Z");
        registre = new JdbcRegistreTentatives(connexion, new HorlogeReglable());
    }

    @AfterEach
    void fermer() throws SQLException {
        connexion.close();
    }

    /** Horloge qu'on avance à la main, pour franchir la fenêtre de verrouillage. */
    private class HorlogeReglable extends Clock {
        @Override public ZoneId getZone() { return ZoneId.systemDefault(); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return maintenant; }
    }

    private void creerClient(int rib, String nom) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement(
                "INSERT INTO clients (rib, nom, mot_de_passe) VALUES (?, ?, 'x')")) {
            requete.setInt(1, rib);
            requete.setString(2, nom);
            requete.executeUpdate();
        }
    }

    /** Nombre de lignes du compteur, lu directement en SQL. */
    private int lignesEnBase() throws SQLException {
        try (Statement statement = connexion.createStatement();
             ResultSet lignes = statement.executeQuery(
                     "SELECT COUNT(*) FROM tentatives_connexion")) {
            lignes.next();
            return lignes.getInt(1);
        }
    }

    /** Échecs lus directement en SQL, sans passer par le registre. */
    private Integer echecsEnBase(int rib) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement(
                "SELECT echecs FROM tentatives_connexion WHERE client_rib = ?")) {
            requete.setInt(1, rib);
            try (ResultSet lignes = requete.executeQuery()) {
                return lignes.next() ? lignes.getInt("echecs") : null;
            }
        }
    }

    /** Horodatage lu directement en SQL, sans passer par le registre. */
    private LocalDateTime derniereTentativeEnBase(int rib) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement(
                "SELECT derniere_tentative FROM tentatives_connexion WHERE client_rib = ?")) {
            requete.setInt(1, rib);
            try (ResultSet lignes = requete.executeQuery()) {
                return lignes.next() ? lignes.getTimestamp(1).toLocalDateTime() : null;
            }
        }
    }

    private void echouer(int fois) {
        for (int i = 0; i < fois; i++) {
            registre.enregistrerEchec(RIB);
        }
    }

    @Nested
    @DisplayName("Comptage persisté")
    class Comptage {

        @Test
        @DisplayName("Un compte sans ligne en base n'a aucun échec au compteur")
        void compteurNeuf() throws SQLException {
            assertEquals(0, registre.echecs(RIB));
            assertFalse(registre.estVerrouille(RIB));
            assertEquals(0, lignesEnBase());
        }

        @Test
        @DisplayName("Un échec enregistré est relu depuis la base, pas depuis l'objet")
        void echecReluDepuisLaBase() throws SQLException {
            registre.enregistrerEchec(RIB);

            assertEquals(1, echecsEnBase(RIB));
            // Un registre tout neuf sur la même connexion voit la même chose :
            // rien n'est retenu en mémoire par l'instance qui a écrit.
            assertEquals(1, new JdbcRegistreTentatives(connexion).echecs(RIB));
        }

        @Test
        @DisplayName("Chaque échec incrémente le compteur d'une unité")
        void echecsSuccessifsSAdditionnent() throws SQLException {
            echouer(3);

            assertEquals(3, registre.echecs(RIB));
            assertEquals(3, echecsEnBase(RIB));
        }

        @Test
        @DisplayName("Les compteurs de deux clients sont indépendants")
        void compteursIndependants() throws SQLException {
            echouer(2);
            registre.enregistrerEchec(RIB_AUTRE);

            assertEquals(2, registre.echecs(RIB));
            assertEquals(1, registre.echecs(RIB_AUTRE));
            assertEquals(2, lignesEnBase());
        }

        @Test
        @DisplayName("L'horodatage écrit est celui de l'horloge injectée")
        void horodatageDeLHorlogeInjectee() throws SQLException {
            registre.enregistrerEchec(RIB);

            assertEquals(LocalDateTime.now(new HorlogeReglable()),
                    derniereTentativeEnBase(RIB));
        }
    }

    /**
     * Le MERGE ... KEY (client_rib) est le point délicat de cette classe : la
     * même requête doit INSÉRER au premier échec et METTRE À JOUR aux
     * suivants. Une clé mal choisie produirait soit une violation de clé
     * primaire, soit une seconde ligne pour le même client.
     */
    @Nested
    @DisplayName("MERGE : insertion puis mise à jour")
    class Fusion {

        @Test
        @DisplayName("Le premier échec insère une ligne")
        void premierEchecInsere() throws SQLException {
            registre.enregistrerEchec(RIB);

            assertEquals(1, lignesEnBase());
            assertEquals(1, echecsEnBase(RIB));
        }

        @Test
        @DisplayName("Un échec suivant met à jour la ligne existante sans en créer une seconde")
        void echecSuivantMetAJour() throws SQLException {
            registre.enregistrerEchec(RIB);
            registre.enregistrerEchec(RIB);

            assertEquals(1, lignesEnBase(), "Le MERGE aurait dû mettre à jour, pas insérer.");
            assertEquals(2, echecsEnBase(RIB));
        }

        @Test
        @DisplayName("La mise à jour rafraîchit aussi l'horodatage")
        void miseAJourRafraichitLHorodatage() throws SQLException {
            registre.enregistrerEchec(RIB);
            LocalDateTime premier = derniereTentativeEnBase(RIB);

            maintenant = maintenant.plus(Duration.ofMinutes(5));
            registre.enregistrerEchec(RIB);

            assertEquals(premier.plusMinutes(5), derniereTentativeEnBase(RIB));
        }

        @Test
        @DisplayName("Après une remise à zéro, le compteur repart d'un insert à 1")
        void repartDeUnApresReinitialisation() throws SQLException {
            echouer(2);
            registre.reinitialiser(RIB);

            registre.enregistrerEchec(RIB);

            assertEquals(1, echecsEnBase(RIB));
            assertEquals(1, lignesEnBase());
        }
    }

    /**
     * Le seuil et le délai appartiennent à {@code Verrouillage} et à
     * {@code RegistreTentatives} : on ne les recalcule pas ici, on vérifie que
     * la version SQL applique exactement ce qu'ils disent.
     */
    @Nested
    @DisplayName("Verrouillage au seuil")
    class Seuil {

        @Test
        @DisplayName("En dessous du seuil, le compte reste ouvert")
        void sousLeSeuilPasDeVerrouillage() {
            echouer(RegistreTentatives.ECHECS_AVANT_VERROUILLAGE - 1);

            assertFalse(registre.estVerrouille(RIB));
            assertEquals(0, registre.minutesRestantes(RIB));
        }

        @Test
        @DisplayName("Au seuil exact, le compte est verrouillé")
        void auSeuilVerrouillage() {
            echouer(RegistreTentatives.ECHECS_AVANT_VERROUILLAGE);

            assertTrue(registre.estVerrouille(RIB));
        }

        @Test
        @DisplayName("Le verrouillage annonce la durée complète juste après le dernier échec")
        void minutesRestantesAuVerrouillage() {
            echouer(RegistreTentatives.ECHECS_AVANT_VERROUILLAGE);

            assertEquals(RegistreTentatives.MINUTES_DE_VERROUILLAGE,
                    registre.minutesRestantes(RIB));
        }

        @Test
        @DisplayName("Le verrouillage d'un client n'en verrouille pas un autre")
        void verrouillageIsole() {
            echouer(RegistreTentatives.ECHECS_AVANT_VERROUILLAGE);

            assertFalse(registre.estVerrouille(RIB_AUTRE));
        }

        @Test
        @DisplayName("Le verrouillage survit à une nouvelle instance : il est en base, pas en mémoire")
        void verrouillageSurvitAUneNouvelleInstance() {
            echouer(RegistreTentatives.ECHECS_AVANT_VERROUILLAGE);

            assertTrue(new JdbcRegistreTentatives(connexion, new HorlogeReglable())
                    .estVerrouille(RIB));
        }
    }

    @Nested
    @DisplayName("Remise à zéro")
    class RemiseAZero {

        @Test
        @DisplayName("Une remise à zéro supprime la ligne du client")
        void reinitialiserSupprimeLaLigne() throws SQLException {
            echouer(2);

            registre.reinitialiser(RIB);

            assertEquals(0, registre.echecs(RIB));
            assertNull(echecsEnBase(RIB));
        }

        @Test
        @DisplayName("Une remise à zéro déverrouille le compte")
        void reinitialiserDeverrouille() {
            echouer(RegistreTentatives.ECHECS_AVANT_VERROUILLAGE);

            registre.reinitialiser(RIB);

            assertFalse(registre.estVerrouille(RIB));
        }

        @Test
        @DisplayName("Remettre à zéro un compte sans ligne ne lève rien")
        void reinitialiserSansLigne() throws SQLException {
            registre.reinitialiser(RIB);

            assertEquals(0, lignesEnBase());
        }

        @Test
        @DisplayName("Une remise à zéro ne touche pas le compteur des autres clients")
        void reinitialiserNeToucheQueLeClientVise() {
            echouer(2);
            registre.enregistrerEchec(RIB_AUTRE);

            registre.reinitialiser(RIB);

            assertEquals(1, registre.echecs(RIB_AUTRE));
        }
    }

    @Nested
    @DisplayName("Expiration du verrouillage")
    class Expiration {

        @BeforeEach
        void verrouiller() {
            echouer(RegistreTentatives.ECHECS_AVANT_VERROUILLAGE);
        }

        @Test
        @DisplayName("Le verrouillage tient encore une minute avant l'échéance")
        void tientJusteAvantLEcheance() {
            maintenant = maintenant.plus(
                    Duration.ofMinutes(RegistreTentatives.MINUTES_DE_VERROUILLAGE - 1));

            assertTrue(registre.estVerrouille(RIB));
            assertEquals(1, registre.minutesRestantes(RIB));
        }

        @Test
        @DisplayName("Le verrouillage tombe de lui-même à l'échéance")
        void tombeALEcheance() {
            maintenant = maintenant.plus(
                    Duration.ofMinutes(RegistreTentatives.MINUTES_DE_VERROUILLAGE));

            assertFalse(registre.estVerrouille(RIB));
            assertEquals(0, registre.minutesRestantes(RIB));
        }

        @Test
        @DisplayName("L'expiration efface la ligne en base, elle ne fait pas que la taire")
        void expirationEffaceLaLigne() throws SQLException {
            maintenant = maintenant.plus(
                    Duration.ofMinutes(RegistreTentatives.MINUTES_DE_VERROUILLAGE + 1));

            assertFalse(registre.estVerrouille(RIB));

            assertEquals(0, lignesEnBase());
            assertEquals(0, registre.echecs(RIB));
        }

        @Test
        @DisplayName("Un échec après expiration repart du premier, pas du quatrième")
        void echecApresExpirationRepartDeUn() throws SQLException {
            maintenant = maintenant.plus(
                    Duration.ofMinutes(RegistreTentatives.MINUTES_DE_VERROUILLAGE + 1));
            registre.estVerrouille(RIB);

            registre.enregistrerEchec(RIB);

            assertEquals(1, echecsEnBase(RIB));
        }
    }

    /**
     * La classe ne laisse pas remonter de {@code SQLException} : tout échec
     * technique devient une {@code PersistanceException}, comme partout
     * ailleurs dans la couche de persistance.
     */
    @Nested
    @DisplayName("Contrat technique")
    class Technique {

        @Test
        @DisplayName("Une connexion nulle est refusée à la construction")
        void connexionObligatoire() {
            assertThrows(NullPointerException.class, () -> new JdbcRegistreTentatives(null));
        }
    }
}
