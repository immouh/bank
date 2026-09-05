package com.example.bank.core.model;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.exception.existence.LivretAAbsentException;
import com.example.bank.core.exception.existence.LivretADejaExistantException;
import com.example.bank.core.exception.validation.MontantInvalideException;
import com.example.bank.core.exception.etat.SoldeInsuffisantException;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.etat.concret.CompteActif;
import com.example.bank.core.model.offre.compte.etat.concret.CompteEnDecouvert;
import com.example.bank.core.model.offre.concret.OffreEtudianteFactory;
import com.example.bank.core.model.offre.concret.OffrePremiumFactory;
import com.example.bank.core.model.offre.concret.OffreStandardFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le modèle Client : ce qu'il fait de son PROPRE état.
 *
 * FILET DE SÉCURITÉ AVANT REFONTE — ces tests documentent le comportement
 * ACTUEL du coeur historique, y compris ce qui pourra changer en phase B
 * (convergence de {@code Client} et de {@code model.offre.compte.Compte}).
 * Les écarts constatés sont signalés par des commentaires
 * {@code TODO Phase A} et n'ont volontairement pas été corrigés.
 *
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("500" != "500.00").
 */
@DisplayName("Modèle Client")
class ClientTest {

    private static final String MOT_DE_PASSE = "tata";

    private Client client;

    @BeforeEach
    void creerClient() {
        client = new Client("Mouh", MOT_DE_PASSE, 123);
    }

    @Nested
    @DisplayName("À la création")
    class ALaCreation {

