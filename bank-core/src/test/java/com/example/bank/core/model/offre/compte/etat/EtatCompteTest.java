package com.example.bank.core.model.offre.compte.etat;

import com.example.bank.core.exception.etat.OperationInterditeException;
import com.example.bank.core.exception.etat.SoldeInsuffisantException;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.concret.CompteEtudiant;
import com.example.bank.core.model.offre.compte.concret.CompteStandard;
import com.example.bank.core.model.offre.compte.etat.concret.CompteActif;
import com.example.bank.core.model.offre.compte.etat.concret.CompteBloque;
import com.example.bank.core.model.offre.compte.etat.concret.CompteEnDecouvert;
import com.example.bank.core.model.offre.compte.etat.concret.CompteFerme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Machine à états du compte : Actif -> EnDécouvert -> Bloqué -> Fermé.
 *
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("300" != "300.00").
 *
 * Le tier Standard sert de support (300 € de découvert) : c'est le seul qui
 * permet d'observer le passage en découvert ET son refus au-delà du plafond.
 */
@DisplayName("État d'un compte")
class EtatCompteTest {

    private Compte compte;

    @BeforeEach
    void creerCompte() {
        compte = new CompteStandard();
    }

    @Nested
    @DisplayName("Transitions automatiques Actif <-> EnDécouvert")
    class TransitionsAutomatiques {

        @Test
        @DisplayName("Un compte neuf est actif")
        void compteNeufEstActif() {
            assertInstanceOf(CompteActif.class, compte.getEtat());
        }

        @Test
        @DisplayName("Un solde qui passe sous zéro bascule le compte en découvert")
        void soldeNegatifBasculeEnDecouvert() {
            compte.debiter(new BigDecimal("100.00"));

            assertInstanceOf(CompteEnDecouvert.class, compte.getEtat());
        }

        @Test
        @DisplayName("Un solde qui reste positif laisse le compte actif")
        void soldePositifResteActif() {
            compte.crediter(new BigDecimal("500.00"));
            compte.debiter(new BigDecimal("200.00"));

            assertInstanceOf(CompteActif.class, compte.getEtat());
        }

        @Test
        @DisplayName("Un retour à zéro exactement fait sortir du découvert")
        void retourAZeroSortDuDecouvert() {
            compte.debiter(new BigDecimal("100.00"));
            compte.crediter(new BigDecimal("100.00"));

            assertEquals(0, new BigDecimal("0.00").compareTo(compte.getSolde()));
            assertInstanceOf(CompteActif.class, compte.getEtat());
        }

        @Test
        @DisplayName("Un crédit partiel laisse le compte en découvert tant que le solde est négatif")
        void creditPartielResteEnDecouvert() {
            compte.debiter(new BigDecimal("100.00"));
            compte.crediter(new BigDecimal("40.00"));

            assertInstanceOf(CompteEnDecouvert.class, compte.getEtat());
        }

        @Test
        @DisplayName("TIER — un compte Étudiant ne passe jamais en découvert, son plafond est nul")
        void compteEtudiantNePasseJamaisEnDecouvert() {
            Compte etudiant = new CompteEtudiant();

            assertThrows(SoldeInsuffisantException.class,
                    () -> etudiant.debiter(new BigDecimal("0.01")));
            assertInstanceOf(CompteActif.class, etudiant.getEtat());
        }
    }

    @Nested
    @DisplayName("En découvert")
    class EnDecouvert {

