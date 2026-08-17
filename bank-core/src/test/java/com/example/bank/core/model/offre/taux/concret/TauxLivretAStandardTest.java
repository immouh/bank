package com.example.bank.core.model.offre.taux.concret;

import com.example.bank.core.model.offre.taux.TauxInteretStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("0.02" != "0.0200").
 */
@DisplayName("Taux d'épargne de l'offre Standard")
class TauxLivretAStandardTest {

    private final TauxInteretStrategy strategie = new TauxLivretAStandard();

    @Test
    @DisplayName("Le taux annuel du tier est de 3,00 %")
    void tauxDuTier() {
        assertEquals(0, new BigDecimal("0.0300").compareTo(strategie.getTaux()));
    }
}
