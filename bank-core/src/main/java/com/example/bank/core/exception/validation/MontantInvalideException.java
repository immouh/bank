package com.example.bank.core.exception.validation;

/** Montant nul, négatif ou absent. */
public class MontantInvalideException extends ErreurValidationException {

    public MontantInvalideException(String message) {
        super(message);
    }
}
