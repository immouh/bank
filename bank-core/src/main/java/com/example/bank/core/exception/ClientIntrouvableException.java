package com.example.bank.core.exception;

/** Aucun client ne correspond au RIB, au nom ou aux identifiants fournis. */
public class ClientIntrouvableException extends BanqueException {

    public ClientIntrouvableException(String message) {
        super(message);
    }

    public static ClientIntrouvableException parRib(int rib) {
        return new ClientIntrouvableException("Aucun client ne correspond au RIB " + rib + ".");
    }
}
