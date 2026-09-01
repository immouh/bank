package com.example.bank.core.model.offre.pret.etat;

import com.example.bank.core.exception.OperationInterditeException;
import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.pret.concret.PretPersonnel;
import com.example.bank.core.model.offre.pret.etat.concret.PretApprouve;
import com.example.bank.core.model.offre.pret.etat.concret.PretEnAttente;
import com.example.bank.core.model.offre.pret.etat.concret.PretEnDefaut;
import com.example.bank.core.model.offre.pret.etat.concret.PretEnRemboursement;
import com.example.bank.core.model.offre.pret.etat.concret.PretSolde;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Machine à états du prêt :
 * EnAttente -> Approuvé -> EnRemboursement -> Soldé | EnDéfaut.
 *
 * MENSUALITÉ ATTENDUE — 342,05 € pour 15 000 € à 4,50 % sur 48 mois, valeur
 * déjà calculée indépendamment du code dans {@code PretPersonnelTest}. Elle
 * n'est reprise ici que pour vérifier le FILTRAGE par l'état, pas la formule.
 */
@DisplayName("État d'un prêt")
class EtatPretTest {

    /** 15 000 € à 4,50 % sur 48 mois. */
    private static final BigDecimal MENSUALITE_ATTENDUE = new BigDecimal("342.05");

    private Pret pret;

    @BeforeEach
    void creerPret() {
        pret = new PretPersonnel(new BigDecimal("15000.00"));
    }

    @Nested
    @DisplayName("En attente")
    class EnAttente {

        @Test
        @DisplayName("Un prêt neuf est en attente d'instruction")
        void pretNeufEstEnAttente() {
            assertInstanceOf(PretEnAttente.class, pret.getEtat());
        }

        @Test
        @DisplayName("Aucune mensualité n'est exigible tant que le prêt n'est pas accordé")
        void aucuneMensualiteExigibleEnAttente() {
            assertThrows(OperationInterditeException.class, () -> pret.mensualiteExigible());
        }

        @Test
        @DisplayName("CHOIX — le barème reste simulable en attente : c'est l'objet d'une demande de prêt")
        void baremeSimulableEnAttente() {
            assertEquals(0, MENSUALITE_ATTENDUE.compareTo(pret.calculerMensualite()));
        }

        @Test
        @DisplayName("Un prêt en attente ne peut pas passer directement en remboursement")
        void pasDeRemboursementSansApprobation() {
            assertThrows(OperationInterditeException.class, () -> pret.demarrerRemboursement());
        }

        @Test
        @DisplayName("Un prêt en attente ne peut pas être soldé")
        void pasDeSoldeSansApprobation() {
            assertThrows(OperationInterditeException.class, () -> pret.solder());
        }

        @Test
        @DisplayName("Un prêt en attente ne peut pas être déclaré en défaut")
        void pasDeDefautSansApprobation() {
            assertThrows(OperationInterditeException.class, () -> pret.declarerDefaut());
        }
    }

    @Nested
    @DisplayName("Approuvé")
    class Approuve {

        @BeforeEach
        void approuver() {
            pret.approuver();
        }

        @Test
        @DisplayName("L'approbation fait passer le prêt à l'état Approuvé")
        void approbationChangeLEtat() {
            assertInstanceOf(PretApprouve.class, pret.getEtat());
        }

        @Test
        @DisplayName("La mensualité devient exigible dès l'approbation")
        void mensualiteExigibleDesLApprobation() {
            assertEquals(0, MENSUALITE_ATTENDUE.compareTo(pret.mensualiteExigible()));
        }

        @Test
        @DisplayName("Un prêt déjà approuvé ne se réapprouve pas")
        void doubleApprobationRefusee() {
            assertThrows(OperationInterditeException.class, () -> pret.approuver());
        }

        @Test
        @DisplayName("Un prêt approuvé mais non débloqué ne peut pas être soldé")
        void pasDeSoldeAvantRemboursement() {
            assertThrows(OperationInterditeException.class, () -> pret.solder());
        }

        @Test
        @DisplayName("Un prêt approuvé mais non débloqué ne peut pas être en défaut")
        void pasDeDefautAvantRemboursement() {
            assertThrows(OperationInterditeException.class, () -> pret.declarerDefaut());
        }
    }

    @Nested
    @DisplayName("En remboursement")
    class EnRemboursement {

        @BeforeEach
        void demarrerLeRemboursement() {
            pret.approuver();
            pret.demarrerRemboursement();
        }

        @Test
        @DisplayName("Le déblocage des fonds fait passer le prêt en remboursement")
        void deblocageChangeLEtat() {
            assertInstanceOf(PretEnRemboursement.class, pret.getEtat());
        }

        @Test
        @DisplayName("La mensualité reste exigible pendant le remboursement")
        void mensualiteExigiblePendantLeRemboursement() {
            assertEquals(0, MENSUALITE_ATTENDUE.compareTo(pret.mensualiteExigible()));
        }

        @Test
        @DisplayName("Le prêt peut être soldé")
        void peutEtreSolde() {
            pret.solder();

            assertInstanceOf(PretSolde.class, pret.getEtat());
        }

        @Test
        @DisplayName("CHOIX — le défaut se déclare explicitement, il n'est jamais détecté tout seul")
        void peutEtreDeclareEnDefaut() {
            pret.declarerDefaut();

            assertInstanceOf(PretEnDefaut.class, pret.getEtat());
        }
    }

    @Nested
    @DisplayName("Soldé — état terminal")
    class Solde {

        @BeforeEach
        void solderLePret() {
            pret.approuver();
            pret.demarrerRemboursement();
            pret.solder();
        }

        @Test
        @DisplayName("Plus aucune mensualité n'est exigible sur un prêt soldé")
        void plusDeMensualiteExigible() {
            assertThrows(OperationInterditeException.class, () -> pret.mensualiteExigible());
        }

        @Test
        @DisplayName("Un prêt soldé ne repart pas en remboursement")
        void pasDeRetourEnRemboursement() {
            assertThrows(OperationInterditeException.class, () -> pret.demarrerRemboursement());
        }

        @Test
        @DisplayName("Un prêt soldé ne peut plus tomber en défaut")
        void pasDeDefautApresSolde() {
            assertThrows(OperationInterditeException.class, () -> pret.declarerDefaut());
        }
    }

    @Nested
    @DisplayName("En défaut — état terminal")
    class EnDefaut {

        @BeforeEach
        void mettreEnDefaut() {
            pret.approuver();
            pret.demarrerRemboursement();
            pret.declarerDefaut();
        }

        @Test
        @DisplayName("Plus aucune mensualité n'est exigible au titre du contrat initial")
        void plusDeMensualiteExigible() {
            assertThrows(OperationInterditeException.class, () -> pret.mensualiteExigible());
        }

        @Test
        @DisplayName("Un prêt en défaut ne peut pas être soldé sans repasser par un traitement")
        void pasDeSoldeDepuisLeDefaut() {
            assertThrows(OperationInterditeException.class, () -> pret.solder());
        }

        @Test
        @DisplayName("Le montant emprunté reste connu : le défaut n'efface pas la créance")
        void montantEmprunteConserve() {
            assertEquals(0, new BigDecimal("15000.00").compareTo(pret.getMontantEmprunte()));
        }
    }
}
