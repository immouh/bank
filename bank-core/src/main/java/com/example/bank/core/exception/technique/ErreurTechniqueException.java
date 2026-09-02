package com.example.bank.core.exception.technique;

import com.example.bank.core.exception.BanqueException;

/**
 * Catégorie : la panne ou le bug, pas la règle métier.
 *
 * Base injoignable, paramètre interne absent, invariant rompu. L'utilisateur
 * n'y peut rien et ne peut rien corriger dans sa saisie — mais ces erreurs
 * restent sous {@link BanqueException} pour une raison pratique : l'IHM
 * n'attrape que celle-là, et une panne doit produire un message à l'écran
 * plutôt qu'une trace dans la console pendant que l'utilisateur croit son
 * opération passée.
 */
public abstract class ErreurTechniqueException extends BanqueException {

    protected ErreurTechniqueException(String message) {
        super(message);
    }
}
