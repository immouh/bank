package com.example.bank.core.service.commande;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Point de passage unique des commandes : il les exécute et garde la trace de
 * celles qui ont abouti.
 *
 * POURQUOI CE JOURNAL — c'est ce qui rendra possible, sans retoucher ni les
 * fenêtres ni le service, un historique des opérations d'une session, un
 * rejeu, ou un undo (les commandes n'auraient alors qu'à savoir s'annuler).
 * L'annulation n'est PAS implémentée ici : rien ne le demande aujourd'hui, et
 * une pile d'undo dont personne ne se sert serait du code mort.
 *
 * SEULES LES COMMANDES ABOUTIES SONT JOURNALISÉES. Une commande qui a levé
 * une exception n'a rien changé : la journaliser laisserait croire à une
 * opération passée, et un rejeu du journal rejouerait des échecs.
 */
public class InvocateurCommande {

    private final List<Commande> journal = new ArrayList<>();

    /**
     * Exécute la commande et la journalise si elle aboutit.
     *
     * @throws com.example.bank.core.exception.BanqueException laissée remonter
     *         telle quelle jusqu'à l'IHM, qui l'affiche
     */
    public void executer(Commande commande) {
        Objects.requireNonNull(commande, "La commande est obligatoire.");
        commande.executer();
        journal.add(commande);
    }

    /** Commandes abouties, dans l'ordre d'exécution. Vue non modifiable. */
    public List<Commande> getJournal() {
        return Collections.unmodifiableList(journal);
    }

    /** Nombre de commandes exécutées avec succès. */
    public int nombreExecutees() {
        return journal.size();
    }

    /** Vide le journal (changement de session, déconnexion). */
    public void viderJournal() {
        journal.clear();
    }
}
