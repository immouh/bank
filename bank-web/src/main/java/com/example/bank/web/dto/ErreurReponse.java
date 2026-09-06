package com.example.bank.web.dto;

/**
 * Corps JSON de TOUTE erreur de l'API : {@code {"erreur": "..."}}.
 *
 * Un format unique, quelle que soit la couche qui refuse — règle métier,
 * jeton absent, panne technique. Un client n'a alors qu'une seule forme à
 * savoir lire, et la trace Java ne sort jamais du serveur.
 */
public record ErreurReponse(String erreur) {
}
