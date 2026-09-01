package com.example.bank.core.model.offre.compte.concret;

import com.example.bank.core.model.offre.compte.CompteBase;

import java.math.BigDecimal;

/**
 * Compte de l'offre Premium : découvert large, en contrepartie de frais de
 * tenue plus élevés.
 *
 * VALEURS D'EXEMPLE, à ajuster selon la grille tarifaire réelle.
 */
public class ComptePremium extends CompteBase {

    private static final BigDecimal DECOUVERT_AUTORISE = new BigDecimal("2000.00");
    private static final BigDecimal FRAIS_TENUE_MENSUELS = new BigDecimal("12.00");

    @Override
    public BigDecimal getDecouvertAutorise() {
        return DECOUVERT_AUTORISE;
    }

    public BigDecimal getFraisTenueMensuels() {
        return FRAIS_TENUE_MENSUELS;
    }
}
