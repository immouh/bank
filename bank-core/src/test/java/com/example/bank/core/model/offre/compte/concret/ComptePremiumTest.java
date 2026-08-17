package com.example.bank.core.model.offre.compte.concret;

import com.example.bank.core.exception.MontantInvalideException;
import com.example.bank.core.exception.SoldeInsuffisantException;
import com.example.bank.core.model.offre.compte.Compte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("60" != "60.00").
 */
@DisplayName("Compte de l'offre Premium")
class ComptePremiumTest {

    /** Découvert autorisé du tier Premium. */
    private static final BigDecimal DECOUVERT = new BigDecimal("2000.00");

    private Compte compte;

    @BeforeEach
    void creerCompte() {
        compte = new ComptePremium();
    }

    @Test
    @DisplayName("Un compte neuf démarre à zéro")
    void compteNeufAZero() {
        assertEquals(0, new BigDecimal("0.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Un crédit augmente le solde du montant exact")
    void crediterAugmenteLeSolde() {
        compte.crediter(new BigDecimal("1000.00"));

        assertEquals(0, new BigDecimal("1000.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Un crédit de zéro lève MontantInvalideException")
    void crediterZeroEstRefuse() {
        assertThrows(MontantInvalideException.class,
                () -> compte.crediter(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Un crédit négatif lève MontantInvalideException")
    void crediterNegatifEstRefuse() {
        assertThrows(MontantInvalideException.class,
                () -> compte.crediter(new BigDecimal("-50.00")));
    }

    @Test
    @DisplayName("Un débit inférieur au solde diminue le solde du montant exact")
    void debiterDiminueLeSolde() {
        compte.crediter(new BigDecimal("1000.00"));

        compte.debiter(new BigDecimal("400.00"));

        assertEquals(0, new BigDecimal("600.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Un débit négatif lève MontantInvalideException")
    void debiterNegatifEstRefuse() {
        compte.crediter(new BigDecimal("1000.00"));

        assertThrows(MontantInvalideException.class,
                () -> compte.debiter(new BigDecimal("-10.00")));
    }

    @Test
    @DisplayName("TIER — un débit peut rendre le solde négatif dans la limite des 2000 € de découvert")
    void decouvertLargeAutoriseParLeTier() {
        compte.crediter(new BigDecimal("100.00"));

        compte.debiter(new BigDecimal("1500.00"));

        assertEquals(0, new BigDecimal("-1400.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("TIER — un débit qui amène exactement à -2000 € est accepté")
    void debitJusquALaLimiteExacteDuDecouvert() {
        compte.crediter(new BigDecimal("100.00"));

        compte.debiter(new BigDecimal("2100.00"));

        assertEquals(0, new BigDecimal("-2000.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("TIER — même le découvert Premium est plafonné : au-delà de 2000 € le débit est refusé")
    void debitAuDelaDuDecouvertEstRefuse() {
        compte.crediter(new BigDecimal("100.00"));

        assertThrows(SoldeInsuffisantException.class,
                () -> compte.debiter(new BigDecimal("2100.01")));
    }

    @Test
    @DisplayName("Un débit refusé laisse le solde inchangé")
    void debitRefuseNAlterePasLeSolde() {
        compte.crediter(new BigDecimal("100.00"));

        assertThrows(SoldeInsuffisantException.class,
                () -> compte.debiter(new BigDecimal("5000.00")));

        assertEquals(0, new BigDecimal("100.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Le découvert autorisé du tier Premium est de 2000 €")
    void decouvertAutoriseDuTier() {
        assertEquals(0, DECOUVERT.compareTo(new ComptePremium().getDecouvertAutorise()));
    }

    @Test
    @DisplayName("Les frais de tenue du tier Premium sont de 12 € par mois")
    void fraisDeTenueDuTier() {
        assertEquals(0, new BigDecimal("12.00")
                .compareTo(new ComptePremium().getFraisTenueMensuels()));
    }
}
