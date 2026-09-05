package com.example.bank.core.service;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.core.exception.existence.LivretAAbsentException;
import com.example.bank.core.exception.existence.LivretADejaExistantException;
import com.example.bank.core.exception.validation.MontantInvalideException;
import com.example.bank.core.exception.etat.SoldeInsuffisantException;
import com.example.bank.core.exception.validation.VirementVersSoiMemeException;
import com.example.bank.core.model.Client;
import com.example.bank.core.model.Transaction;
import com.example.bank.core.model.offre.concret.OffreEtudianteFactory;
import com.example.bank.core.model.offre.concret.OffrePremiumFactory;
import com.example.bank.core.repository.ClientRepository;
import com.example.bank.core.repository.InMemoryClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'orchestration : dépôt, retrait, virement, Livret A.
 *
 * FILET DE SÉCURITÉ AVANT REFONTE — ces tests documentent le comportement
 * ACTUEL. Les écarts constatés sont signalés par {@code TODO Phase A} et
 * n'ont volontairement pas été corrigés.
 *
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("300" != "300.00").
 */
@DisplayName("Service bancaire")
class BanqueServiceTest {

    private BanqueService service;
    private Client mouh;
    private Client amine;

    @BeforeEach
    void preparerLaBanque() {
        ClientRepository repository = new InMemoryClientRepository();
        service = new BanqueService(repository);
        mouh = service.rechercherParRib(123);
        amine = service.rechercherParRib(456);
    }

    /** Dernière ligne d'historique du client, celle que l'opération vient de produire. */
    private static Transaction derniereTransaction(Client client) {
        List<Transaction> historique = client.getHistorique();
        return historique.get(historique.size() - 1);
    }

    @Nested
    @DisplayName("Dépôt")
    class Deposer {

