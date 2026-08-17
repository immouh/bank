package com.example.bank.core.model.offre.taux.concret;

import com.example.bank.core.model.offre.taux.TauxInteretStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("0.02" != "0.0200").
 */
@DisplayName("Taux d'épargne de l'offre Étudiante")
class TauxLivretAEtudiantTest {

    private final TauxInteretStrategy strategie = new TauxLivretAEtudiant();

    @Test
    @DisplayName("Le taux annuel du tier est de 2,00 %")
    void tauxDuTier() {
        assertEquals(0, new BigDecimal("0.0200").compareTo(strategie.getTaux()));
    }

    @Test
    @DisplayName("HIÉRARCHIE — le taux Étudiante est strictement inférieur au Standard, lui-même strictement inférieur au Premium")
    void hierarchieDesTauxEntreLesTroisTiers() {
        // Verrouille la règle commerciale : plus l'offre monte en gamme,
        // mieux l'épargne est rémunérée. Un futur ajustement de barème qui
        // casserait cet ordre fera échouer ce test.
        BigDecimal etudiant = new TauxLivretAEtudiant().getTaux();
        BigDecimal standard = new TauxLivretAStandard().getTaux();
        BigDecimal premium = new TauxLivretAPremium().getTaux();

        assertTrue(etudiant.compareTo(standard) < 0 && standard.compareTo(premium) < 0,
                "Ordre attendu : " + etudiant + " < " + standard + " < " + premium);
    }
}
