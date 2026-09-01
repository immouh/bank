package com.example.bank.core.model.offre.carte.concret;

import com.example.bank.core.model.offre.carte.CarteBase;

import java.math.BigDecimal;

/**
 * Carte de l'offre Étudiante : plafond bas, cotisation offerte.
 *
 * VALEURS D'EXEMPLE, à ajuster selon la grille tarifaire réelle.
 */
public class CarteJeune extends CarteBase {

    private static final BigDecimal PLAFOND_MENSUEL = new BigDecimal("500.00");
    /** Cotisation offerte : argument commercial de l'offre Étudiante. */
    private static final BigDecimal COTISATION_ANNUELLE = new BigDecimal("0.00");

    @Override
    public BigDecimal getPlafond() {
        return PLAFOND_MENSUEL;
    }

    public BigDecimal getCotisationAnnuelle() {
        return COTISATION_ANNUELLE;
    }
}
