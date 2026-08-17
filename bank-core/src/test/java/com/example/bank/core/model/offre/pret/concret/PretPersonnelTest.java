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
 * où t = 0.0450 / 12 (taux mensuel) et n = 48 mensualités,
 * arrondi au centime en HALF_EVEN.
 */
@DisplayName("Prêt de l'offre Standard")
class PretPersonnelTest {

    @Test
    @DisplayName("Le taux nominal annuel du tier est de 4,50 %")
    void tauxDuTier() {
        assertEquals(0, new BigDecimal("0.0450").compareTo(new PretPersonnel().getTaux()));
    }

    @Test
    @DisplayName("La durée de remboursement du tier est de 48 mensualités")
    void dureeDuTier() {
        assertEquals(48, new PretPersonnel().getDureeMois());
    }

    @Test
    @DisplayName("Sans montant précisé, le prêt reprend le montant type du tier (15000.00 €)")
    void montantParDefautDuTier() {
        assertEquals(0, new BigDecimal("15000.00").compareTo(new PretPersonnel().getMontantEmprunte()));
    }

    @Test
    @DisplayName("Le montant emprunté est celui passé au constructeur")
    void montantEmprunteEstCeluiDemande() {
        PretPersonnel pret = new PretPersonnel(new BigDecimal("20000.00"));

        assertEquals(0, new BigDecimal("20000.00").compareTo(pret.getMontantEmprunte()));
    }

    @Test
    @DisplayName("15000.00 € à 4,50 % sur 48 mois donnent une mensualité de 342.05 €")
    void mensualitePourLeMontantParDefaut() {
        PretPersonnel pret = new PretPersonnel(new BigDecimal("15000.00"));

        assertEquals(0, new BigDecimal("342.05").compareTo(pret.calculerMensualite()));
    }

    @Test
    @DisplayName("20000.00 € à 4,50 % sur 48 mois donnent une mensualité de 456.07 €")
    void mensualitePourUnAutreMontant() {
        PretPersonnel pret = new PretPersonnel(new BigDecimal("20000.00"));

        assertEquals(0, new BigDecimal("456.07").compareTo(pret.calculerMensualite()));
    }

    @Test
    @DisplayName("La mensualité est arrondie au centime")
    void mensualiteArrondieAuCentime() {
        assertEquals(2, new PretPersonnel().calculerMensualite().scale());
    }

    @Test
    @DisplayName("Le total remboursé dépasse le capital emprunté")
    void totalRembourseSuperieurAuCapital() {
        PretPersonnel pret = new PretPersonnel();
        BigDecimal total = pret.calculerMensualite().multiply(new BigDecimal(pret.getDureeMois()));

        assertEquals(1, total.compareTo(pret.getMontantEmprunte()));
    }

    @Test
    @DisplayName("Un montant emprunté nul est refusé")
    void montantNulRefuse() {
        assertThrows(MontantInvalideException.class, () -> new PretPersonnel(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Un montant emprunté négatif est refusé")
    void montantNegatifRefuse() {
        assertThrows(MontantInvalideException.class,
                () -> new PretPersonnel(new BigDecimal("-1000.00")));
    }
}
