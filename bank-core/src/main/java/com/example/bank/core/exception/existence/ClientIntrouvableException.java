package com.example.bank.core.exception.existence;

/** Aucun client ne correspond au RIB, au nom ou aux identifiants fournis. */
public class ClientIntrouvableException extends ErreurExistenceException {

    public ClientIntrouvableException(String message) {
        super(message);
    }

    public static ClientIntrouvableException parRib(int rib) {
        return new ClientIntrouvableException("Aucun client ne correspond au RIB " + rib + ".");
    }
}
