package com.example.bank.core.service.audit;

import java.time.LocalDateTime;

/** Une ligne du journal d'audit, telle qu'elle est consignée. */
public record LigneAudit(int rib,
                         EvenementAudit evenement,
                         LocalDateTime horodatage,
                         String detail) {
}
