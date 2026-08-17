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
 * où t = 0.0320 / 12 (taux mensuel) et n = 240 mensualités,
 * arrondi au centime en HALF_EVEN.
 */
@DisplayName("Prêt de l'offre Premium")
class PretImmobilierTest {

    @Test
    @DisplayName("Le taux nominal annuel du tier est de 3,20 %")
    void tauxDuTier() {
        assertEquals(0, new BigDecimal("0.0320").compareTo(new PretImmobilier().getTaux()));
    }

    @Test
    @DisplayName("La durée de remboursement du tier est de 240 mensualités")
    void dureeDuTier() {
        assertEquals(240, new PretImmobilier().getDureeMois());
    }

    @Test
    @DisplayName("Sans montant précisé, le prêt reprend le montant type du tier (200000.00 €)")
    void montantParDefautDuTier() {
        assertEquals(0, new BigDecimal("200000.00").compareTo(new PretImmobilier().getMontantEmprunte()));
    }

    @Test
    @DisplayName("Le montant emprunté est celui passé au constructeur")
    void montantEmprunteEstCeluiDemande() {
        PretImmobilier pret = new PretImmobilier(new BigDecimal("150000.00"));

        assertEquals(0, new BigDecimal("150000.00").compareTo(pret.getMontantEmprunte()));
    }

    @Test
    @DisplayName("200000.00 € à 3,20 % sur 240 mois donnent une mensualité de 1129.33 €")
    void mensualitePourLeMontantParDefaut() {
        PretImmobilier pret = new PretImmobilier(new BigDecimal("200000.00"));

        assertEquals(0, new BigDecimal("1129.33").compareTo(pret.calculerMensualite()));
    }

    @Test
    @DisplayName("150000.00 € à 3,20 % sur 240 mois donnent une mensualité de 846.99 €")
    void mensualitePourUnAutreMontant() {
        PretImmobilier pret = new PretImmobilier(new BigDecimal("150000.00"));

        assertEquals(0, new BigDecimal("846.99").compareTo(pret.calculerMensualite()));
    }

    @Test
    @DisplayName("La mensualité est arrondie au centime")
    void mensualiteArrondieAuCentime() {
        assertEquals(2, new PretImmobilier().calculerMensualite().scale());
    }

    @Test
    @DisplayName("Le total remboursé dépasse le capital emprunté")
    void totalRembourseSuperieurAuCapital() {
        PretImmobilier pret = new PretImmobilier();
        BigDecimal total = pret.calculerMensualite().multiply(new BigDecimal(pret.getDureeMois()));

        assertEquals(1, total.compareTo(pret.getMontantEmprunte()));
    }

    @Test
    @DisplayName("Un montant emprunté nul est refusé")
    void montantNulRefuse() {
        assertThrows(MontantInvalideException.class, () -> new PretImmobilier(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Un montant emprunté négatif est refusé")
    void montantNegatifRefuse() {
        assertThrows(MontantInvalideException.class,
                () -> new PretImmobilier(new BigDecimal("-1000.00")));
    }
}
