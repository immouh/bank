package com.example.bank.core.exception.etat;

import java.math.BigDecimal;

/**
 * Le paiement dépasse le plafond de la carte.
 *
 * Distincte de {@link SoldeInsuffisantException} : le compte peut être
 * largement approvisionné, c'est la carte qui limite l'opération.
 */
public class PlafondDepasseException extends ErreurEtatException {

    public PlafondDepasseException(BigDecimal plafond, BigDecimal demande) {
        super("Plafond de la carte dépassé : " + demande + " € demandés, "
                + plafond + " € autorisés.");
    }
}
