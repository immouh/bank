package com.example.bank.web.dto;

import java.math.BigDecimal;

/**
 * Corps de {@code POST /api/virements}.
 *
 * IL N'Y A PAS DE {@code ribEmetteur} : l'émetteur est TOUJOURS le porteur du
 * jeton. L'accepter en paramètre laisserait un client authentifié débiter le
 * compte d'un autre — le champ absent est ici une décision de sécurité, pas
 * un oubli.
 */
public record VirementRequete(Integer ribDestinataire, BigDecimal montant) {
}
