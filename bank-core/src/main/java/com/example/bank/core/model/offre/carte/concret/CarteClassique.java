package com.example.bank.core.model.offre.carte.concret;

import com.example.bank.core.model.offre.carte.CarteBancaire;

import java.math.BigDecimal;

/**
 * Carte de l'offre Standard : plafond et cotisation intermédiaires.
 *
 * VALEURS D'EXEMPLE, à ajuster selon la grille tarifaire réelle.
 */
public class CarteClassique implements CarteBancaire {

    private static final BigDecimal PLAFOND_MENSUEL = new BigDecimal("1500.00");
    private static final BigDecimal COTISATION_ANNUELLE = new BigDecimal("45.00");

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
