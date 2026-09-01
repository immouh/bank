package com.example.bank.core.model.offre.carte;

import com.example.bank.core.model.offre.carte.etat.EtatCarte;

import java.math.BigDecimal;

/**
 * Carte bancaire d'une offre. Chaque tier a sa propre implémentation, qui se
 * distingue par son plafond de paiement et sa cotisation annuelle.
 *
 * CYCLE DE VIE — la carte délègue à son {@link EtatCarte} courant (patron
 * State) : active, bloquée ou expirée. Les implémentations passent par
 * {@link CarteBase}, qui tient l'état.
 */
public interface CarteBancaire {

    /** Plafond de paiement mensuel, en euros. */
    BigDecimal getPlafond();

    /** Une carte bloquée ou expirée refuse les paiements. */
    boolean estActive();

    /** État courant de la carte. */
    EtatCarte getEtat();

    /**
     * Autorise un paiement ou un retrait par carte : refusé si la carte n'est
     * pas active, ou si le montant dépasse le plafond du tier.
     */
    void autoriserPaiement(BigDecimal montant);

    /** Met la carte en opposition. Réversible par {@link #activer()}. */
    void bloquer();

    /** Lève l'opposition. Sans effet sur une carte déjà active, refusé si expirée. */
    void activer();

    /** Fin de validité : état terminal, sans retour possible. */
    void expirer();
}
