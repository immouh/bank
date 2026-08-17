package com.example.bank.core.model.offre.compte.concret;

import com.example.bank.core.model.Montants;
import com.example.bank.core.model.offre.compte.Compte;

import java.math.BigDecimal;

/**
 * Compte de l'offre Étudiante : gratuit, mais aucun découvert toléré.
 *
 * VALEURS D'EXEMPLE, à ajuster selon la grille tarifaire réelle.
 */
public class CompteEtudiant implements Compte {

    /** Aucun découvert : le solde ne peut pas descendre sous zéro. */
    private static final BigDecimal DECOUVERT_AUTORISE = new BigDecimal("0.00");
    /** Compte sans frais, argument commercial de l'offre. */
    private static final BigDecimal FRAIS_TENUE_MENSUELS = new BigDecimal("0.00");

    private BigDecimal solde = Montants.ZERO;

    @Override
    public BigDecimal getSolde() {
        return solde;
    }

    @Override
    public void crediter(BigDecimal montant) {
        solde = Compte.soldeApresCredit(solde, montant);
    }

    @Override
    public void debiter(BigDecimal montant) {
        solde = Compte.soldeApresDebit(solde, montant, DECOUVERT_AUTORISE);
    }

    public BigDecimal getDecouvertAutorise() {
        return DECOUVERT_AUTORISE;
    }

    public BigDecimal getFraisTenueMensuels() {
        return FRAIS_TENUE_MENSUELS;
    }
}
