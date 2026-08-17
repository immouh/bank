package com.example.bank.core.model.offre.concret;

import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.carte.CarteBancaire;
import com.example.bank.core.model.offre.carte.concret.CarteJeune;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.concret.CompteEtudiant;
import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.pret.concret.PretEtudiant;
import com.example.bank.core.model.offre.taux.TauxInteretStrategy;
import com.example.bank.core.model.offre.taux.concret.TauxLivretAEtudiant;

/**
 * Fabrique de l'offre Étudiante : produit la famille cohérente
 * CompteEtudiant + CarteJeune + PretEtudiant + TauxLivretAEtudiant.
 *
 * Le type de retour reste l'interface : l'appelant ne dépend jamais des
 * classes concrètes, seule cette fabrique les connaît.
 */
public class OffreEtudianteFactory implements OffreFactory {

    @Override
    public Compte creerCompte() {
        return new CompteEtudiant();
    }

    @Override
    public CarteBancaire creerCarte() {
        return new CarteJeune();
    }

    @Override
    public Pret creerPret() {
        return new PretEtudiant();
    }

    @Override
    public TauxInteretStrategy creerStrategieTaux() {
        return new TauxLivretAEtudiant();
    }
}
