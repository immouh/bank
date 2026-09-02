package com.example.bank.core.exception.validation;

/**
 * Durée de prêt nulle ou négative.
 *
 * Une durée non positive n'a pas de sens dans la formule d'amortissement :
 * elle ferait diviser par zéro le nombre de mensualités. Le refus est une
 * règle de saisie, pas un accident technique — d'où sa place sous
 * {@link ErreurValidationException}, donc sous la racine que l'IHM attrape.
 */
public class DureeInvalideException extends ErreurValidationException {

    public DureeInvalideException(String message) {
        super(message);
    }

    public DureeInvalideException() {
        super("La durée du prêt doit être strictement positive.");
    }
}
