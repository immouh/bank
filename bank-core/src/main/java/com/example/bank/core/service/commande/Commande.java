package com.example.bank.core.service.commande;

import com.example.bank.core.service.audit.EvenementAudit;

/**
 * Une opération bancaire transformée en objet.
 *
 * PATRON COMMAND — l'appelant (une fenêtre, un bouton, plus tard un
 * ordonnanceur ou un import de fichier) ne sait plus QUOI est exécuté, il
 * manipule des {@code Commande}. Ce qui devient possible une fois l'opération
 * réifiée : la journaliser, la rejouer, la mettre en file, l'annuler.
 *
 * Une commande ORCHESTRE, elle ne recalcule rien : les règles métier restent
 * dans {@code BanqueService}, qu'elle se contente d'appeler. Dupliquer ici la
 * validation des montants ou le contrôle du solde ferait exactement le
 * doublon que le service existe pour éviter.
 */
public interface Commande {

    /**
     * Exécute l'opération.
     *
     * @throws com.example.bank.core.exception.BanqueException si le métier la refuse
     */
    void executer();

    /** Description lisible, pour le journal d'exécution et l'IHM. */
    String libelle();

    /**
     * RIB du client principalement concerné — celui qui est débité pour un
     * retrait ou un virement, crédité pour un dépôt.
     *
     * Sert au journal d'audit, qui doit pouvoir dire QUI a fait quoi. La
     * commande est le seul endroit qui le sache sans que l'invocateur ait à
     * deviner le type d'opération qu'il exécute.
     */
    int ribConcerne();

    /** Nature de l'événement à consigner au journal d'audit. */
    EvenementAudit evenementAudit();
}
