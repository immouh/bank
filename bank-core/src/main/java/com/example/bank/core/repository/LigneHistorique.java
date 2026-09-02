package com.example.bank.core.repository;

import com.example.bank.core.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Une ligne d'historique telle qu'elle est STOCKÉE, avec son horodatage réel.
 *
 * POURQUOI CE TYPE PLUTÔT QU'UNE {@link Transaction} — le constructeur de
 * {@code Transaction} fixe lui-même la date à {@code LocalDateTime.now()} et
 * n'accepte pas d'horodatage. Relire l'historique en fabriquant des
 * {@code Transaction} donnerait donc à toutes les opérations passées la date
 * du redémarrage : des données fausses, présentées comme vraies.
 *
 * Ce type de lecture rend les vraies dates, sans mentir. Il disparaîtra le
 * jour où {@code Transaction} acceptera son horodatage à la construction —
 * un ajout de trois lignes au modèle, hors du périmètre de cette phase.
 */
public record LigneHistorique(int clientRib,
                              int numeroOrdre,
                              Transaction.TypeTransaction type,
                              BigDecimal montant,
                              String description,
                              LocalDateTime horodatage) {
}
