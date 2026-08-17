package com.example.bank.core.exception;

/** Opération demandée sur un Livret A qui n'a jamais été ouvert. */
public class LivretAAbsentException extends BanqueException {

    public LivretAAbsentException() {
        super("Livret A inexistant.");
    }
}
