package com.example.bank.core.exception;

/**
 * L'état courant d'une entité (compte, carte, prêt) refuse l'opération ou la
 * transition demandée.
 *
 * Distincte de {@link SoldeInsuffisantException} : là, le compte pouvait
 * opérer mais n'avait pas les fonds ; ici, l'opération est refusée par
 * principe, quel que soit le montant — un compte fermé refuse un dépôt de
 * 1 € comme de 1 000 €.
 */
public class OperationInterditeException extends BanqueException {

    public OperationInterditeException(String message) {
        super(message);
    }

    /** L'opération métier est refusée par l'état courant. */
    public static OperationInterditeException operation(String operation, String etat) {
        return new OperationInterditeException(
                "Opération « " + operation + " » impossible : " + etat + ".");
    }

    /** La transition demandée n'existe pas depuis l'état courant. */
    public static OperationInterditeException transition(String transition, String etat) {
        return new OperationInterditeException(
                "Transition « " + transition + " » impossible : " + etat + ".");
    }
}
