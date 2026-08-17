package com.example.bank.core.model.offre.taux.concret;

import com.example.bank.core.model.offre.taux.TauxInteretStrategy;

import java.math.BigDecimal;

/**
 * Rémunération du Livret A de l'offre Etudiant : 2,00 % annuel — rémunération la plus basse, cohérente avec une offre gratuite.
 *
 * VALEUR D'EXEMPLE, à ajuster selon le taux réglementaire en vigueur.
 */
public class TauxLivretAEtudiant implements TauxInteretStrategy {

    /** 2,00 % annuel, exprimé en fraction décimale. */
    private static final BigDecimal TAUX_ANNUEL = new BigDecimal("0.0200");

    @Override
    public BigDecimal getTaux() {
        return TAUX_ANNUEL;
    }
}
