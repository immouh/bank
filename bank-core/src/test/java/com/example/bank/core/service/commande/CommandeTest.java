package com.example.bank.core.service.commande;

import com.example.bank.core.exception.validation.MontantInvalideException;
import com.example.bank.core.exception.etat.SoldeInsuffisantException;
import com.example.bank.core.exception.validation.VirementVersSoiMemeException;
import com.example.bank.core.model.Client;
import com.example.bank.core.model.Transaction;
import com.example.bank.core.repository.ClientRepository;
import com.example.bank.core.repository.InMemoryClientRepository;
import com.example.bank.core.service.BanqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les trois opérations réifiées en commandes.
 *
 * CE QUI EST VÉRIFIÉ ICI — que la commande produit bien le même effet que
 * l'appel direct au service : soldes ET lignes d'historique. Les règles
 * elles-mêmes (montant positif, solde suffisant) appartiennent à
 * {@code BanqueService} ; on vérifie seulement qu'elles remontent intactes à
 * travers la commande, sans être avalées ni redéfinies.
 *
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("500" != "500.00").
 */
@DisplayName("Commandes bancaires")
class CommandeTest {

    private BanqueService banqueService;
    private Client mouh;
    private Client amine;

    @BeforeEach
    void preparerLaBanque() {
        ClientRepository repository = new InMemoryClientRepository();
        banqueService = new BanqueService(repository);
        mouh = banqueService.rechercherParRib(123);
        amine = banqueService.rechercherParRib(456);
    }

    @Nested
    @DisplayName("Dépôt")
    class Deposer {

        @Test
        @DisplayName("La commande crédite le compte du montant exact")
        void depotCrediteLeCompte() {
            new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")).executer();

            assertEquals(0, new BigDecimal("500.00").compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("La commande produit une transaction de type DEPOT")
        void depotProduitUneTransaction() {
            new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")).executer();

            List<Transaction> historique = mouh.getHistorique();
            assertEquals(1, historique.size());
            assertEquals(Transaction.TypeTransaction.DEPOT, historique.get(0).getType());
            assertEquals(0, new BigDecimal("500.00").compareTo(historique.get(0).getMontant()));
        }

        @Test
        @DisplayName("Deux dépôts s'additionnent au lieu de s'écraser")
        void deuxDepotsSAdditionnent() {
            new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")).executer();
            new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")).executer();

            assertEquals(0, new BigDecimal("1000.00").compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un montant négatif reste refusé à travers la commande")
        void montantNegatifRefuse() {
            Commande commande = new DeposerCommande(banqueService, mouh, new BigDecimal("-10.00"));

            assertThrows(MontantInvalideException.class, commande::executer);
        }

        @Test
        @DisplayName("Le libellé décrit l'opération et son bénéficiaire")
        void libelleDecritLOperation() {
            String libelle = new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")).libelle();

            assertTrue(libelle.contains("500.00"), libelle);
            assertTrue(libelle.contains("Mouh"), libelle);
        }
    }

    @Nested
    @DisplayName("Retrait")
    class Retirer {

        @BeforeEach
        void approvisionner() {
            new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")).executer();
        }

        @Test
        @DisplayName("La commande débite le compte du montant exact")
        void retraitDebiteLeCompte() {
            new RetirerCommande(banqueService, mouh, new BigDecimal("200.00")).executer();

            assertEquals(0, new BigDecimal("300.00").compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("La commande produit une transaction de type RETRAIT")
        void retraitProduitUneTransaction() {
            new RetirerCommande(banqueService, mouh, new BigDecimal("200.00")).executer();

            List<Transaction> historique = mouh.getHistorique();
            assertEquals(Transaction.TypeTransaction.RETRAIT,
                    historique.get(historique.size() - 1).getType());
        }

        @Test
        @DisplayName("Un retrait supérieur au solde reste refusé à travers la commande")
        void retraitSuperieurAuSoldeRefuse() {
            Commande commande = new RetirerCommande(banqueService, mouh, new BigDecimal("99999.00"));

            assertThrows(SoldeInsuffisantException.class, commande::executer);
        }

        @Test
        @DisplayName("Un retrait refusé laisse le solde intact")
        void retraitRefuseLaisseLeSoldeIntact() {
            Commande commande = new RetirerCommande(banqueService, mouh, new BigDecimal("99999.00"));

            assertThrows(SoldeInsuffisantException.class, commande::executer);
            assertEquals(0, new BigDecimal("500.00").compareTo(mouh.getSoldeCompte()));
        }
    }

    @Nested
    @DisplayName("Virement")
    class Virer {

        @BeforeEach
        void approvisionner() {
            new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")).executer();
        }

        @Test
        @DisplayName("La commande débite l'émetteur et crédite le destinataire")
        void virementDeplaceLArgent() {
            new VirerCommande(banqueService, mouh, amine, new BigDecimal("300.00")).executer();

            assertEquals(0, new BigDecimal("200.00").compareTo(mouh.getSoldeCompte()));
            assertEquals(0, new BigDecimal("300.00").compareTo(amine.getSoldeCompte()));
        }

        @Test
        @DisplayName("La commande produit DEUX transactions, une de chaque côté")
        void virementProduitDeuxTransactions() {
            int avantEmetteur = mouh.getHistorique().size();

            new VirerCommande(banqueService, mouh, amine, new BigDecimal("300.00")).executer();

            assertEquals(avantEmetteur + 1, mouh.getHistorique().size());
            assertEquals(1, amine.getHistorique().size());
            assertEquals(Transaction.TypeTransaction.VIREMENT,
                    amine.getHistorique().get(0).getType());
        }

        @Test
        @DisplayName("La ligne du destinataire nomme l'émetteur")
        void ligneDuDestinataireNommeLEmetteur() {
            new VirerCommande(banqueService, mouh, amine, new BigDecimal("300.00")).executer();

            assertTrue(amine.getHistorique().get(0).getDescription().contains("Mouh"));
        }

        @Test
        @DisplayName("Un virement vers soi-même reste refusé à travers la commande")
        void virementVersSoiMemeRefuse() {
            Commande commande = new VirerCommande(banqueService, mouh, mouh, new BigDecimal("10.00"));

            assertThrows(VirementVersSoiMemeException.class, commande::executer);
        }

        @Test
        @DisplayName("Un virement refusé ne déplace aucun argent")
        void virementRefuseNeDeplaceRien() {
            Commande commande = new VirerCommande(banqueService, mouh, amine, new BigDecimal("99999.00"));

            assertThrows(SoldeInsuffisantException.class, commande::executer);
            assertEquals(0, new BigDecimal("500.00").compareTo(mouh.getSoldeCompte()));
            assertEquals(0, new BigDecimal("0.00").compareTo(amine.getSoldeCompte()));
        }

        @Test
        @DisplayName("Le libellé nomme l'émetteur et le destinataire")
        void libelleNommeLesDeuxParties() {
            String libelle = new VirerCommande(banqueService, mouh, amine, new BigDecimal("300.00")).libelle();

            assertTrue(libelle.contains("Mouh"), libelle);
            assertTrue(libelle.contains("amine"), libelle);
        }
    }
}
