package com.example.bank.core.exception;

/**
 * Racine de toutes les exceptions métier de la banque.
 *
 * Elle existe pour que l'interface graphique puisse écrire un seul
 * {@code catch (BanqueException e)} et afficher {@code e.getMessage()},
 * au lieu d'énumérer les cinq exceptions concrètes à chaque bouton.
 */
public abstract class BanqueException extends RuntimeException {

    protected BanqueException(String message) {
        super(message);
    }
}
