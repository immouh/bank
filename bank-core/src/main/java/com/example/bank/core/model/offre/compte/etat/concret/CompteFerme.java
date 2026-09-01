package com.example.bank.core.model.offre.compte.etat.concret;

import com.example.bank.core.model.offre.compte.etat.EtatCompte;

/**
 * Compte clôturé. État TERMINAL : aucune opération, aucune transition, pas
 * même une régularisation. Un compte fermé ne se rouvre pas, on en ouvre un
 * nouveau.
 *
 * La classe ne redéfinit donc rien : tous les refus viennent des méthodes par
 * défaut de {@link EtatCompte}.
 */
public class CompteFerme implements EtatCompte {

    @Override
    public String libelle() {
        return "compte fermé";
    }
}
