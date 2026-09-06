package com.example.bank.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Réponse de {@code GET /api/comptes/moi} : l'identité du porteur du jeton et
 * ses comptes.
 *
 * Le mot de passe n'y figure pas, et ne PEUT pas y figurer : {@code Client}
 * n'expose aucun accesseur qui le rende (un test du coeur l'interdit depuis
 * la phase A), et ce record est construit champ par champ.
 */
public record SituationReponse(int rib,
                               String nom,
                               BigDecimal soldeTotal,
                               List<CompteReponse> comptes) {
}
