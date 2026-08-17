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
@DisplayName("Compte de l'offre Standard")
class CompteStandardTest {

    /** Découvert autorisé du tier Standard. */
    private static final BigDecimal DECOUVERT = new BigDecimal("300.00");

    private Compte compte;

    @BeforeEach
    void creerCompte() {
        compte = new CompteStandard();
    }

    @Test
    @DisplayName("Un compte neuf démarre à zéro")
    void compteNeufAZero() {
        assertEquals(0, new BigDecimal("0.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Un crédit augmente le solde du montant exact")
    void crediterAugmenteLeSolde() {
        compte.crediter(new BigDecimal("500.00"));

        assertEquals(0, new BigDecimal("500.00").compareTo(compte.getSolde()));
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
        compte.crediter(new BigDecimal("500.00"));

        compte.debiter(new BigDecimal("200.00"));

        assertEquals(0, new BigDecimal("300.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Un débit négatif lève MontantInvalideException")
    void debiterNegatifEstRefuse() {
        compte.crediter(new BigDecimal("500.00"));

        assertThrows(MontantInvalideException.class,
                () -> compte.debiter(new BigDecimal("-10.00")));
    }

    @Test
    @DisplayName("TIER — un débit peut rendre le solde négatif dans la limite des 300 € de découvert")
    void decouvertAutoriseJusquAuPlafondDuTier() {
        compte.crediter(new BigDecimal("100.00"));

        compte.debiter(new BigDecimal("250.00"));

        assertEquals(0, new BigDecimal("-150.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("TIER — un débit qui amène exactement à -300 € est accepté")
    void debitJusquALaLimiteExacteDuDecouvert() {
        compte.crediter(new BigDecimal("100.00"));

        compte.debiter(new BigDecimal("400.00"));

        assertEquals(0, new BigDecimal("-300.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("TIER — un débit qui dépasse le découvert de 300 € est refusé")
    void debitAuDelaDuDecouvertEstRefuse() {
        compte.crediter(new BigDecimal("100.00"));

        assertThrows(SoldeInsuffisantException.class,
                () -> compte.debiter(new BigDecimal("400.01")));
    }

    @Test
    @DisplayName("Un débit refusé laisse le solde inchangé")
    void debitRefuseNAlterePasLeSolde() {
        compte.crediter(new BigDecimal("100.00"));

        assertThrows(SoldeInsuffisantException.class,
                () -> compte.debiter(new BigDecimal("1000.00")));

        assertEquals(0, new BigDecimal("100.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Le découvert autorisé du tier Standard est de 300 €")
    void decouvertAutoriseDuTier() {
        assertEquals(0, DECOUVERT.compareTo(new CompteStandard().getDecouvertAutorise()));
    }

    @Test
    @DisplayName("Les frais de tenue du tier Standard sont de 2 € par mois")
    void fraisDeTenueDuTier() {
        assertEquals(0, new BigDecimal("2.00")
                .compareTo(new CompteStandard().getFraisTenueMensuels()));
    }
}
