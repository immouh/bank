package com.example.bank.core.exception.existence;

/** Opération demandée sur un Livret A qui n'a jamais été ouvert. */
public class LivretAAbsentException extends ErreurExistenceException {

    public LivretAAbsentException() {
        super("Livret A inexistant.");
    }
}
