package com.example.bank.core.model.offre.compte.etat.concret;

import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.etat.EtatCompte;

import java.math.BigDecimal;

/**
 * Compte mis en opposition : plus aucune opération du client, dans les deux
 * sens.
 *
 * SEULE EXCEPTION — le crédit de RÉGULARISATION, à l'initiative de la banque.
 * Sans lui, un compte bloqué en découvert serait dans une impasse : on ne
 * pourrait ni le renflouer ni le débloquer proprement. Le crédit ordinaire
 * reste refusé, pour que ce mouvement de faveur passe par un appel explicite
 * ({@code compte.crediterRegularisation(...)}) qui se voit dans le code et
 * dans l'historique.
 *
 * Un blocage ne se lève jamais tout seul : la régularisation renfloue le
 * solde mais laisse le compte bloqué, seul {@code debloquer()} en sort.
 */
public class CompteBloque implements EtatCompte {

    @Override
    public String libelle() {
        return "compte bloqué";
    }

    @Override
    public BigDecimal crediterRegularisation(Compte compte, BigDecimal montant) {
        return Compte.soldeApresCredit(compte.getSolde(), montant);
    }

    /** Le blocage survit à la régularisation : la sortie est manuelle. */
    @Override
    public EtatCompte apresVariationDeSolde(BigDecimal nouveauSolde) {
        return this;
    }

    /** Le compte repart d'un état cohérent avec son solde du moment. */
    @Override
    public EtatCompte debloquer(BigDecimal soldeCourant) {
        return soldeCourant.signum() < 0 ? new CompteEnDecouvert() : new CompteActif();
    }

    @Override
    public EtatCompte fermer() {
        return new CompteFerme();
    }
}
