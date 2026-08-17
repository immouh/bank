package com.example.bank.core.exception;

/** Montant nul, négatif ou absent. */
public class MontantInvalideException extends BanqueException {

    public MontantInvalideException(String message) {
        super(message);
    }
}
