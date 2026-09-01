package com.example.bank.core.model.offre.compte;

import com.example.bank.core.model.Montants;
import com.example.bank.core.model.offre.compte.etat.EtatCompte;
import com.example.bank.core.model.offre.compte.etat.concret.CompteActif;

import java.math.BigDecimal;

/**
 * Socle commun aux comptes des trois tiers : il détient le solde et l'état,
 * et délègue chaque opération à l'état courant.
 *
 * Les trois implémentations concrètes écrivaient jusqu'ici exactement le même
 * corps de {@code crediter} / {@code debiter} ; il ne leur reste ici que les
 * constantes qui les distinguent réellement (découvert, frais).
 *
 * DÉLÉGATION — cette classe ne décide de rien. Elle demande à l'état le
 * nouveau solde, l'enregistre, puis laisse l'état dire ce qu'il devient. Si
 * l'état refuse, il lève l'exception avant toute écriture : le solde reste
 * intact.
 */
public abstract class CompteBase implements Compte {

    private BigDecimal solde = Montants.ZERO;
    private EtatCompte etat = new CompteActif();

    @Override
    public BigDecimal getSolde() {
        return solde;
    }

    @Override
    public EtatCompte getEtat() {
        return etat;
    }

    @Override
    public void crediter(BigDecimal montant) {
        appliquer(etat.crediter(this, montant));
    }

    @Override
    public void debiter(BigDecimal montant) {
        appliquer(etat.debiter(this, montant));
    }

    @Override
    public void crediterRegularisation(BigDecimal montant) {
        appliquer(etat.crediterRegularisation(this, montant));
    }

    @Override
    public void bloquer() {
        etat = etat.bloquer();
    }

    @Override
    public void debloquer() {
        etat = etat.debloquer(solde);
    }

    @Override
    public void fermer() {
        etat = etat.fermer();
    }

    /**
     * Enregistre le nouveau solde puis rejoue la transition automatique
     * Actif <-> EnDécouvert. C'est le seul endroit où le solde change.
     */
    private void appliquer(BigDecimal nouveauSolde) {
        solde = nouveauSolde;
        etat = etat.apresVariationDeSolde(nouveauSolde);
    }
}
