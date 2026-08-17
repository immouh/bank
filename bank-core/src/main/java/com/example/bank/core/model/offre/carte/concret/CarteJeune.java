package com.example.bank.core.model.offre.carte.concret;

import com.example.bank.core.model.offre.carte.CarteBancaire;

import java.math.BigDecimal;

/**
 * Carte de l'offre Étudiante : plafond volontairement bas, cotisation offerte.
 *
 * VALEURS D'EXEMPLE, à ajuster selon la grille tarifaire réelle.
 */
public class CarteJeune implements CarteBancaire {

    private static final BigDecimal PLAFOND_MENSUEL = new BigDecimal("500.00");
    /** Cotisation offerte : argument commercial de l'offre Étudiante. */
    private static final BigDecimal COTISATION_ANNUELLE = new BigDecimal("0.00");

    private boolean active = true;

    @Override
    public BigDecimal getPlafond() {
        return PLAFOND_MENSUEL;
    }

    @Override
    public boolean estActive() {
        return active;
    }

    public BigDecimal getCotisationAnnuelle() {
        return COTISATION_ANNUELLE;
    }

    public void bloquer() {
        active = false;
    }

    public void activer() {
        active = true;
    }
}
