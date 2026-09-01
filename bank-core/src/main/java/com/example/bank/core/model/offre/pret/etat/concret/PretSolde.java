package com.example.bank.core.model.offre.pret.etat.concret;

import com.example.bank.core.model.offre.pret.etat.EtatPret;

/**
 * Prêt remboursé jusqu'à la dernière échéance. État TERMINAL : plus rien
 * n'est exigible, et un prêt soldé ne repart pas.
 *
 * La classe ne redéfinit donc rien : tous les refus viennent des méthodes par
 * défaut de {@link EtatPret}.
 */
public class PretSolde implements EtatPret {

    @Override
    public String libelle() {
        return "prêt soldé";
    }
}
