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
 * COMPARAISON DES BigDecimal — tous les tests utilisent
 * {@code assertEquals(0, attendu.compareTo(reel))} et jamais
 * {@code assertEquals(attendu, reel)} : {@code BigDecimal.equals} compare AUSSI
 * l'échelle, donc {@code new BigDecimal("60").equals(new BigDecimal("60.00"))}
 * vaut {@code false}. {@code compareTo} ne compare que la valeur numérique.
 */
@DisplayName("Compte de l'offre Étudiante")
class CompteEtudiantTest {

    private Compte compte;

    @BeforeEach
    void creerCompte() {
        compte = new CompteEtudiant();
    }

    @Test
    @DisplayName("Un compte neuf démarre à zéro")
    void compteNeufAZero() {
        assertEquals(0, new BigDecimal("0.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Un crédit augmente le solde du montant exact")
    void crediterAugmenteLeSolde() {
        compte.crediter(new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("100.00").compareTo(compte.getSolde()));
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
        compte.crediter(new BigDecimal("100.00"));

        compte.debiter(new BigDecimal("40.00"));

        assertEquals(0, new BigDecimal("60.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Un débit égal au solde est accepté et laisse le compte à zéro")
    void debiterExactementLeSoldeEstAccepte() {
        compte.crediter(new BigDecimal("100.00"));

        compte.debiter(new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("0.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Un débit négatif lève MontantInvalideException")
    void debiterNegatifEstRefuse() {
        compte.crediter(new BigDecimal("100.00"));

        assertThrows(MontantInvalideException.class,
                () -> compte.debiter(new BigDecimal("-10.00")));
    }

    @Test
    @DisplayName("TIER — sans découvert autorisé, un débit qui rendrait le solde négatif est refusé")
    void aucunDecouvertAutorise() {
        compte.crediter(new BigDecimal("100.00"));

        assertThrows(SoldeInsuffisantException.class,
                () -> compte.debiter(new BigDecimal("100.01")));
    }

    @Test
    @DisplayName("Un débit refusé laisse le solde inchangé")
    void debitRefuseNAlterePasLeSolde() {
        compte.crediter(new BigDecimal("100.00"));

        assertThrows(SoldeInsuffisantException.class,
                () -> compte.debiter(new BigDecimal("150.00")));

        assertEquals(0, new BigDecimal("100.00").compareTo(compte.getSolde()));
    }

    @Test
    @DisplayName("Le découvert autorisé du tier Étudiante est de 0 €")
    void decouvertAutoriseDuTier() {
        assertEquals(0, new BigDecimal("0.00")
                .compareTo(new CompteEtudiant().getDecouvertAutorise()));
    }

    @Test
    @DisplayName("Le compte de l'offre Étudiante est sans frais de tenue")
    void fraisDeTenueDuTier() {
        assertEquals(0, new BigDecimal("0.00")
                .compareTo(new CompteEtudiant().getFraisTenueMensuels()));
    }
}
