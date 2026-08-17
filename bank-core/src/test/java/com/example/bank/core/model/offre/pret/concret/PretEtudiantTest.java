package com.example.bank.core.model.offre.pret.concret;

import com.example.bank.core.exception.MontantInvalideException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("170.51" != "170.510").
 *
 * MENSUALITÉS ATTENDUES — calculées indépendamment du code testé, avec la
 * formule d'amortissement constant :
 *
 * <pre>M = C x t x (1+t)^n / ((1+t)^n - 1)</pre>
 *
 * où t = 0.0090 / 12 (taux mensuel) et n = 60 mensualités,
 * arrondi au centime en HALF_EVEN.
 */
@DisplayName("Prêt de l'offre Étudiante")
class PretEtudiantTest {

    @Test
    @DisplayName("Le taux nominal annuel du tier est de 0,90 %")
    void tauxDuTier() {
        assertEquals(0, new BigDecimal("0.0090").compareTo(new PretEtudiant().getTaux()));
    }

    @Test
    @DisplayName("La durée de remboursement du tier est de 60 mensualités")
    void dureeDuTier() {
        assertEquals(60, new PretEtudiant().getDureeMois());
    }

    @Test
    @DisplayName("Sans montant précisé, le prêt reprend le montant type du tier (10000.00 €)")
    void montantParDefautDuTier() {
        assertEquals(0, new BigDecimal("10000.00").compareTo(new PretEtudiant().getMontantEmprunte()));
    }

    @Test
    @DisplayName("Le montant emprunté est celui passé au constructeur")
    void montantEmprunteEstCeluiDemande() {
        PretEtudiant pret = new PretEtudiant(new BigDecimal("5000.00"));

        assertEquals(0, new BigDecimal("5000.00").compareTo(pret.getMontantEmprunte()));
    }

    @Test
    @DisplayName("10000.00 € à 0,90 % sur 60 mois donnent une mensualité de 170.51 €")
    void mensualitePourLeMontantParDefaut() {
        PretEtudiant pret = new PretEtudiant(new BigDecimal("10000.00"));

        assertEquals(0, new BigDecimal("170.51").compareTo(pret.calculerMensualite()));
    }

    @Test
    @DisplayName("5000.00 € à 0,90 % sur 60 mois donnent une mensualité de 85.25 €")
    void mensualitePourUnAutreMontant() {
        PretEtudiant pret = new PretEtudiant(new BigDecimal("5000.00"));

        assertEquals(0, new BigDecimal("85.25").compareTo(pret.calculerMensualite()));
    }

    @Test
    @DisplayName("La mensualité est arrondie au centime")
    void mensualiteArrondieAuCentime() {
        assertEquals(2, new PretEtudiant().calculerMensualite().scale());
    }

    @Test
    @DisplayName("Le total remboursé dépasse le capital emprunté")
    void totalRembourseSuperieurAuCapital() {
        PretEtudiant pret = new PretEtudiant();
        BigDecimal total = pret.calculerMensualite().multiply(new BigDecimal(pret.getDureeMois()));

        assertEquals(1, total.compareTo(pret.getMontantEmprunte()));
    }

    @Test
    @DisplayName("Un montant emprunté nul est refusé")
    void montantNulRefuse() {
        assertThrows(MontantInvalideException.class, () -> new PretEtudiant(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Un montant emprunté négatif est refusé")
    void montantNegatifRefuse() {
        assertThrows(MontantInvalideException.class,
                () -> new PretEtudiant(new BigDecimal("-1000.00")));
    }
}
