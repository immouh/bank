package com.example.bank.core.model.offre.pret.etat.concret;

import com.example.bank.core.model.offre.pret.etat.EtatPret;

/**
 * Demande de prêt déposée, pas encore instruite. Aucune mensualité n'est
 * exigible : rien n'engage encore l'emprunteur.
 *
 * C'est l'état de départ de tout prêt.
 */
public class PretEnAttente implements EtatPret {

    @Override
    public String libelle() {
        return "prêt en attente";
    }

    @Override
    public EtatPret approuver() {
        return new PretApprouve();
    }
}
