package com.example.bank.core.service.audit;

/** Nature d'un événement consigné au journal d'audit. */
public enum EvenementAudit {

    CONNEXION_REUSSIE,
    CONNEXION_ECHOUEE,
    COMPTE_VEROUILLE,
    DEPOT,
    RETRAIT,
    VIREMENT,
    OUVERTURE_LIVRET_A
}
