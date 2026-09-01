package com.example.bank.core.model.offre.carte.etat;

import com.example.bank.core.exception.OperationInterditeException;
import com.example.bank.core.model.offre.carte.CarteBancaire;

import java.math.BigDecimal;

/**
 * État d'une carte : {@code Active}, {@code Bloquée}, {@code Expirée}.
 *
 * PATRON STATE — même principe que {@code EtatCompte} : chaque méthode par
 * défaut refuse, un état n'autorise que ce qu'il redéfinit.
 */
public interface EtatCarte {

    /** Libellé lisible, utilisé dans les messages d'erreur et par l'IHM. */
    String libelle();

    /** Seule une carte {@code Active} répond vrai. */
    default boolean estActive() {
        return false;
    }

    /**
     * Autorise — ou refuse — un paiement ou un retrait passant par la carte.
     * Ne bouge aucun solde : la carte donne son accord, le compte est débité
     * ensuite.
     *
     * @throws OperationInterditeException si l'état de la carte l'interdit
     */
    default void autoriserPaiement(CarteBancaire carte, BigDecimal montant) {
        throw OperationInterditeException.operation("paiement", libelle());
    }

    /** Transition MANUELLE : mise en opposition (perte, vol, fraude). */
    default EtatCarte bloquer() {
        throw OperationInterditeException.transition("blocage", libelle());
    }

    /** Transition MANUELLE : remise en service après un blocage. */
    default EtatCarte activer() {
        throw OperationInterditeException.transition("activation", libelle());
    }

    /** Transition MANUELLE : fin de validité. État terminal. */
    default EtatCarte expirer() {
        throw OperationInterditeException.transition("expiration", libelle());
    }
}
