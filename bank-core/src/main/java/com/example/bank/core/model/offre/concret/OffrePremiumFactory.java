package com.example.bank.core.model.offre.concret;

import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.carte.CarteBancaire;
import com.example.bank.core.model.offre.carte.concret.CarteBlack;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.concret.ComptePremium;
import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.pret.concret.PretImmobilier;
import com.example.bank.core.model.offre.taux.TauxInteretStrategy;
import com.example.bank.core.model.offre.taux.concret.TauxLivretAPremium;

/**
 * Fabrique de l'offre Premium : produit la famille cohérente
 * ComptePremium + CarteBlack + PretImmobilier + TauxLivretAPremium.
 *
 * Le type de retour reste l'interface : l'appelant ne dépend jamais des
 * classes concrètes, seule cette fabrique les connaît.
 */
public class OffrePremiumFactory implements OffreFactory {

    @Override
    public Compte creerCompte() {
        return new ComptePremium();
    }

    @Override
    public CarteBancaire creerCarte() {
        return new CarteBlack();
    }

    @Override
    public Pret creerPret() {
        return new PretImmobilier();
    }

    @Override
    public TauxInteretStrategy creerStrategieTaux() {
        return new TauxLivretAPremium();
    }
}
