package com.example.bank.core.model.offre.compte.etat;

import com.example.bank.core.exception.etat.OperationInterditeException;
import com.example.bank.core.model.offre.compte.Compte;

import java.math.BigDecimal;

/**
 * État d'un compte : {@code Actif}, {@code EnDécouvert}, {@code Bloqué},
 * {@code Fermé}.
 *
 * PATRON STATE — c'est l'état qui porte les règles, pas une cascade de
 * {@code if} dans le service. Chaque méthode par défaut REFUSE : un état
 * n'autorise que ce qu'il redéfinit explicitement. On ne peut donc pas
 * oublier d'interdire quelque chose, seulement oublier de l'autoriser — et
 * ça, un test le voit tout de suite.
 *
 * Les états sont sans état interne (le solde appartient au compte), donc
 * interchangeables et librement instanciables.
 */
public interface EtatCompte {

    /** Libellé lisible, utilisé dans les messages d'erreur et par l'IHM. */
    String libelle();

    /** Vrai si le compte accepte les opérations courantes du client. */
    default boolean autoriseOperations() {
        return false;
    }

    /**
     * Crédit ordinaire.
     *
     * @return le nouveau solde
     */
    default BigDecimal crediter(Compte compte, BigDecimal montant) {
        throw OperationInterditeException.operation("crédit", libelle());
    }

    /**
     * Débit ordinaire.
     *
     * @return le nouveau solde
     */
    default BigDecimal debiter(Compte compte, BigDecimal montant) {
        throw OperationInterditeException.operation("débit", libelle());
    }

    /**
     * Crédit de régularisation, à l'initiative de la banque : le seul
     * mouvement qu'un compte bloqué accepte, puisque c'est ce qui permet de
     * revenir à flot avant un déblocage.
     *
     * @return le nouveau solde
     */
    default BigDecimal crediterRegularisation(Compte compte, BigDecimal montant) {
        throw OperationInterditeException.operation("régularisation", libelle());
    }

    /**
     * Transition AUTOMATIQUE déclenchée après chaque mouvement de solde.
     * Par défaut l'état ne bouge pas : seuls {@code Actif} et
     * {@code EnDécouvert} basculent l'un vers l'autre selon le signe du solde.
     */
    default EtatCompte apresVariationDeSolde(BigDecimal nouveauSolde) {
        return this;
    }

    /** Transition MANUELLE : mise en opposition du compte. */
    default EtatCompte bloquer() {
        throw OperationInterditeException.transition("blocage", libelle());
    }

    /** Transition MANUELLE : levée du blocage, l'état repart du signe du solde. */
    default EtatCompte debloquer(BigDecimal soldeCourant) {
        throw OperationInterditeException.transition("déblocage", libelle());
    }

    /** Transition MANUELLE : clôture définitive. */
    default EtatCompte fermer() {
        throw OperationInterditeException.transition("fermeture", libelle());
    }
}
