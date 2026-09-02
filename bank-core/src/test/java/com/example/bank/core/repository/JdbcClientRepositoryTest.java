package com.example.bank.core.repository;

import com.example.bank.core.exception.technique.PersistanceException;
import com.example.bank.core.model.Client;
import com.example.bank.core.model.Transaction;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.etat.concret.CompteActif;
import com.example.bank.core.model.offre.compte.etat.concret.CompteBloque;
import com.example.bank.core.model.offre.compte.etat.concret.CompteEnDecouvert;
import com.example.bank.core.model.offre.compte.etat.concret.CompteFerme;
import com.example.bank.core.model.offre.concret.OffreEtudianteFactory;
import com.example.bank.core.model.offre.concret.OffrePremiumFactory;
import com.example.bank.core.model.offre.concret.OffreStandardFactory;
import com.example.bank.core.service.BanqueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration du repository JDBC, sur une base H2 EN MÉMOIRE.
 *
 * Une base neuve par test (nom aléatoire) : aucun test ne dépend de ce qu'un
 * autre a laissé derrière lui, et rien n'est écrit sur le disque.
 *
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals} :
 * la base rend un DECIMAL(15,2), donc « 500.00 », là où le code peut avoir
 * construit « 500 ».
 */
@DisplayName("Repository JDBC (H2)")
class JdbcClientRepositoryTest {

    private Connection connexion;
    private JdbcClientRepository repository;

    @BeforeEach
    void ouvrirUneBaseNeuve() throws SQLException {
        connexion = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        BaseDeDonneesH2.creerSchema(connexion);
        repository = new JdbcClientRepository(connexion);
    }

    @AfterEach
    void fermer() throws SQLException {
        connexion.close();
    }

