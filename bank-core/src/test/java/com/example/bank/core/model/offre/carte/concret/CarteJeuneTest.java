package com.example.bank.core.model.offre.carte.concret;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("500" != "500.00").
 */
@DisplayName("Carte de l'offre Étudiante")
class CarteJeuneTest {

    private final CarteJeune carte = new CarteJeune();

    @Test
    @DisplayName("Le plafond de paiement du tier est de 500.00 €")
    void plafondDuTier() {
        assertEquals(0, new BigDecimal("500.00").compareTo(carte.getPlafond()));
    }

    @Test
    @DisplayName("Une carte neuve est active par défaut")
    void carteNeuveEstActive() {
        assertTrue(carte.estActive());
    }

    @Test
    @DisplayName("Une carte bloquée n'est plus active")
    void carteBloqueeNEstPlusActive() {
        carte.bloquer();

        assertFalse(carte.estActive());
    }

    @Test
    @DisplayName("La cotisation annuelle du tier est de 0.00 €")
    void cotisationDuTier() {
        assertEquals(0, new BigDecimal("0.00").compareTo(carte.getCotisationAnnuelle()));
    }
}
