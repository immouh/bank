package com.example.bank.core.model.offre.carte;

import java.math.BigDecimal;

/**
 * Carte bancaire d'une offre. Chaque tier a sa propre implémentation, qui se
 * distingue par son plafond de paiement et sa cotisation annuelle.
 */
public interface CarteBancaire {

    /** Plafond de paiement mensuel, en euros. */
    BigDecimal getPlafond();

    /** Une carte bloquée refuse les paiements. */
    boolean estActive();
}