    /** Solde lu directement en SQL, sans passer par le repository. */
    private BigDecimal soldeEnBase(int rib, String type) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement(
                "SELECT solde FROM comptes WHERE client_rib = ? AND type = ?")) {
            requete.setInt(1, rib);
            requete.setString(2, type);
            try (ResultSet lignes = requete.executeQuery()) {
                return lignes.next() ? lignes.getBigDecimal("solde") : null;
            }
        }
    }

    private Client creerEtSauver(String nom, String mdp, int rib) {
        Client client = new Client(nom, mdp, rib);
        repository.creer(client, mdp);
        return client;
    }

    @Nested
    @DisplayName("Aller-retour en base")
    class AllerRetour {

        @Test
        @DisplayName("Un client créé est retrouvé par son RIB")
        void clientRetrouveParRib() {
            creerEtSauver("Mouh", "tata", 123);

            assertTrue(repository.findByRib(123).isPresent());
            assertEquals("Mouh", repository.findByRib(123).orElseThrow().getNom());
        }

        @Test
        @DisplayName("Un RIB inconnu ne rend rien plutôt que null")
        void ribInconnuRendVide() {
            assertTrue(repository.findByRib(999).isEmpty());
        }

        @Test
        @DisplayName("Un client est retrouvé par son nom")
        void clientRetrouveParNom() {
            creerEtSauver("Mouh", "tata", 123);

            assertEquals(123, repository.findByNom("Mouh").orElseThrow().getRib());
        }

        @Test
        @DisplayName("Le mot de passe stocké permet de se réauthentifier après relecture")
        void motDePasseSurvitALaRelecture() {
            creerEtSauver("Mouh", "tata", 123);

            Client relu = repository.findByRib(123).orElseThrow();
            assertTrue(relu.verifierMotDePasse("tata"));
            assertFalse(relu.verifierMotDePasse("mauvais"));
        }

        @Test
        @DisplayName("existsByRib répond juste avant et après création")
        void existsByRib() {
            assertFalse(repository.existsByRib(123));

            creerEtSauver("Mouh", "tata", 123);

            assertTrue(repository.existsByRib(123));
        }

        @Test
        @DisplayName("findAll rend tous les clients, triés par RIB")
        void findAllRendTousLesClients() {
            creerEtSauver("amine", "matoub", 456);
            creerEtSauver("Mouh", "tata", 123);

            List<Client> clients = repository.findAll();

            assertEquals(2, clients.size());
            assertEquals(123, clients.get(0).getRib());
            assertEquals(456, clients.get(1).getRib());
        }

        @Test
        @DisplayName("Le solde du compte courant survit à l'aller-retour")
        void soldeSurvitALAllerRetour() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.crediter(new BigDecimal("1234.56"));
            repository.save(client);

            assertEquals(0, new BigDecimal("1234.56")
                    .compareTo(repository.findByRib(123).orElseThrow().getSoldeCompte()));
        }

        @Test
        @DisplayName("Les centimes traversent la base sans dérive : DECIMAL, pas de flottant")
        void pasDeDeriveDeCentimes() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            for (int i = 0; i < 10; i++) {
                client.crediter(new BigDecimal("0.10"));
            }
            repository.save(client);

            assertEquals(0, new BigDecimal("1.00")
                    .compareTo(repository.findByRib(123).orElseThrow().getSoldeCompte()));
        }

        @Test
        @DisplayName("Sauvegarder deux fois le même client ne duplique pas ses comptes")
        void sauvegardesRepeteesNeDupliquentPas() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.crediter(new BigDecimal("100.00"));
            repository.save(client);
            repository.save(client);

            assertEquals(1, repository.findByRib(123).orElseThrow().getComptes().size());
        }

        @Test
        @DisplayName("Sauvegarder un client absent de la base est refusé explicitement")
        void sauvegardeDUnClientInconnuRefusee() {
            Client jamaisCree = new Client("Fantome", "x", 777);

            PersistanceException levee = assertThrows(PersistanceException.class,
                    () -> repository.save(jamaisCree));
            assertTrue(levee.getMessage().contains("creer"), levee.getMessage());
        }
    }

    @Nested
    @DisplayName("Fidélité de l'offre et des comptes")
    class FideliteDesComptes {

        @Test
        @DisplayName("TIER Étudiante — le découvert nul est retrouvé après relecture")
        void offreEtudianteRelue() {
            Client etudiant = new Client("E", "x", 1, new OffreEtudianteFactory());
            repository.creer(etudiant, "x");

            Client relu = repository.findByRib(1).orElseThrow();

            assertEquals(0, new BigDecimal("0.00")
                    .compareTo(relu.getCompteCourant().getDecouvertAutorise()));
        }

        @Test
        @DisplayName("TIER Premium — le découvert de 2000 € est retrouvé après relecture")
        void offrePremiumRelue() {
            Client premium = new Client("P", "x", 3, new OffrePremiumFactory());
            repository.creer(premium, "x");

            Client relu = repository.findByRib(3).orElseThrow();

            assertEquals(0, new BigDecimal("2000.00")
                    .compareTo(relu.getCompteCourant().getDecouvertAutorise()));
        }

        @Test
        @DisplayName("Un client relu peut toujours descendre jusqu'au découvert de son offre")
        void decouvertUtilisableApresRelecture() {
            Client premium = new Client("P", "x", 3, new OffrePremiumFactory());
            repository.creer(premium, "x");

            Client relu = repository.findByRib(3).orElseThrow();
            relu.debiter(new BigDecimal("2000.00"));

            assertEquals(0, new BigDecimal("-2000.00").compareTo(relu.getSoldeCompte()));
        }

        @Test
        @DisplayName("Le Livret A survit à l'aller-retour, avec son solde")
        void livretASurvit() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.crediter(new BigDecimal("1000.00"));
            client.ouvrirLivretA();
            client.crediterLivretA(new BigDecimal("400.00"));
            repository.save(client);

            Client relu = repository.findByRib(123).orElseThrow();

            assertEquals(2, relu.getComptes().size());
            assertTrue(relu.getLivretA().isPresent());
            assertEquals(0, new BigDecimal("400.00").compareTo(relu.getSoldeLivretA()));
            assertEquals(0, new BigDecimal("1000.00").compareTo(relu.getSoldeCompte()));
        }

        @Test
        @DisplayName("Le Livret A relu est rémunéré au taux de l'offre du client")
        void livretARelueAuBonTaux() {
            Client etudiant = new Client("E", "x", 1, new OffreEtudianteFactory());
            repository.creer(etudiant, "x");
            etudiant.ouvrirLivretA();
            repository.save(etudiant);

            var livret = (com.example.bank.core.model.offre.compte.concret.LivretA)
                    repository.findByRib(1).orElseThrow().getLivretA().orElseThrow();

            assertEquals(0, new BigDecimal("0.0200").compareTo(livret.getTauxInteret()));
        }

        @Test
        @DisplayName("Le solde total est identique avant et après relecture")
        void soldeTotalIdentique() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.crediter(new BigDecimal("1000.00"));
            client.ouvrirLivretA();
            client.crediterLivretA(new BigDecimal("250.00"));
            repository.save(client);

            assertEquals(0, client.soldeTotal()
                    .compareTo(repository.findByRib(123).orElseThrow().soldeTotal()));
        }
    }

    @Nested
    @DisplayName("Fidélité de l'état des comptes")
    class FideliteDesEtats {

        @Test
        @DisplayName("Un compte au solde positif revient à l'état actif")
        void compteActifRelu() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.crediter(new BigDecimal("100.00"));
            repository.save(client);

            assertInstanceOf(CompteActif.class,
                    repository.findByRib(123).orElseThrow().getCompteCourant().getEtat());
        }

        @Test
        @DisplayName("Un compte en découvert revient EN DÉCOUVERT, pas seulement avec un solde négatif")
        void compteEnDecouvertRelu() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.debiter(new BigDecimal("150.00"));
            repository.save(client);

            Client relu = repository.findByRib(123).orElseThrow();

            assertEquals(0, new BigDecimal("-150.00").compareTo(relu.getSoldeCompte()));
            assertInstanceOf(CompteEnDecouvert.class, relu.getCompteCourant().getEtat());
        }

        @Test
        @DisplayName("Un compte bloqué revient bloqué et refuse toujours les opérations")
        void compteBloqueRelu() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.crediter(new BigDecimal("500.00"));
            client.getCompteCourant().bloquer();
            repository.save(client);

            Compte relu = repository.findByRib(123).orElseThrow().getCompteCourant();

            assertInstanceOf(CompteBloque.class, relu.getEtat());
            assertFalse(relu.getEtat().autoriseOperations());
            assertEquals(0, new BigDecimal("500.00").compareTo(relu.getSolde()));
        }

        @Test
        @DisplayName("Un compte fermé revient fermé, solde conservé")
        void compteFermeRelu() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.crediter(new BigDecimal("42.00"));
            client.getCompteCourant().fermer();
            repository.save(client);

            Compte relu = repository.findByRib(123).orElseThrow().getCompteCourant();

            assertInstanceOf(CompteFerme.class, relu.getEtat());
            assertEquals(0, new BigDecimal("42.00").compareTo(relu.getSolde()));
        }

        @Test
        @DisplayName("Un compte bloqué ET en découvert revient dans les deux états à la fois")
        void compteBloqueEnDecouvertRelu() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.debiter(new BigDecimal("200.00"));
            client.getCompteCourant().bloquer();
            repository.save(client);

            Compte relu = repository.findByRib(123).orElseThrow().getCompteCourant();

            assertInstanceOf(CompteBloque.class, relu.getEtat());
            assertEquals(0, new BigDecimal("-200.00").compareTo(relu.getSolde()));
        }
    }

    @Nested
    @DisplayName("Historique stocké")
    class Historique {

        @Test
        @DisplayName("Les transactions écrites sont relues dans leur ordre d'exécution")
        void historiqueRelu() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.ajouterTransaction(new Transaction(new BigDecimal("100.00"),
                    Transaction.TypeTransaction.DEPOT, "premier"));
            client.ajouterTransaction(new Transaction(new BigDecimal("40.00"),
                    Transaction.TypeTransaction.RETRAIT, "second"));

            repository.enregistrerHistorique(client);

            List<LigneHistorique> lignes = repository.historique(123);
            assertEquals(2, lignes.size());
            assertEquals("premier", lignes.get(0).description());
            assertEquals(Transaction.TypeTransaction.RETRAIT, lignes.get(1).type());
            assertEquals(0, new BigDecimal("40.00").compareTo(lignes.get(1).montant()));
        }

        /**
         * C'est la raison d'être de {@link LigneHistorique} : l'horodatage
         * relu est celui de l'opération, pas celui de la relecture.
         */
        @Test
        @DisplayName("L'horodatage relu est bien celui de l'opération d'origine")
        void horodatageFidele() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            LocalDateTime avant = LocalDateTime.now();
            client.ajouterTransaction(new Transaction(new BigDecimal("100.00"),
                    Transaction.TypeTransaction.DEPOT, "dépôt"));
            LocalDateTime apres = LocalDateTime.now();

            repository.enregistrerHistorique(client);

            LocalDateTime relu = repository.historique(123).get(0).horodatage();
            assertFalse(relu.isBefore(avant), "Horodatage antérieur à l'opération : " + relu);
            assertFalse(relu.isAfter(apres), "Horodatage postérieur à l'opération : " + relu);
        }

        @Test
        @DisplayName("Réenregistrer l'historique ne duplique pas les lignes")
        void reenregistrementNeDupliquePas() {
            Client client = creerEtSauver("Mouh", "tata", 123);
            client.ajouterTransaction(new Transaction(new BigDecimal("100.00"),
                    Transaction.TypeTransaction.DEPOT, "dépôt"));

            repository.enregistrerHistorique(client);
            repository.enregistrerHistorique(client);

            assertEquals(1, repository.historique(123).size());
        }

        @Test
        @DisplayName("Un client sans opération a un historique vide")
        void historiqueVide() {
            creerEtSauver("Mouh", "tata", 123);

            assertTrue(repository.historique(123).isEmpty());
        }
    }

    /**
     * REFERME LE TODO #6 DE L'AUDIT PHASE A — ces tests ne travaillent plus
     * sur un faux repository, mais sur une vraie base : l'échec est provoqué
     * par une contrainte SQL, et l'état est vérifié en SQL, pas en mémoire.
     */
    @Nested
    @DisplayName("Atomicité du virement")
    class Atomicite {

        private Client mouh;
        private Client amine;

        @BeforeEach
        void deuxClientsApprovisionnes() {
            mouh = new Client("Mouh", "tata", 123, new OffreStandardFactory());
            amine = new Client("amine", "matoub", 456, new OffreStandardFactory());
            repository.creer(mouh, "tata");
            repository.creer(amine, "matoub");

            BanqueService service = new BanqueService(repository);
            service.deposer(mouh, new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Un virement sauvegardé en une transaction met à jour les deux clients")
        void virementGroupeEcritLesDeuxCotes() throws SQLException {
            mouh.debiter(new BigDecimal("300.00"));
            amine.crediter(new BigDecimal("300.00"));

            repository.sauvegarderEnsemble(mouh, amine);

            assertEquals(0, new BigDecimal("200.00")
                    .compareTo(soldeEnBase(123, "CompteStandard")));
            assertEquals(0, new BigDecimal("300.00")
                    .compareTo(soldeEnBase(456, "CompteStandard")));
        }

        /**
         * L'échec est provoqué au MILIEU de l'écriture : le premier client
         * est déjà écrit quand le second échoue. Sans transaction, la base
         * garderait un émetteur débité et un destinataire jamais crédité.
         */
        @Test
        @DisplayName("Si la sauvegarde du destinataire échoue, le débit de l'émetteur est annulé")
        void echecAMiCheminAnnuleToutLeVirement() throws SQLException {
            mouh.debiter(new BigDecimal("300.00"));
            amine.crediter(new BigDecimal("300.00"));
            // Le destinataire disparaît de la base : sa mise à jour échouera,
            // alors que celle de l'émetteur sera déjà passée.
            supprimerClient(456);

            assertThrows(PersistanceException.class,
                    () -> repository.sauvegarderEnsemble(mouh, amine));

            assertEquals(0, new BigDecimal("500.00").compareTo(soldeEnBase(123, "CompteStandard")),
                    "Le débit de l'émetteur aurait dû être annulé par le rollback.");
        }

        @Test
        @DisplayName("Après un rollback, le client relu depuis la base a bien son solde d'avant")
        void apresRollbackLeClientReluEstIntact() throws SQLException {
            mouh.debiter(new BigDecimal("300.00"));
            supprimerClient(456);

            assertThrows(PersistanceException.class,
                    () -> repository.sauvegarderEnsemble(mouh, amine));

            assertEquals(0, new BigDecimal("500.00")
                    .compareTo(repository.findByRib(123).orElseThrow().getSoldeCompte()));
        }

        @Test
        @DisplayName("Le mode de validation automatique est rétabli après un échec")
        void autoCommitRetabliApresEchec() throws SQLException {
            supprimerClient(456);

            assertThrows(PersistanceException.class,
                    () -> repository.sauvegarderEnsemble(mouh, amine));

            assertTrue(connexion.getAutoCommit(),
                    "Une transaction ratée ne doit pas laisser la connexion en mode manuel.");
        }

        @Test
        @DisplayName("Le mode de validation automatique est rétabli après un succès")
        void autoCommitRetabliApresSucces() throws SQLException {
            repository.sauvegarderEnsemble(mouh, amine);

            assertTrue(connexion.getAutoCommit());
        }

        private void supprimerClient(int rib) throws SQLException {
            try (Statement statement = connexion.createStatement()) {
                statement.executeUpdate("DELETE FROM comptes WHERE client_rib = " + rib);
                statement.executeUpdate("DELETE FROM clients WHERE rib = " + rib);
            }
        }
    }
}
