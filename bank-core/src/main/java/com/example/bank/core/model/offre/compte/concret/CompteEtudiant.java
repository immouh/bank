package com.example.bank.core.model.offre.compte.concret;

import com.example.bank.core.model.offre.compte.CompteBase;

import java.math.BigDecimal;

/**
 * Compte de l'offre Étudiante : aucun découvert, aucun frais.
 *
 * VALEURS D'EXEMPLE, à ajuster selon la grille tarifaire réelle.
 */
public class CompteEtudiant extends CompteBase {

    /** Aucun découvert : le solde ne peut pas descendre sous zéro. */
    private static final BigDecimal DECOUVERT_AUTORISE = new BigDecimal("0.00");
    /** Compte sans frais, argument commercial de l'offre. */
    private static final BigDecimal FRAIS_TENUE_MENSUELS = new BigDecimal("0.00");

    @Override
    public BigDecimal getDecouvertAutorise() {
        return DECOUVERT_AUTORISE;
    }

    public BigDecimal getFraisTenueMensuels() {
        return FRAIS_TENUE_MENSUELS;
    }
}
