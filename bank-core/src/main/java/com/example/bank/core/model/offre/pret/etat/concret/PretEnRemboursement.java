package com.example.bank.core.model.offre.pret.etat.concret;

import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.pret.etat.EtatPret;

import java.math.BigDecimal;

/**
 * Fonds débloqués, mensualités en cours. Deux sorties possibles, et deux
 * seulement : le prêt va au bout ({@code Soldé}) ou l'emprunteur décroche
 * ({@code EnDéfaut}).
 */
public class PretEnRemboursement implements EtatPret {

    @Override
    public String libelle() {
        return "prêt en remboursement";
    }

    @Override
    public BigDecimal mensualiteExigible(Pret pret) {
        return pret.calculerMensualite();
    }

    @Override
    public EtatPret solder() {
        return new PretSolde();
    }

    @Override
    public EtatPret declarerDefaut() {
        return new PretEnDefaut();
    }
}
