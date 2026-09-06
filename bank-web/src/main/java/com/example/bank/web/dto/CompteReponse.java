package com.example.bank.web.dto;

import java.math.BigDecimal;

/**
 * Un compte, vu de l'extérieur.
 *
 * POURQUOI PAS {@code Compte} DIRECTEMENT — Jackson sérialiserait tout ce qui
 * ressemble à un accesseur, y compris ce qu'on n'a pas choisi d'exposer, et
 * le JSON changerait de forme à chaque ajout de méthode au modèle. Un record
 * dédié fige le contrat : ce qui n'est pas listé ici ne sort pas de l'API.
 */
public record CompteReponse(String type,
                            boolean epargne,
                            BigDecimal solde,
                            BigDecimal decouvertAutorise,
                            String etat) {
}
