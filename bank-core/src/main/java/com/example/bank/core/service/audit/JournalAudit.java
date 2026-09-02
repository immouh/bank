package com.example.bank.core.service.audit;

import java.util.List;

/**
 * Trace des événements sensibles : qui, quoi, quand.
 *
 * PUREMENT DESCRIPTIF — le journal n'autorise ni ne refuse jamais rien. C'est
 * ce qui permet d'y écrire sans crainte depuis n'importe où : une ligne
 * d'audit ne peut pas changer le déroulement d'une opération. Le verrouillage,
 * lui, est décidé par {@code RegistreTentatives}, qui est un tout autre objet.
 */
public interface JournalAudit {

    /** Consigne un événement, avec un détail libre (peut être {@code null}). */
    void enregistrer(int rib, EvenementAudit evenement, String detail);

    /** Consigne un événement sans détail. */
    default void enregistrer(int rib, EvenementAudit evenement) {
        enregistrer(rib, evenement, null);
    }

    /** Événements consignés pour un client, du plus ancien au plus récent. */
    List<LigneAudit> pour(int rib);
}
