package com.example.bank.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Une ligne d'historique telle qu'elle est STOCKÉE, donc avec son horodatage
 * réel — c'est tout l'intérêt de passer par {@code LigneHistorique} du coeur
 * plutôt que par {@code Transaction}, dont le constructeur refixe la date à
 * l'instant de la relecture.
 */
public record LigneHistoriqueReponse(int numeroOrdre,
                                     String type,
                                     BigDecimal montant,
                                     String description,
                                     LocalDateTime horodatage) {
}
