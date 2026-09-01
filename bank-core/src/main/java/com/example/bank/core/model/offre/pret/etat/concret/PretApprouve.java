package com.example.bank.core.model.offre.pret.etat.concret;

import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.pret.etat.EtatPret;

import java.math.BigDecimal;

/**
 * Prêt accordé, fonds pas encore débloqués. L'échéancier est arrêté : la
 * mensualité devient exigible, ce qui n'était pas le cas en attente.
 *
 * Le calcul n'est pas réécrit : c'est {@code Pret.mensualite}, la formule
 * d'amortissement déjà partagée par les trois tiers.
 */
public class PretApprouve implements EtatPret {

    @Override
    public String libelle() {
        return "prêt approuvé";
    }

    @Override
    public BigDecimal mensualiteExigible(Pret pret) {
        return pret.calculerMensualite();
    }

    @Override
    public EtatPret demarrerRemboursement() {
        return new PretEnRemboursement();
    }
}