        @BeforeEach
        void passerEnDecouvert() {
            compte.debiter(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Un débit qui reste dans le plafond de découvert est accepté")
        void debitDansLePlafondAccepte() {
            compte.debiter(new BigDecimal("200.00"));

            assertEquals(0, new BigDecimal("-300.00").compareTo(compte.getSolde()));
        }

        @Test
        @DisplayName("Un débit qui aggraverait le découvert au-delà du plafond est refusé")
        void debitAuDelaDuPlafondRefuse() {
            assertThrows(SoldeInsuffisantException.class,
                    () -> compte.debiter(new BigDecimal("200.01")));
        }

        @Test
        @DisplayName("Un débit refusé laisse le solde ET l'état inchangés")
        void debitRefuseNeChangeRien() {
            assertThrows(SoldeInsuffisantException.class,
                    () -> compte.debiter(new BigDecimal("10000.00")));

            assertEquals(0, new BigDecimal("-100.00").compareTo(compte.getSolde()));
            assertInstanceOf(CompteEnDecouvert.class, compte.getEtat());
        }

        @Test
        @DisplayName("Le compte en découvert autorise toujours les opérations")
        void decouvertAutoriseLesOperations() {
            assertTrue(compte.getEtat().autoriseOperations());
        }
    }

    @Nested
    @DisplayName("Bloqué")
    class Bloque {

        @BeforeEach
        void bloquerLeCompte() {
            compte.crediter(new BigDecimal("500.00"));
            compte.bloquer();
        }

        @Test
        @DisplayName("Le blocage change l'état du compte")
        void blocageChangeLEtat() {
            assertInstanceOf(CompteBloque.class, compte.getEtat());
        }

        @Test
        @DisplayName("Un compte bloqué n'autorise plus les opérations")
        void bloqueNAutorisePlusLesOperations() {
            assertFalse(compte.getEtat().autoriseOperations());
        }

        @Test
        @DisplayName("Un compte bloqué refuse tout débit")
        void bloqueRefuseLeDebit() {
            assertThrows(OperationInterditeException.class,
                    () -> compte.debiter(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Un compte bloqué refuse aussi le crédit ordinaire")
        void bloqueRefuseLeCreditOrdinaire() {
            assertThrows(OperationInterditeException.class,
                    () -> compte.crediter(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("CHOIX — un compte bloqué accepte le crédit de régularisation de la banque")
        void bloqueAccepteLaRegularisation() {
            compte.crediterRegularisation(new BigDecimal("100.00"));

            assertEquals(0, new BigDecimal("600.00").compareTo(compte.getSolde()));
        }

        @Test
        @DisplayName("CHOIX — la régularisation ne débloque pas le compte, elle le renfloue")
        void regularisationNeDebloquePas() {
            compte.crediterRegularisation(new BigDecimal("100.00"));

            assertInstanceOf(CompteBloque.class, compte.getEtat());
        }

        @Test
        @DisplayName("Le déblocage rend un compte au solde positif à l'état actif")
        void deblocageRendActif() {
            compte.debloquer();

            assertInstanceOf(CompteActif.class, compte.getEtat());
        }

        @Test
        @DisplayName("Le déblocage d'un compte au solde négatif le rend à l'état EnDécouvert")
        void deblocageAvecSoldeNegatifRendEnDecouvert() {
            Compte autre = new CompteStandard();
            autre.debiter(new BigDecimal("50.00"));
            autre.bloquer();

            autre.debloquer();

            assertInstanceOf(CompteEnDecouvert.class, autre.getEtat());
        }

        @Test
        @DisplayName("Un compte actif ne peut pas être débloqué : il ne l'est pas")
        void deblocageDUnCompteActifRefuse() {
            Compte actif = new CompteStandard();

            assertThrows(OperationInterditeException.class, actif::debloquer);
        }

        @Test
        @DisplayName("Un compte déjà bloqué ne peut pas être bloqué une seconde fois")
        void doubleBlocageRefuse() {
            assertThrows(OperationInterditeException.class, () -> compte.bloquer());
        }
    }

    @Nested
    @DisplayName("Fermé — état terminal")
    class Ferme {

        @BeforeEach
        void fermerLeCompte() {
            compte.crediter(new BigDecimal("500.00"));
            compte.fermer();
        }

        @Test
        @DisplayName("La fermeture change l'état du compte")
        void fermetureChangeLEtat() {
            assertInstanceOf(CompteFerme.class, compte.getEtat());
        }

        @Test
        @DisplayName("Un compte fermé refuse tout dépôt")
        void fermeRefuseLeCredit() {
            assertThrows(OperationInterditeException.class,
                    () -> compte.crediter(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Un compte fermé refuse tout retrait")
        void fermeRefuseLeDebit() {
            assertThrows(OperationInterditeException.class,
                    () -> compte.debiter(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Un compte fermé refuse même une régularisation")
        void fermeRefuseLaRegularisation() {
            assertThrows(OperationInterditeException.class,
                    () -> compte.crediterRegularisation(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Un compte fermé ne se rouvre pas")
        void fermeNeSeRouvrePas() {
            assertThrows(OperationInterditeException.class, () -> compte.debloquer());
        }

        @Test
        @DisplayName("Un compte fermé ne peut pas être bloqué")
        void fermeNeSeBloquePas() {
            assertThrows(OperationInterditeException.class, () -> compte.fermer());
        }

        @Test
        @DisplayName("Un compte fermé conserve son solde : la fermeture n'efface rien")
        void fermeConserveSonSolde() {
            assertEquals(0, new BigDecimal("500.00").compareTo(compte.getSolde()));
        }

        @Test
        @DisplayName("Un compte bloqué peut être fermé")
        void bloquePeutEtreFerme() {
            Compte autre = new CompteStandard();
            autre.bloquer();

            autre.fermer();

            assertInstanceOf(CompteFerme.class, autre.getEtat());
        }
    }
}
