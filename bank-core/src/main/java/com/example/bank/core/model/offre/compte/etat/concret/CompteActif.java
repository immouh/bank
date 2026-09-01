package com.example.bank.core.model.offre.compte.etat.concret;

import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.etat.EtatCompte;

import java.math.BigDecimal;

/**
 * Compte au solde positif ou nul : toutes les opérations courantes sont
 * ouvertes.
 *
 * Les règles de calcul ne sont pas réécrites ici — ce sont exactement celles
 * de {@link Compte#soldeApresCredit} et {@link Compte#soldeApresDebit}, déjà
 * partagées par les trois tiers.
 */
public class CompteActif implements EtatCompte {

    @Override
    public String libelle() {
        return "compte actif";
    }

    @Override
    public boolean autoriseOperations() {
        return true;
    }

    @Override
    public BigDecimal crediter(Compte compte, BigDecimal montant) {
        return Compte.soldeApresCredit(compte.getSolde(), montant);
    }

    @Override
    public BigDecimal debiter(Compte compte, BigDecimal montant) {
        return Compte.soldeApresDebit(compte.getSolde(), montant, compte.getDecouvertAutorise());
    }

    @Override
    public BigDecimal crediterRegularisation(Compte compte, BigDecimal montant) {
        return crediter(compte, montant);
    }

    /** Un solde qui passe sous zéro fait basculer le compte en découvert. */
    @Override
    public EtatCompte apresVariationDeSolde(BigDecimal nouveauSolde) {
        return nouveauSolde.signum() < 0 ? new CompteEnDecouvert() : this;
    }

    @Override
    public EtatCompte bloquer() {
        return new CompteBloque();
    }

    @Override
    public EtatCompte fermer() {
        return new CompteFerme();
    }
}
