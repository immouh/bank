package com.example.bank.core.model.offre.taux;

import java.math.BigDecimal;

/**
 * Patron Strategy : encapsule le calcul du taux de rémunération de l'épargne,
 * pour qu'il puisse varier indépendamment du compte qui l'utilise.
 *
 * Le Livret A lui-même n'existe pas encore dans le modèle ; ces stratégies
 * sont posées prêtes à lui être injectées.
 */
public interface TauxInteretStrategy {

    /**
     * Taux ANNUEL de rémunération, exprimé en fraction décimale :
     * {@code 0.0300} signifie 3,00 %.
     */
    BigDecimal getTaux();
}
