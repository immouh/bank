package com.example.bank.core.model.offre.carte.etat.concret;

import com.example.bank.core.model.offre.carte.etat.EtatCarte;

/**
 * Carte arrivée au bout de sa validité. État TERMINAL : aucun paiement, et
 * pas de retour en arrière — on ne réactive pas une carte périmée, on en
 * fabrique une nouvelle.
 *
 * La classe ne redéfinit donc rien : tous les refus viennent des méthodes par
 * défaut de {@link EtatCarte}.
 */
public class CarteExpiree implements EtatCarte {

    @Override
    public String libelle() {
        return "carte expirée";
    }
}
