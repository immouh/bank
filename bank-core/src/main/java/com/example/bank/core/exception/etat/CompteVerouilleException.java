package com.example.bank.core.exception.etat;

/**
 * Trop de tentatives de connexion échouées : l'accès est suspendu.
 *
 * COMPROMIS ASSUMÉ — cette exception est DISTINCTE du message unique de
 * non-divulgation, et donc observable de l'extérieur : provoquer un
 * verrouillage révèle qu'un compte existe. C'est le prix du verrouillage, et
 * c'est le choix habituel des banques : un utilisateur bloqué doit comprendre
 * pourquoi il n'entre plus, sinon il croit à une panne et rappelle trois fois
 * le support. L'attaquant, lui, gagne l'existence d'un compte mais perd la
 * possibilité d'en tester le mot de passe.
 */
public class CompteVerouilleException extends ErreurEtatException {

    public CompteVerouilleException(int minutesRestantes) {
        super("Compte temporairement verrouillé après plusieurs tentatives échouées.\n"
                + "Réessayez dans " + minutesRestantes + " minute(s).");
    }
}
