package com.example.bank.core.model.offre.carte.concret;

import com.example.bank.core.model.offre.carte.CarteBancaire;

import java.math.BigDecimal;

/**
 * Carte de l'offre Premium : plafond très élevé, assurance voyage incluse,
 * en contrepartie d'une cotisation annuelle importante.
 *
 * VALEURS D'EXEMPLE, à ajuster selon la grille tarifaire réelle.
 */
public class CarteBlack implements CarteBancaire {

    private static final BigDecimal PLAFOND_MENSUEL = new BigDecimal("10000.00");
    private static final BigDecimal COTISATION_ANNUELLE = new BigDecimal("300.00");
    /** Garantie propre au tier Premium. */
    private static final boolean ASSURANCE_VOYAGE_INCLUSE = true;

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

    public boolean aAssuranceVoyage() {
        return ASSURANCE_VOYAGE_INCLUSE;
    }

    public void bloquer() {
        active = false;
    }

    public void activer() {
        active = true;
    }
}
