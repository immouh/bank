package com.example.bank.web.dto;

/**
 * Ce que rend une connexion réussie.
 *
 * NI MOT DE PASSE NI HACHÉ, évidemment — mais pas de solde non plus : le
 * jeton sert à appeler {@code /api/comptes/moi}, qui est le seul endroit d'où
 * la situation du compte doit venir. Un client n'a pas à choisir entre deux
 * sources pour la même information.
 */
public record ConnexionReponse(String token,
                               String typeToken,
                               long expireDansSecondes,
                               int rib,
                               String nom) {
}
