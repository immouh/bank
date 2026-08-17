package com.example.bank.core.model.offre.concret;

import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.carte.CarteBancaire;
import com.example.bank.core.model.offre.carte.concret.CarteClassique;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.concret.CompteStandard;
import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.pret.concret.PretPersonnel;
import com.example.bank.core.model.offre.taux.TauxInteretStrategy;
import com.example.bank.core.model.offre.taux.concret.TauxLivretAStandard;

/**
 * Fabrique de l'offre Standard : produit la famille cohérente
 * CompteStandard + CarteClassique + PretPersonnel + TauxLivretAStandard.
 *
 * Le type de retour reste l'interface : l'appelant ne dépend jamais des
 * classes concrètes, seule cette fabrique les connaît.
 */
public class OffreStandardFactory implements OffreFactory {

    @Override
    public Compte creerCompte() {
        return new CompteStandard();
    }

    @Override
    public CarteBancaire creerCarte() {
        return new CarteClassique();
    }

    @Override
    public Pret creerPret() {
        return new PretPersonnel();
    }

    @Override
    public TauxInteretStrategy creerStrategieTaux() {
        return new TauxLivretAStandard();
    }
}
