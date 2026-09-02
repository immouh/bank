package com.example.bank.core.exception.existence;

import com.example.bank.core.exception.BanqueException;

/**
 * Catégorie : l'objet visé n'existe pas — ou existe déjà alors qu'on le crée.
 *
 * Client introuvable, Livret A absent, second Livret A refusé. Les deux faces
 * d'une même question : la présence de l'entité ne correspond pas à ce que
 * l'opération suppose.
 */
public abstract class ErreurExistenceException extends BanqueException {

    protected ErreurExistenceException(String message) {
        super(message);
    }
}
