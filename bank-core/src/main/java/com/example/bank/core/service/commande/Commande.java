package com.example.bank.core.service.commande;

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
}
