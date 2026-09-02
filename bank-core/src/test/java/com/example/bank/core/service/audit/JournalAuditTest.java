package com.example.bank.core.service.audit;

import com.example.bank.core.model.Client;
import com.example.bank.core.repository.BaseDeDonneesH2;
import com.example.bank.core.repository.JdbcClientRepository;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.commande.DeposerCommande;
import com.example.bank.core.service.commande.InvocateurCommande;
import com.example.bank.core.service.commande.RetirerCommande;
import com.example.bank.core.service.commande.VirerCommande;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le journal d'audit, sur une base H2 en mémoire.
 *
 * Une base neuve par test : rien ne dépend de ce qu'un autre a laissé.
 */
@DisplayName("Journal d'audit")
class JournalAuditTest {

    private static final int RIB = 123;

    private Connection connexion;
    private JdbcJournalAudit journal;

    @BeforeEach
    void ouvrirUneBaseNeuve() throws SQLException {
        connexion = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        BaseDeDonneesH2.creerSchema(connexion);
        journal = new JdbcJournalAudit(connexion);
    }

    @AfterEach
    void fermer() throws SQLException {
        connexion.close();
    }

    @Nested
    @DisplayName("Écriture en base")
    class Ecriture {

        /** L'exigence explicite : CHAQUE type d'événement doit produire une ligne. */
        @Test
        @DisplayName("Chaque type d'événement produit bien une ligne en base")
        void chaqueEvenementProduitUneLigne() {
            for (EvenementAudit evenement : EvenementAudit.values()) {
                journal.enregistrer(RIB, evenement);
            }

            List<EvenementAudit> consignes = journal.pour(RIB).stream()
                    .map(LigneAudit::evenement).toList();
            assertEquals(List.of(EvenementAudit.values()), consignes);
        }

        @Test
        @DisplayName("Un journal neuf est vide")
        void journalNeufVide() {
            assertTrue(journal.pour(RIB).isEmpty());
        }

        @Test
        @DisplayName("Les lignes sont rendues dans leur ordre d'écriture")
        void ordreConserve() {
            journal.enregistrer(RIB, EvenementAudit.CONNEXION_ECHOUEE);
            journal.enregistrer(RIB, EvenementAudit.CONNEXION_REUSSIE);

            assertEquals(EvenementAudit.CONNEXION_ECHOUEE, journal.pour(RIB).get(0).evenement());
            assertEquals(EvenementAudit.CONNEXION_REUSSIE, journal.pour(RIB).get(1).evenement());
        }

        @Test
        @DisplayName("Le détail libre est conservé tel quel")
        void detailConserve() {
            journal.enregistrer(RIB, EvenementAudit.COMPTE_VEROUILLE, "15 minutes");

            assertEquals("15 minutes", journal.pour(RIB).get(0).detail());
        }

        @Test
        @DisplayName("Un événement sans détail se consigne sans détail")
        void detailFacultatif() {
            journal.enregistrer(RIB, EvenementAudit.CONNEXION_REUSSIE);

            assertNull(journal.pour(RIB).get(0).detail());
        }

        @Test
        @DisplayName("Chaque ligne est horodatée au moment de l'événement")
        void ligneHorodatee() {
            LocalDateTime avant = LocalDateTime.now();
            journal.enregistrer(RIB, EvenementAudit.CONNEXION_REUSSIE);
            LocalDateTime apres = LocalDateTime.now();

            LocalDateTime horodatage = journal.pour(RIB).get(0).horodatage();
            assertFalse(horodatage.isBefore(avant));
            assertFalse(horodatage.isAfter(apres));
        }

        @Test
        @DisplayName("Le journal d'un client ne contient pas les événements des autres")
        void cloisonnementParClient() {
            journal.enregistrer(RIB, EvenementAudit.CONNEXION_REUSSIE);
            journal.enregistrer(456, EvenementAudit.CONNEXION_ECHOUEE);

            assertEquals(1, journal.pour(RIB).size());
            assertEquals(1, journal.pour(456).size());
        }

        /**
         * Pas de clé étrangère vers {@code clients}, volontairement : un
         * journal d'audit doit survivre à la disparition de ce qu'il décrit.
         */
        @Test
        @DisplayName("Un événement peut être consigné pour un RIB absent de la table des clients")
        void auditSansClientEnBase() {
            journal.enregistrer(999, EvenementAudit.CONNEXION_ECHOUEE);

            assertEquals(1, journal.pour(999).size());
        }
    }

    @Nested
    @DisplayName("Traçage des opérations bancaires")
    class OperationsBancaires {

        private Client mouh;
        private Client amine;
        private InvocateurCommande invocateur;
        private BanqueService service;

        @BeforeEach
        void preparerDeuxClients() {
            JdbcClientRepository repository = new JdbcClientRepository(connexion);
            mouh = new Client("Mouh", "tata", RIB);
            amine = new Client("amine", "matoub", 456);
            repository.creer(mouh, "haché");
            repository.creer(amine, "haché");
            service = new BanqueService(repository);
            invocateur = new InvocateurCommande(journal);
        }

        @Test
        @DisplayName("Un dépôt exécuté par l'invocateur laisse une ligne DEPOT")
        void depotTrace() {
            invocateur.executer(new DeposerCommande(service, mouh, new BigDecimal("500.00")));

            assertEquals(EvenementAudit.DEPOT, journal.pour(RIB).get(0).evenement());
        }

        @Test
        @DisplayName("Un retrait laisse une ligne RETRAIT")
        void retraitTrace() {
            invocateur.executer(new DeposerCommande(service, mouh, new BigDecimal("500.00")));
            invocateur.executer(new RetirerCommande(service, mouh, new BigDecimal("200.00")));

            assertEquals(EvenementAudit.RETRAIT, journal.pour(RIB).get(1).evenement());
        }

        @Test
        @DisplayName("Un virement laisse une ligne VIREMENT au nom de l'émetteur")
        void virementTrace() {
            invocateur.executer(new DeposerCommande(service, mouh, new BigDecimal("500.00")));
            invocateur.executer(new VirerCommande(service, mouh, amine, new BigDecimal("300.00")));

            assertEquals(EvenementAudit.VIREMENT, journal.pour(RIB).get(1).evenement());
        }

        @Test
        @DisplayName("Le détail consigné reprend le libellé de l'opération, montant compris")
        void detailReprendLeLibelle() {
            invocateur.executer(new DeposerCommande(service, mouh, new BigDecimal("500.00")));

            assertTrue(journal.pour(RIB).get(0).detail().contains("500.00"),
                    journal.pour(RIB).get(0).detail());
        }

        /** Une opération refusée n'a rien fait : elle n'a rien à laisser au journal. */
        @Test
        @DisplayName("Une opération refusée ne laisse aucune trace")
        void operationRefuseeSansTrace() {
            try {
                invocateur.executer(new RetirerCommande(service, mouh, new BigDecimal("99999.00")));
            } catch (RuntimeException attendu) {
                // le solde ne le permet pas
            }

            assertTrue(journal.pour(RIB).isEmpty());
        }
    }
}
