package com.example.bank.core.exception.validation;

import com.example.bank.core.exception.BanqueException;

/**
 * Catégorie : la SAISIE est refusée avant même de regarder l'état du compte.
 *
 * Un montant négatif, une durée nulle, un virement vers soi-même : la demande
 * est mal formée, et elle le resterait quel que soit le solde ou le moment.
 * C'est ce qui la distingue de {@code ErreurEtatException}, où la demande est
 * correcte mais l'état courant la refuse.
 */
public abstract class ErreurValidationException extends BanqueException {

    protected ErreurValidationException(String message) {
        super(message);
    }
}
