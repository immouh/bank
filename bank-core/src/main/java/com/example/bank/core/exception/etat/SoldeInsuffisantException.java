package com.example.bank.core.exception.etat;

import java.math.BigDecimal;

/** Le compte débité ne couvre pas le montant demandé. */
public class SoldeInsuffisantException extends ErreurEtatException {

    public SoldeInsuffisantException(BigDecimal disponible, BigDecimal demande) {
        super("Solde insuffisant : " + demande + " € demandés, "
                + disponible + " € disponibles.");
    }
}
