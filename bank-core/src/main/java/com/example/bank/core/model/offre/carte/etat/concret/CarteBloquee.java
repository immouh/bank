package com.example.bank.core.model.offre.carte.etat.concret;

import com.example.bank.core.model.offre.carte.etat.EtatCarte;

/**
 * Carte en opposition : tout paiement est refusé, quel que soit le montant et
 * l'état du compte derrière.
 *
 * Le blocage est réversible — c'est ce qui la distingue d'{@code Expirée} :
 * une opposition levée redonne une carte utilisable, une carte périmée est
 * définitivement hors jeu.
 */
public class CarteBloquee implements EtatCarte {

    @Override
    public String libelle() {
        return "carte bloquée";
    }

    @Override
    public EtatCarte activer() {
        return new CarteActive();
    }

    /** Une carte bloquée peut arriver au bout de sa validité. */
    @Override
    public EtatCarte expirer() {
        return new CarteExpiree();
    }
}