        @Test
        @DisplayName("Un dépôt crédite le compte du montant exact")
        void depotCrediteLeCompte() {
            service.deposer(mouh, new BigDecimal("500.00"));

            assertEquals(0, new BigDecimal("500.00").compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un dépôt produit une transaction de type DEPOT au montant exact")
        void depotProduitUneTransaction() {
            service.deposer(mouh, new BigDecimal("500.00"));

            Transaction trace = derniereTransaction(mouh);
            assertEquals(Transaction.TypeTransaction.DEPOT, trace.getType());
            assertEquals(0, new BigDecimal("500.00").compareTo(trace.getMontant()));
        }

        @Test
        @DisplayName("La transaction produite est horodatée au moment de l'opération")
        void transactionHorodatee() {
            LocalDateTime avant = LocalDateTime.now();
            service.deposer(mouh, new BigDecimal("500.00"));
            LocalDateTime apres = LocalDateTime.now();

            LocalDateTime date = derniereTransaction(mouh).getDate();
            assertFalse(date.isBefore(avant), "Transaction horodatée avant l'opération : " + date);
            assertFalse(date.isAfter(apres), "Transaction horodatée après l'opération : " + date);
        }

        @Test
        @DisplayName("Un dépôt de montant nul est refusé")
        void depotNulRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> service.deposer(mouh, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Un dépôt négatif est refusé")
        void depotNegatifRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> service.deposer(mouh, new BigDecimal("-500.00")));
        }

        @Test
        @DisplayName("Un dépôt refusé ne laisse ni solde modifié ni ligne d'historique")
        void depotRefuseNeLaisseAucuneTrace() {
            assertThrows(MontantInvalideException.class,
                    () -> service.deposer(mouh, new BigDecimal("-500.00")));

            assertEquals(0, new BigDecimal("0.00").compareTo(mouh.getSoldeCompte()));
            assertTrue(mouh.getHistorique().isEmpty());
        }

        @Test
        @DisplayName("Un dépôt sur un client inexistant est refusé")
        void depotSurClientNulRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> service.deposer(null, new BigDecimal("500.00")));
        }
    }

    @Nested
    @DisplayName("Retrait")
    class Retirer {

        @BeforeEach
        void approvisionner() {
            service.deposer(mouh, new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Un retrait débite le compte du montant exact")
        void retraitDebiteLeCompte() {
            service.retirer(mouh, new BigDecimal("200.00"));

            assertEquals(0, new BigDecimal("300.00").compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un retrait produit une transaction de type RETRAIT au montant exact")
        void retraitProduitUneTransaction() {
            service.retirer(mouh, new BigDecimal("200.00"));

            Transaction trace = derniereTransaction(mouh);
            assertEquals(Transaction.TypeTransaction.RETRAIT, trace.getType());
            assertEquals(0, new BigDecimal("200.00").compareTo(trace.getMontant()));
        }

        @Test
        @DisplayName("Un retrait égal au solde est accepté")
        void retraitEgalAuSoldeAccepte() {
            service.retirer(mouh, new BigDecimal("500.00"));

            assertEquals(0, new BigDecimal("0.00").compareTo(mouh.getSoldeCompte()));
        }

        /**
         * PHASE B — le découvert du tier traverse le service sans que celui-ci
         * ait une ligne de code à ce sujet : il appelle {@code client.debiter},
         * qui délègue au {@code Compte}, qui applique le plafond de son offre.
         */
        @Test
        @DisplayName("Un retrait d'un centime au-delà du solde est accepté : le découvert de l'offre traverse le service")
        void decouvertDeLOffreTraverseLeService() {
            service.retirer(mouh, new BigDecimal("500.01"));

            assertEquals(0, new BigDecimal("-0.01").compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un retrait qui met le compte en découvert produit quand même sa transaction")
        void retraitEnDecouvertTraceQuandMeme() {
            service.retirer(mouh, new BigDecimal("500.01"));

            assertEquals(Transaction.TypeTransaction.RETRAIT, derniereTransaction(mouh).getType());
        }

        @Test
        @DisplayName("Un retrait supérieur au solde laisse le solde strictement intact")
        void retraitRefuseLaisseLeSoldeIntact() {
            BigDecimal avant = mouh.getSoldeCompte();

            assertThrows(SoldeInsuffisantException.class,
                    () -> service.retirer(mouh, new BigDecimal("99999.00")));

            assertEquals(0, avant.compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un retrait refusé ne produit aucune ligne d'historique")
        void retraitRefuseNeProduitAucuneTrace() {
            int avant = mouh.getHistorique().size();

            assertThrows(SoldeInsuffisantException.class,
                    () -> service.retirer(mouh, new BigDecimal("99999.00")));

            assertEquals(avant, mouh.getHistorique().size());
        }

        @Test
        @DisplayName("Un retrait de montant nul est refusé")
        void retraitNulRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> service.retirer(mouh, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Un retrait négatif est refusé")
        void retraitNegatifRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> service.retirer(mouh, new BigDecimal("-100.00")));
        }
    }

    @Nested
    @DisplayName("Virement entre clients")
    class Virer {

        @BeforeEach
        void approvisionnerLEmetteur() {
            service.deposer(mouh, new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Un virement débite l'émetteur et crédite le destinataire du même montant")
        void virementDeplaceLArgent() {
            service.virer(mouh, amine, new BigDecimal("300.00"));

            assertEquals(0, new BigDecimal("200.00").compareTo(mouh.getSoldeCompte()));
            assertEquals(0, new BigDecimal("300.00").compareTo(amine.getSoldeCompte()));
        }

        /**
         * BUG HISTORIQUE — le destinataire d'un virement ne recevait aucune
         * ligne d'historique.
         */
        @Test
        @DisplayName("Un virement produit DEUX transactions, une chez chaque partie")
        void virementProduitDeuxTransactions() {
            int avantEmetteur = mouh.getHistorique().size();

            service.virer(mouh, amine, new BigDecimal("300.00"));

            assertEquals(avantEmetteur + 1, mouh.getHistorique().size());
            assertEquals(1, amine.getHistorique().size());
        }

        @Test
        @DisplayName("La transaction de l'émetteur est un VIREMENT au bon montant, qui nomme le destinataire et son RIB")
        void transactionDeLEmetteur() {
            service.virer(mouh, amine, new BigDecimal("300.00"));

            Transaction trace = derniereTransaction(mouh);
            assertEquals(Transaction.TypeTransaction.VIREMENT, trace.getType());
            assertEquals(0, new BigDecimal("300.00").compareTo(trace.getMontant()));
            assertTrue(trace.getDescription().contains("amine"), trace.getDescription());
            assertTrue(trace.getDescription().contains("456"), trace.getDescription());
        }

        @Test
        @DisplayName("La transaction du destinataire est un VIREMENT au bon montant, qui nomme l'émetteur et son RIB")
        void transactionDuDestinataire() {
            service.virer(mouh, amine, new BigDecimal("300.00"));

            Transaction trace = derniereTransaction(amine);
            assertEquals(Transaction.TypeTransaction.VIREMENT, trace.getType());
            assertEquals(0, new BigDecimal("300.00").compareTo(trace.getMontant()));
            assertTrue(trace.getDescription().contains("Mouh"), trace.getDescription());
            assertTrue(trace.getDescription().contains("123"), trace.getDescription());
        }

        @Test
        @DisplayName("Les deux transactions d'un même virement sont horodatées pendant l'opération")
        void lesDeuxTransactionsSontHorodatees() {
            LocalDateTime avant = LocalDateTime.now();
            service.virer(mouh, amine, new BigDecimal("300.00"));
            LocalDateTime apres = LocalDateTime.now();

            for (Transaction trace : List.of(derniereTransaction(mouh), derniereTransaction(amine))) {
                assertFalse(trace.getDate().isBefore(avant), "Horodatage antérieur : " + trace);
                assertFalse(trace.getDate().isAfter(apres), "Horodatage postérieur : " + trace);
            }
        }

        @Test
        @DisplayName("Un virement vers soi-même est refusé")
        void virementVersSoiMemeRefuse() {
            assertThrows(VirementVersSoiMemeException.class,
                    () -> service.virer(mouh, mouh, new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Un virement vers soi-même ne modifie aucun solde et ne crée aucune transaction")
        void virementVersSoiMemeSansEffet() {
            int avant = mouh.getHistorique().size();

            assertThrows(VirementVersSoiMemeException.class,
                    () -> service.virer(mouh, mouh, new BigDecimal("100.00")));

            assertEquals(0, new BigDecimal("500.00").compareTo(mouh.getSoldeCompte()));
            assertEquals(avant, mouh.getHistorique().size());
        }

        /**
         * REFERME LE TODO #4 DE L'AUDIT — CHANGEMENT DE COMPORTEMENT.
         *
         * L'ordre de validation plaçait « vers soi-même » AVANT le montant :
         * un virement vers soi-même d'un montant négatif remontait
         * {@code VirementVersSoiMemeException} et taisait que le montant était
         * lui aussi invalide. L'ordre est désormais inverse — le montant est
         * la règle la plus fondamentale des deux, et celle que l'utilisateur
         * peut corriger sans rien savoir des destinataires.
         */
        @Test
        @DisplayName("Vers soi-même ET avec un montant invalide, c'est le montant qui est signalé en premier")
        void ordreDeValidationDuVirement() {
            assertThrows(MontantInvalideException.class,
                    () -> service.virer(mouh, mouh, new BigDecimal("-100.00")));
        }

        /** L'inversion ne doit pas avoir masqué la règle métier elle-même. */
        @Test
        @DisplayName("Vers soi-même avec un montant valide, c'est toujours le virement vers soi-même qui est signalé")
        void versSoiMemeAvecMontantValideSignaleToujoursLaRegle() {
            assertThrows(VirementVersSoiMemeException.class,
                    () -> service.virer(mouh, mouh, new BigDecimal("100.00")));
        }

        /** ATOMICITÉ — le solde de l'émetteur doit être strictement identique avant et après l'échec. */
        @Test
        @DisplayName("Un virement au-delà du solde laisse l'émetteur strictement intact")
        void virementEnEchecLaisseLEmetteurIntact() {
            BigDecimal avant = mouh.getSoldeCompte();

            assertThrows(SoldeInsuffisantException.class,
                    () -> service.virer(mouh, amine, new BigDecimal("99999.00")));

            assertEquals(0, avant.compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un virement au-delà du solde ne crédite pas le destinataire")
        void virementEnEchecNeCreditePasLeDestinataire() {
            assertThrows(SoldeInsuffisantException.class,
                    () -> service.virer(mouh, amine, new BigDecimal("99999.00")));

            assertEquals(0, new BigDecimal("0.00").compareTo(amine.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un virement en échec ne produit aucune transaction, d'aucun côté")
        void virementEnEchecNeProduitAucuneTransaction() {
            int avant = mouh.getHistorique().size();

            assertThrows(SoldeInsuffisantException.class,
                    () -> service.virer(mouh, amine, new BigDecimal("99999.00")));

            assertEquals(avant, mouh.getHistorique().size());
            assertTrue(amine.getHistorique().isEmpty());
        }

        @Test
        @DisplayName("Un virement de montant nul est refusé")
        void virementNulRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> service.virer(mouh, amine, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Un virement de montant négatif est refusé")
        void virementNegatifRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> service.virer(mouh, amine, new BigDecimal("-100.00")));
        }

        @Test
        @DisplayName("Un virement de montant négatif ne déplace aucun argent")
        void virementNegatifNeDeplaceRien() {
            assertThrows(MontantInvalideException.class,
                    () -> service.virer(mouh, amine, new BigDecimal("-100.00")));

            assertEquals(0, new BigDecimal("500.00").compareTo(mouh.getSoldeCompte()));
            assertEquals(0, new BigDecimal("0.00").compareTo(amine.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un virement vers un destinataire inexistant est refusé")
        void destinataireInexistantRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> service.virer(mouh, null, new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Un virement depuis un émetteur inexistant est refusé")
        void emetteurInexistantRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> service.virer(null, amine, new BigDecimal("100.00")));
        }
    }

    @Nested
    @DisplayName("Livret A")
    class LivretA {

        @Test
        @DisplayName("La création ouvre le Livret A à zéro")
        void creationAZero() {
            service.creerLivretA(mouh);

            assertTrue(mouh.isLivretAExiste());
            assertEquals(0, new BigDecimal("0.00").compareTo(mouh.getSoldeLivretA()));
        }

        /** PHASE B — refus par exception métier, attrapable par l'IHM. */
        @Test
        @DisplayName("Créer un second Livret A est refusé par une exception métier, que l'IHM sait afficher")
        void secondeCreationRefusee() {
            service.creerLivretA(mouh);

            LivretADejaExistantException levee = assertThrows(LivretADejaExistantException.class,
                    () -> service.creerLivretA(mouh));
            assertInstanceOf(BanqueException.class, levee);
        }

        @Test
        @DisplayName("La création d'un Livret A ne produit aucune ligne d'historique")
        void creationSansTrace() {
            service.creerLivretA(mouh);

            assertTrue(mouh.getHistorique().isEmpty());
        }

        @Test
        @DisplayName("Un virement interne déplace l'argent du compte courant vers le Livret A")
        void virementInterneAlimenteLEpargne() {
            service.deposer(mouh, new BigDecimal("1000.00"));
            service.creerLivretA(mouh);

            service.virerVersLivretA(mouh, new BigDecimal("400.00"));

            assertEquals(0, new BigDecimal("600.00").compareTo(mouh.getSoldeCompte()));
            assertEquals(0, new BigDecimal("400.00").compareTo(mouh.getSoldeLivretA()));
        }

        @Test
        @DisplayName("Un virement interne produit UNE seule transaction de type VIREMENT")
        void virementInterneProduitUneTransaction() {
            service.deposer(mouh, new BigDecimal("1000.00"));
            service.creerLivretA(mouh);
            int avant = mouh.getHistorique().size();

            service.virerVersLivretA(mouh, new BigDecimal("400.00"));

            assertEquals(avant + 1, mouh.getHistorique().size());
            assertEquals(Transaction.TypeTransaction.VIREMENT, derniereTransaction(mouh).getType());
        }

        @Test
        @DisplayName("Alimenter un Livret A inexistant est refusé")
        void virementInterneSansLivretRefuse() {
            service.deposer(mouh, new BigDecimal("1000.00"));

            assertThrows(LivretAAbsentException.class,
                    () -> service.virerVersLivretA(mouh, new BigDecimal("400.00")));
        }

        /**
         * L'absence de Livret A est vérifiée AVANT le débit : sans cela, le
         * compte courant serait débité par une opération qui échoue juste après.
         */
        @Test
        @DisplayName("Alimenter un Livret A inexistant laisse le compte courant intact")
        void virementInterneSansLivretLaisseLeCompteIntact() {
            service.deposer(mouh, new BigDecimal("1000.00"));

            assertThrows(LivretAAbsentException.class,
                    () -> service.virerVersLivretA(mouh, new BigDecimal("400.00")));

            assertEquals(0, new BigDecimal("1000.00").compareTo(mouh.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un virement interne au-delà du solde laisse compte courant et Livret A intacts")
        void virementInterneAuDelaDuSoldeSansEffet() {
            service.deposer(mouh, new BigDecimal("100.00"));
            service.creerLivretA(mouh);

            // 500 € : au-delà des 100 € de solde ET des 300 € de découvert Standard.
            assertThrows(SoldeInsuffisantException.class,
                    () -> service.virerVersLivretA(mouh, new BigDecimal("500.00")));

            assertEquals(0, new BigDecimal("100.00").compareTo(mouh.getSoldeCompte()));
            assertEquals(0, new BigDecimal("0.00").compareTo(mouh.getSoldeLivretA()));
        }

        @Test
        @DisplayName("Un virement interne de montant négatif est refusé")
        void virementInterneNegatifRefuse() {
            service.creerLivretA(mouh);

            assertThrows(MontantInvalideException.class,
                    () -> service.virerVersLivretA(mouh, new BigDecimal("-100.00")));
        }
    }

    @Nested
    @DisplayName("Recherche par RIB")
    class RechercherParRib {

        @Test
        @DisplayName("Un RIB connu rend le client correspondant")
        void ribConnuRendLeClient() {
            assertEquals("Mouh", service.rechercherParRib(123).getNom());
        }

        @Test
        @DisplayName("Un RIB inconnu est refusé, et le message indique le RIB cherché")
        void ribInconnuRefuse() {
            ClientIntrouvableException levee = assertThrows(ClientIntrouvableException.class,
                    () -> service.rechercherParRib(999));

            assertTrue(levee.getMessage().contains("999"), levee.getMessage());
        }
    }

    @Nested
    @DisplayName("Persistance des opérations")
    class Persistance {

        // TODO Phase A : comportement à vérifier — le service MUTE les objets
        // métier puis appelle save(). Avec le repository en mémoire, les deux
        // pointent sur la même instance, donc l'incohérence ne se voit pas. Avec
        // un futur JdbcClientRepository, un échec entre le débit et le save
        // laisserait l'émetteur débité en mémoire et intact en base. Le test
        // ci-dessous rend cet écart visible dès aujourd'hui : c'est ce que la
        // transaction SQL atomique de la phase C devra fermer.
        @Test
        @DisplayName("Un virement modifie les soldes AVANT de les persister : un save en échec laisse la mémoire déjà modifiée")
        void mutationAvantPersistance() {
            RepositoryQuiEchoueAuSave repository = new RepositoryQuiEchoueAuSave();
            BanqueService serviceFragile = new BanqueService(repository);
            Client emetteur = new Client("Emetteur", "x", 1);
            Client destinataire = new Client("Destinataire", "y", 2);
            repository.accepterEncore(2); // le dépôt initial passe
            serviceFragile.deposer(emetteur, new BigDecimal("500.00"));

            assertThrows(IllegalStateException.class,
                    () -> serviceFragile.virer(emetteur, destinataire, new BigDecimal("300.00")));

            // L'argent a bougé en mémoire alors que la persistance a échoué.
            assertEquals(0, new BigDecimal("200.00").compareTo(emetteur.getSoldeCompte()));
            assertEquals(0, new BigDecimal("300.00").compareTo(destinataire.getSoldeCompte()));
        }

        @Test
        @DisplayName("Le repository en mémoire rend la même instance que celle qui a été modifiée")
        void repositoryEnMemoirePartageLInstance() {
            service.deposer(mouh, new BigDecimal("500.00"));

            assertSame(mouh, service.rechercherParRib(123));
        }
    }

    /**
     * Repository de test qui accepte un nombre donné de {@code save} puis
     * échoue. Sert uniquement à observer l'ordre « muter, puis persister ».
     */
    private static final class RepositoryQuiEchoueAuSave implements ClientRepository {

        private final List<Client> clients = new ArrayList<>();
        private int savesRestants;

        void accepterEncore(int nombre) {
            savesRestants = nombre;
        }

        @Override
        public void save(Client client) {
            if (savesRestants <= 0) {
                throw new IllegalStateException("Persistance indisponible.");
            }
            savesRestants--;
            clients.remove(client);
            clients.add(client);
        }

        @Override
        public Optional<Client> findByRib(int rib) {
            return clients.stream().filter(c -> c.getRib() == rib).findFirst();
        }

        @Override
        public Optional<Client> findByNom(String nom) {
            return clients.stream().filter(c -> c.getNom().equals(nom)).findFirst();
        }

        @Override
        public List<Client> findAll() {
            return new ArrayList<>(clients);
        }

        @Override
        public boolean existsByRib(int rib) {
            return findByRib(rib).isPresent();
        }
    }

    /**
     * PHASE B — l'offre du client arbitre ses opérations, et le service n'a
     * pas une ligne de code à ce sujet : il appelle le modèle, qui délègue au
     * Compte de l'offre.
     */
    @Nested
    @DisplayName("L'offre du client arbitre les opérations du service")
    class OffreDuClient {

        @Test
        @DisplayName("TIER Étudiante — le service refuse le moindre découvert")
        void serviceRefuseLeDecouvertPourUnEtudiant() {
            Client etudiant = new Client("Etudiant", "x", 10, new OffreEtudianteFactory());
            service.deposer(etudiant, new BigDecimal("100.00"));

            assertThrows(SoldeInsuffisantException.class,
                    () -> service.retirer(etudiant, new BigDecimal("100.01")));
        }

        @Test
        @DisplayName("TIER Premium — le service accepte un retrait jusqu'à -2000,00 €")
        void serviceAccepteLeDecouvertPremium() {
            Client premium = new Client("Premium", "x", 11, new OffrePremiumFactory());

            service.retirer(premium, new BigDecimal("2000.00"));

            assertEquals(0, new BigDecimal("-2000.00").compareTo(premium.getSoldeCompte()));
        }

        @Test
        @DisplayName("HIÉRARCHIE — le même virement de 1500 € est refusé à un Étudiant et passe pour un Premium")
        void memeVirementArbitreParLOffre() {
            Client etudiant = new Client("Etudiant", "x", 10, new OffreEtudianteFactory());
            Client premium = new Client("Premium", "x", 11, new OffrePremiumFactory());

            assertThrows(SoldeInsuffisantException.class,
                    () -> service.virer(etudiant, amine, new BigDecimal("1500.00")));
            service.virer(premium, amine, new BigDecimal("1500.00"));

            assertEquals(0, new BigDecimal("-1500.00").compareTo(premium.getSoldeCompte()));
            assertEquals(0, new BigDecimal("1500.00").compareTo(amine.getSoldeCompte()));
        }

        @Test
        @DisplayName("Le Livret A ouvert par le service est rémunéré au taux de l'offre du client")
        void livretOuvertAuTauxDeLOffre() {
            Client etudiant = new Client("Etudiant", "x", 10, new OffreEtudianteFactory());

            service.creerLivretA(etudiant);

            // Nom complet : la classe @Nested LivretA de ce test masque le type.
            var livret = (com.example.bank.core.model.offre.compte.concret.LivretA)
                    etudiant.getLivretA().orElseThrow();
            assertEquals(0, new BigDecimal("0.0200").compareTo(livret.getTauxInteret()));
        }

        @Test
        @DisplayName("Un virement interne n'entame jamais le découvert au profit de l'épargne au-delà du plafond")
        void virementInterneRespecteLeDecouvert() {
            Client premium = new Client("Premium", "x", 11, new OffrePremiumFactory());
            service.creerLivretA(premium);

            service.virerVersLivretA(premium, new BigDecimal("2000.00"));

            assertEquals(0, new BigDecimal("-2000.00").compareTo(premium.getSoldeCompte()));
            assertEquals(0, new BigDecimal("2000.00").compareTo(premium.getSoldeLivretA()));
            assertThrows(SoldeInsuffisantException.class,
                    () -> service.virerVersLivretA(premium, new BigDecimal("0.01")));
        }
    }
}
