package com.example.bank.core.exception.etat;

import com.example.bank.core.exception.BanqueException;

/**
 * Catégorie : la demande est bien formée, mais l'ÉTAT COURANT la refuse.
 *
 * Solde insuffisant, plafond de carte atteint, compte verrouillé, carte
 * bloquée : la même demande passerait demain, ou sur un autre compte. C'est ce
 * qui la distingue de {@code ErreurValidationException}, où la demande est
 * mal formée en elle-même.
 */
public abstract class ErreurEtatException extends BanqueException {

    protected ErreurEtatException(String message) {
        super(message);
    }
}
