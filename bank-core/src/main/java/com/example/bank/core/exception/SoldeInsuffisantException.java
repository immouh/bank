package com.example.bank.core.exception;

import java.math.BigDecimal;

/** Le compte débité ne couvre pas le montant demandé. */
public class SoldeInsuffisantException extends BanqueException {

    public SoldeInsuffisantException(BigDecimal disponible, BigDecimal demande) {
        super("Solde insuffisant : " + demande + " € demandés, "
                + disponible + " € disponibles.");
    }
}
