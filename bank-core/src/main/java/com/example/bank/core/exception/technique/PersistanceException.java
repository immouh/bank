package com.example.bank.core.exception.technique;

import com.example.bank.core.exception.BanqueException;

/**
 * Le stockage n'a pas pu répondre : base injoignable, écriture refusée,
 * données illisibles.
 *
 * Placée dans la hiérarchie {@link BanqueException} pour une raison
 * pratique : l'IHM n'attrape que celle-là. Une panne de base doit produire un
 * message à l'écran (« opération impossible, réessayez »), pas une trace
 * technique dans la console pendant que l'utilisateur croit son virement
 * passé.
 *
 * Elle conserve la {@code SQLException} d'origine en cause, pour le
 * diagnostic — mais jamais le SQL ni les valeurs, qui pourraient contenir
 * des données sensibles.
 */
public class PersistanceException extends ErreurTechniqueException {

    public PersistanceException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public PersistanceException(String message) {
        super(message);
    }
}
