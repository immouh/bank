package com.example.bank.core.model.offre.taux.concret;

import com.example.bank.core.model.offre.taux.TauxInteretStrategy;

import java.math.BigDecimal;

/**
 * Rémunération du Livret A de l'offre Standard : 3,00 % annuel — rémunération intermédiaire.
 *
 * VALEUR D'EXEMPLE, à ajuster selon le taux réglementaire en vigueur.
 */
public class TauxLivretAStandard implements TauxInteretStrategy {

    /** 3,00 % annuel, exprimé en fraction décimale. */
    private static final BigDecimal TAUX_ANNUEL = new BigDecimal("0.0300");

    @Override
    public BigDecimal getTaux() {
        return TAUX_ANNUEL;
    }
}
