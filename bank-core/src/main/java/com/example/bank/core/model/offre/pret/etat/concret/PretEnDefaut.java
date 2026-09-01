package com.example.bank.core.model.offre.pret.etat.concret;

import com.example.bank.core.model.offre.pret.etat.EtatPret;

/**
 * Prêt en défaut de paiement. État TERMINAL du point de vue de l'échéancier :
 * le dossier sort du circuit normal (recouvrement), plus aucune mensualité
 * n'est exigible au sens du contrat initial.
 *
 * On n'y arrive JAMAIS tout seul : la détection d'impayé n'est pas
 * implémentée, seul un appel explicite à {@code declarerDefaut()} depuis
 * {@code EnRemboursement} y mène.
 *
 * La classe ne redéfinit rien : tous les refus viennent des méthodes par
 * défaut de {@link EtatPret}.
 */
public class PretEnDefaut implements EtatPret {

    @Override
    public String libelle() {
        return "prêt en défaut";
    }
}
