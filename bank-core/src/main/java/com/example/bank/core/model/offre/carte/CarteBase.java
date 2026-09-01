package com.example.bank.core.model.offre.carte;

import com.example.bank.core.model.offre.carte.etat.EtatCarte;
import com.example.bank.core.model.offre.carte.etat.concret.CarteActive;

import java.math.BigDecimal;

/**
 * Socle commun aux cartes des trois tiers : il détient l'état et lui délègue
 * tout. Les implémentations concrètes ne portent plus que leurs constantes
 * (plafond, cotisation, garanties).
 *
 * Le {@code boolean active} qu'elles recopiaient chacune est remplacé par cet
 * état unique : un booléen ne savait pas distinguer « bloquée » (réversible)
 * d'« expirée » (définitive).
 */
public abstract class CarteBase implements CarteBancaire {

    private EtatCarte etat = new CarteActive();

    @Override
    public EtatCarte getEtat() {
        return etat;
    }

    @Override
    public boolean estActive() {
        return etat.estActive();
    }

    @Override
    public void autoriserPaiement(BigDecimal montant) {
        etat.autoriserPaiement(this, montant);
    }

    @Override
    public void bloquer() {
        etat = etat.bloquer();
    }

    @Override
    public void activer() {
        etat = etat.activer();
    }

    @Override
    public void expirer() {
        etat = etat.expirer();
    }
}
