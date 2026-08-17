package com.example.bank.core.model.offre.taux.concret;

import com.example.bank.core.model.offre.taux.TauxInteretStrategy;

import java.math.BigDecimal;

/**
 * Rémunération du Livret A de l'offre Premium : 4,50 % annuel — rémunération la plus élevée, contrepartie de la cotisation Premium.
 *
 * VALEUR D'EXEMPLE, à ajuster selon le taux réglementaire en vigueur.
 */
public class TauxLivretAPremium implements TauxInteretStrategy {

    /** 4,50 % annuel, exprimé en fraction décimale. */
    private static final BigDecimal TAUX_ANNUEL = new BigDecimal("0.0450");

    @Override
    public BigDecimal getTaux() {
        return TAUX_ANNUEL;
    }
}