        @Test
        @DisplayName("Un client neuf a un compte courant à zéro")
        void compteCourantAZero() {
            assertEquals(0, new BigDecimal("0.00").compareTo(client.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un client neuf n'a pas de Livret A")
        void pasDeLivretA() {
            assertFalse(client.isLivretAExiste());
        }

        @Test
        @DisplayName("Un client neuf a un historique vide")
        void historiqueVide() {
            assertTrue(client.getHistorique().isEmpty());
        }

        @Test
        @DisplayName("Le nom et le RIB sont ceux passés au constructeur")
        void identiteConservee() {
            assertEquals("Mouh", client.getNom());
            assertEquals(123, client.getRib());
        }
    }

    @Nested
    @DisplayName("Crédit du compte courant")
    class Crediter {

        @Test
        @DisplayName("Un crédit augmente le solde du montant exact")
        void creditAugmenteLeSolde() {
            client.crediter(new BigDecimal("500.00"));

            assertEquals(0, new BigDecimal("500.00").compareTo(client.getSoldeCompte()));
        }

        /**
         * BUG HISTORIQUE — le code d'origine écrivait {@code soldeCompte =+ montant},
         * qui ÉCRASE le solde au lieu de l'incrémenter. Ce test le verrouille.
         */
        @Test
        @DisplayName("Deux crédits s'additionnent au lieu de s'écraser")
        void deuxCreditsSAdditionnent() {
            client.crediter(new BigDecimal("500.00"));
            client.crediter(new BigDecimal("500.00"));

            assertEquals(0, new BigDecimal("1000.00").compareTo(client.getSoldeCompte()));
        }

        /**
         * BUG HISTORIQUE — les montants étaient des {@code float}, où dix fois
         * 0,10 € ne fait pas exactement 1,00 €.
         */
        @Test
        @DisplayName("Dix crédits de 0,10 € donnent exactement 1,00 €, sans dérive")
        void pasDeDeriveDeCentimes() {
            for (int i = 0; i < 10; i++) {
                client.crediter(new BigDecimal("0.10"));
            }

            assertEquals(0, new BigDecimal("1.00").compareTo(client.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un crédit nul est refusé")
        void creditNulRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> client.crediter(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Un crédit négatif est refusé")
        void creditNegatifRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> client.crediter(new BigDecimal("-1000.00")));
        }

        @Test
        @DisplayName("Un crédit sans montant est refusé")
        void creditSansMontantRefuse() {
            assertThrows(MontantInvalideException.class, () -> client.crediter(null));
        }

        @Test
        @DisplayName("Un crédit refusé laisse le solde inchangé")
        void creditRefuseLaisseLeSoldeIntact() {
            client.crediter(new BigDecimal("100.00"));

            assertThrows(MontantInvalideException.class,
                    () -> client.crediter(new BigDecimal("-50.00")));
            assertEquals(0, new BigDecimal("100.00").compareTo(client.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un montant à plus de deux décimales est arrondi au centime")
        void montantArrondiAuCentime() {
            client.crediter(new BigDecimal("10.004"));

            assertEquals(0, new BigDecimal("10.00").compareTo(client.getSoldeCompte()));
        }
    }

    @Nested
    @DisplayName("Débit du compte courant")
    class Debiter {

        @BeforeEach
        void approvisionner() {
            client.crediter(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Un débit inférieur au solde diminue le solde du montant exact")
        void debitDiminueLeSolde() {
            client.debiter(new BigDecimal("200.00"));

            assertEquals(0, new BigDecimal("300.00").compareTo(client.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un débit égal au solde est accepté et ramène le compte à zéro")
        void debitEgalAuSoldeAccepte() {
            client.debiter(new BigDecimal("500.00"));

            assertEquals(0, new BigDecimal("0.00").compareTo(client.getSoldeCompte()));
        }

        /**
         * PHASE B — le découvert du tier s'applique désormais. Le client par
         * défaut souscrit l'offre Standard, donc 300 € de découvert : un débit
         * d'un centime au-delà du solde passe, là où l'ancien modèle le
         * refusait. Le détail par tier est couvert plus bas, dans
         * {@code DecouvertSelonLOffre}.
         */
        @Test
        @DisplayName("Un débit d'un centime au-delà du solde est accepté : le découvert de l'offre s'applique")
        void decouvertDeLOffreApplique() {
            client.debiter(new BigDecimal("500.01"));

            assertEquals(0, new BigDecimal("-0.01").compareTo(client.getSoldeCompte()));
        }

        @Test
        @DisplayName("Un débit supérieur au solde est refusé")
        void debitSuperieurAuSoldeRefuse() {
            assertThrows(SoldeInsuffisantException.class,
                    () -> client.debiter(new BigDecimal("99999.00")));
        }

        @Test
        @DisplayName("Un débit refusé laisse le solde strictement inchangé")
        void debitRefuseLaisseLeSoldeIntact() {
            assertThrows(SoldeInsuffisantException.class,
                    () -> client.debiter(new BigDecimal("99999.00")));

            assertEquals(0, new BigDecimal("500.00").compareTo(client.getSoldeCompte()));
        }

        /**
         * PHASE B — le « disponible » annoncé n'est plus le seul solde, mais le
         * solde AUGMENTÉ du découvert de l'offre : 500 € + 300 € = 800 €.
         */
        @Test
        @DisplayName("Le message de solde insuffisant indique le disponible découvert compris, et le montant demandé")
        void messageDeSoldeInsuffisantEstParlant() {
            SoldeInsuffisantException levee = assertThrows(SoldeInsuffisantException.class,
                    () -> client.debiter(new BigDecimal("900.00")));

            assertTrue(levee.getMessage().contains("900.00"), levee.getMessage());
            assertTrue(levee.getMessage().contains("800.00"), levee.getMessage());
        }

        @Test
        @DisplayName("Un débit nul est refusé")
        void debitNulRefuse() {
            assertThrows(MontantInvalideException.class, () -> client.debiter(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Un débit négatif est refusé : il créditerait le compte")
        void debitNegatifRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> client.debiter(new BigDecimal("-100.00")));
        }
    }

    @Nested
    @DisplayName("Mot de passe")
    class MotDePasse {

        @Test
        @DisplayName("Le bon mot de passe est reconnu")
        void bonMotDePasseReconnu() {
            assertTrue(client.verifierMotDePasse(MOT_DE_PASSE));
        }

        @Test
        @DisplayName("Un mauvais mot de passe est rejeté")
        void mauvaisMotDePasseRejete() {
            assertFalse(client.verifierMotDePasse("mauvais"));
        }

        @Test
        @DisplayName("La casse compte : « Tata » n'est pas « tata »")
        void casseSignificative() {
            assertFalse(client.verifierMotDePasse("Tata"));
        }

        @Test
        @DisplayName("Une saisie vide est rejetée")
        void saisieVideRejetee() {
            assertFalse(client.verifierMotDePasse(""));
        }

        @Test
        @DisplayName("Une saisie absente est rejetée sans lever d'exception")
        void saisieNulleRejetee() {
            assertFalse(client.verifierMotDePasse(null));
        }

        @Test
        @DisplayName("Un client sans mot de passe enregistré n'authentifie personne, même avec null")
        void clientSansMotDePasseNAuthentifiePersonne() {
            Client sansMdp = new Client("X", null, 999);

            assertFalse(sansMdp.verifierMotDePasse(null));
            assertFalse(sansMdp.verifierMotDePasse(""));
        }

        /**
         * SÉCURITÉ — {@code getMdp()} a été supprimé au profit de
         * {@code verifierMotDePasse}. Ce test empêche sa réapparition sous
         * n'importe quel nom : aucun accesseur public sans argument ne doit
         * rendre le mot de passe.
         */
        @Test
        @DisplayName("Aucun accesseur public ne rend le mot de passe en clair")
        void aucunAccesseurNeRendLeMotDePasse() throws Exception {
            for (Method methode : Client.class.getMethods()) {
                if (methode.getParameterCount() != 0 || methode.getDeclaringClass() == Object.class) {
                    continue;
                }
                Object valeur = methode.invoke(client);
                assertFalse(MOT_DE_PASSE.equals(valeur),
                        "La méthode " + methode.getName() + "() rend le mot de passe en clair.");
            }
        }

        @Test
        @DisplayName("La représentation textuelle du client ne contient pas le mot de passe")
        void toStringNeFuitPasLeMotDePasse() {
            assertFalse(client.toString().contains(MOT_DE_PASSE), client.toString());
        }
    }

    @Nested
    @DisplayName("Livret A")
    class LivretA {

        @Test
        @DisplayName("L'ouverture crée un Livret A à zéro")
        void ouvertureAZero() {
            client.ouvrirLivretA();

            assertTrue(client.isLivretAExiste());
            assertEquals(0, new BigDecimal("0.00").compareTo(client.getSoldeLivretA()));
        }

        @Test
        @DisplayName("L'ouverture ne touche pas au compte courant")
        void ouvertureNeTouchePasAuCompteCourant() {
            client.crediter(new BigDecimal("500.00"));

            client.ouvrirLivretA();

            assertEquals(0, new BigDecimal("500.00").compareTo(client.getSoldeCompte()));
        }

        /**
         * PHASE B — le refus passe désormais par une exception MÉTIER. L'IHM,
         * qui n'attrape que {@code BanqueException}, affiche donc un message
         * lisible au lieu de laisser fuiter une erreur technique.
         */
        @Test
        @DisplayName("Ouvrir un second Livret A est refusé par une exception métier, que l'IHM sait afficher")
        void secondeOuvertureRefusee() {
            client.ouvrirLivretA();

            LivretADejaExistantException levee = assertThrows(LivretADejaExistantException.class,
                    () -> client.ouvrirLivretA());
            assertInstanceOf(BanqueException.class, levee);
        }

        @Test
        @DisplayName("Une seconde ouverture refusée n'ajoute pas de compte en double")
        void secondeOuvertureNAjoutePasDeCompte() {
            client.ouvrirLivretA();

            assertThrows(LivretADejaExistantException.class, () -> client.ouvrirLivretA());
            assertEquals(2, client.getComptes().size());
        }

        @Test
        @DisplayName("Un crédit sur Livret A augmente son solde du montant exact")
        void creditDuLivretA() {
            client.ouvrirLivretA();

            client.crediterLivretA(new BigDecimal("1000.00"));

            assertEquals(0, new BigDecimal("1000.00").compareTo(client.getSoldeLivretA()));
        }

        @Test
        @DisplayName("Créditer un Livret A inexistant est refusé")
        void creditSansLivretRefuse() {
            assertThrows(LivretAAbsentException.class,
                    () -> client.crediterLivretA(new BigDecimal("100.00")));
        }

        /**
         * REFERME LE TODO #3 DE L'AUDIT — CHANGEMENT DE COMPORTEMENT.
         *
         * L'ordre de validation contrôlait le MONTANT avant l'existence du
         * Livret A : un montant négatif sur un client sans livret remontait
         * {@code MontantInvalideException} et taisait la cause première. Le
         * message d'erreur envoyait alors corriger une saisie, alors qu'aucune
         * saisie n'aurait pu aboutir alors qu'il n'y a pas de livret à
         * créditer. L'ordre est désormais inverse — l'absence du livret est le
         * défaut le plus fondamental des deux.
         */
        @Test
        @DisplayName("Sans Livret A ET avec un montant invalide, c'est l'absence de livret qui est signalée en premier")
        void ordreDeValidationDuCreditLivretA() {
            assertThrows(LivretAAbsentException.class,
                    () -> client.crediterLivretA(new BigDecimal("-100.00")));
        }

        /** L'inversion ne doit pas avoir masqué la validation du montant. */
        @Test
        @DisplayName("Avec un Livret A ouvert, un montant invalide est toujours signalé comme tel")
        void montantInvalideSurLivretOuvertSignaleToujoursLeMontant() {
            client.ouvrirLivretA();

            assertThrows(MontantInvalideException.class,
                    () -> client.crediterLivretA(new BigDecimal("-100.00")));
        }

        /** Symétrique du précédent : la règle d'existence tient toujours seule. */
        @Test
        @DisplayName("Sans Livret A mais avec un montant valide, c'est toujours l'absence de livret qui est signalée")
        void montantValideSansLivretSignaleToujoursLAbsence() {
            assertThrows(LivretAAbsentException.class,
                    () -> client.crediterLivretA(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Le crédit du Livret A ne touche pas au compte courant")
        void creditDuLivretNeTouchePasAuCompte() {
            client.crediter(new BigDecimal("500.00"));
            client.ouvrirLivretA();

            client.crediterLivretA(new BigDecimal("100.00"));

            assertEquals(0, new BigDecimal("500.00").compareTo(client.getSoldeCompte()));
        }
    }

    @Nested
    @DisplayName("Historique")
    class Historique {

        @Test
        @DisplayName("Une transaction ajoutée apparaît dans l'historique")
        void transactionAjouteeVisible() {
            client.ajouterTransaction(new Transaction(new BigDecimal("100.00"),
                    Transaction.TypeTransaction.DEPOT, "Test"));

            assertEquals(1, client.getHistorique().size());
        }

        @Test
        @DisplayName("L'historique conserve l'ordre d'ajout")
        void ordreConserve() {
            client.ajouterTransaction(new Transaction(new BigDecimal("1.00"),
                    Transaction.TypeTransaction.DEPOT, "première"));
            client.ajouterTransaction(new Transaction(new BigDecimal("2.00"),
                    Transaction.TypeTransaction.RETRAIT, "seconde"));

            List<Transaction> historique = client.getHistorique();
            assertEquals("première", historique.get(0).getDescription());
            assertEquals("seconde", historique.get(1).getDescription());
        }

        @Test
        @DisplayName("L'historique exposé n'est pas modifiable de l'extérieur")
        void historiqueNonModifiable() {
            List<Transaction> historique = client.getHistorique();

            assertThrows(UnsupportedOperationException.class, () -> historique.clear());
        }

        // TODO Phase A : comportement à vérifier — créditer ou débiter ne produit
        // AUCUNE ligne d'historique. La traçabilité repose entièrement sur
        // BanqueService, qui crée la Transaction séparément : appeler le modèle
        // directement laisse donc un mouvement d'argent sans trace.
        @Test
        @DisplayName("Un crédit fait directement sur le modèle ne laisse aucune trace dans l'historique")
        void mouvementDirectSansTrace() {
            client.crediter(new BigDecimal("500.00"));

            assertTrue(client.getHistorique().isEmpty());
        }
    }

    @Nested
    @DisplayName("Les comptes de l'offre")
    class Comptes {

        @Test
        @DisplayName("Un client neuf a exactement un compte : son compte courant")
        void unSeulCompteALOuverture() {
            assertEquals(1, client.getComptes().size());
            assertFalse(client.getCompteCourant().estEpargne());
        }

        @Test
        @DisplayName("Le compte courant vient de l'offre souscrite")
        void compteCourantVientDeLOffre() {
            Client etudiant = new Client("E", "x", 1, new OffreEtudianteFactory());

            assertEquals(0, new BigDecimal("0.00")
                    .compareTo(etudiant.getCompteCourant().getDecouvertAutorise()));
        }

        @Test
        @DisplayName("Ouvrir un Livret A ajoute un second compte, d'épargne celui-là")
        void ouvertureAjouteUnCompteDEpargne() {
            client.ouvrirLivretA();

            assertEquals(2, client.getComptes().size());
            assertTrue(client.getLivretA().isPresent());
            assertTrue(client.getLivretA().get().estEpargne());
        }

        @Test
        @DisplayName("Sans Livret A, la recherche d'épargne ne rend rien plutôt que null")
        void pasDEpargneAvantOuverture() {
            assertTrue(client.getLivretA().isEmpty());
        }

        @Test
        @DisplayName("La liste des comptes exposée n'est pas modifiable de l'extérieur")
        void listeDeComptesNonModifiable() {
            List<Compte> comptes = client.getComptes();

            assertThrows(UnsupportedOperationException.class, () -> comptes.clear());
        }

        @Test
        @DisplayName("Le solde total d'un client sans épargne est celui de son compte courant")
        void soldeTotalSansEpargne() {
            client.crediter(new BigDecimal("500.00"));

            assertEquals(0, new BigDecimal("500.00").compareTo(client.soldeTotal()));
        }

        @Test
        @DisplayName("Le solde total somme le compte courant et le Livret A")
        void soldeTotalAvecEpargne() {
            client.crediter(new BigDecimal("500.00"));
            client.ouvrirLivretA();
            client.crediterLivretA(new BigDecimal("250.00"));

            assertEquals(0, new BigDecimal("750.00").compareTo(client.soldeTotal()));
        }

        @Test
        @DisplayName("Le solde total tient compte d'un compte courant en découvert")
        void soldeTotalAvecDecouvert() {
            client.ouvrirLivretA();
            client.crediterLivretA(new BigDecimal("100.00"));
            client.debiter(new BigDecimal("40.00"));

            assertEquals(0, new BigDecimal("60.00").compareTo(client.soldeTotal()));
        }

        @Test
        @DisplayName("Le solde total d'un client neuf est nul")
        void soldeTotalNeuf() {
            assertEquals(0, new BigDecimal("0.00").compareTo(client.soldeTotal()));
        }
    }

    /**
     * LE CŒUR DE LA PHASE B — le découvert n'est plus une règle du modèle
     * Client, c'est celle du Compte de son offre. Ces tests le vérifient à
     * travers Client, et non sur un Compte isolé : c'est la preuve que le
     * package offre est réellement branché.
     */
    @Nested
    @DisplayName("Découvert selon l'offre souscrite")
    class DecouvertSelonLOffre {

        @Test
        @DisplayName("TIER Étudiante — aucun découvert : un centime au-delà du solde est refusé")
        void offreEtudianteSansDecouvert() {
            Client etudiant = new Client("E", "x", 1, new OffreEtudianteFactory());
            etudiant.crediter(new BigDecimal("100.00"));

            assertThrows(SoldeInsuffisantException.class,
                    () -> etudiant.debiter(new BigDecimal("100.01")));
        }

        @Test
        @DisplayName("TIER Standard — le solde peut descendre jusqu'à -300,00 € exactement")
        void offreStandardJusquAMoins300() {
            Client standard = new Client("S", "x", 2, new OffreStandardFactory());

            standard.debiter(new BigDecimal("300.00"));

            assertEquals(0, new BigDecimal("-300.00").compareTo(standard.getSoldeCompte()));
        }

        @Test
        @DisplayName("TIER Standard — un centime au-delà des 300 € de découvert est refusé")
        void offreStandardRefuseAuDelaDe300() {
            Client standard = new Client("S", "x", 2, new OffreStandardFactory());

            assertThrows(SoldeInsuffisantException.class,
                    () -> standard.debiter(new BigDecimal("300.01")));
        }

        @Test
        @DisplayName("TIER Premium — le solde peut descendre jusqu'à -2000,00 € exactement")
        void offrePremiumJusquAMoins2000() {
            Client premium = new Client("P", "x", 3, new OffrePremiumFactory());

            premium.debiter(new BigDecimal("2000.00"));

            assertEquals(0, new BigDecimal("-2000.00").compareTo(premium.getSoldeCompte()));
        }

        @Test
        @DisplayName("TIER Premium — un centime au-delà des 2000 € de découvert est refusé")
        void offrePremiumRefuseAuDelaDe2000() {
            Client premium = new Client("P", "x", 3, new OffrePremiumFactory());

            assertThrows(SoldeInsuffisantException.class,
                    () -> premium.debiter(new BigDecimal("2000.01")));
        }

        @Test
        @DisplayName("HIÉRARCHIE — le même retrait de 1000 € est refusé en Standard et accepté en Premium")
        void memeRetraitArbitreParLOffre() {
            Client standard = new Client("S", "x", 2, new OffreStandardFactory());
            Client premium = new Client("P", "x", 3, new OffrePremiumFactory());

            assertThrows(SoldeInsuffisantException.class,
                    () -> standard.debiter(new BigDecimal("1000.00")));
            premium.debiter(new BigDecimal("1000.00"));

            assertEquals(0, new BigDecimal("-1000.00").compareTo(premium.getSoldeCompte()));
        }

        @Test
        @DisplayName("Passer en négatif fait basculer le compte courant dans l'état EnDécouvert")
        void passageEnDecouvertChangeLEtat() {
            client.debiter(new BigDecimal("50.00"));

            assertInstanceOf(CompteEnDecouvert.class, client.getCompteCourant().getEtat());
        }

        @Test
        @DisplayName("Revenir à un solde positif fait ressortir le compte de l'état EnDécouvert")
        void retourEnPositifRetablitLEtatActif() {
            client.debiter(new BigDecimal("50.00"));

            client.crediter(new BigDecimal("50.00"));

            assertInstanceOf(CompteActif.class, client.getCompteCourant().getEtat());
        }

        @Test
        @DisplayName("Un débit refusé pour découvert dépassé laisse le solde strictement intact")
        void debitRefusePourDecouvertLaisseLeSoldeIntact() {
            client.crediter(new BigDecimal("100.00"));

            assertThrows(SoldeInsuffisantException.class,
                    () -> client.debiter(new BigDecimal("400.01")));

            assertEquals(0, new BigDecimal("100.00").compareTo(client.getSoldeCompte()));
        }

        @Test
        @DisplayName("Le Livret A n'a aucun découvert, quelle que soit l'offre du client")
        void livretANAJamaisDeDecouvert() {
            Client premium = new Client("P", "x", 3, new OffrePremiumFactory());
            premium.ouvrirLivretA();
            Compte livret = premium.getLivretA().orElseThrow();

            assertEquals(0, new BigDecimal("0.00").compareTo(livret.getDecouvertAutorise()));
            assertThrows(SoldeInsuffisantException.class,
                    () -> livret.debiter(new BigDecimal("0.01")));
        }
    }
}
