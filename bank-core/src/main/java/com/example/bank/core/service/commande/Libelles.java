package com.example.bank.core.service.commande;

import com.example.bank.core.model.Client;

/**
 * Fragments de texte partagés par les libellés de commandes.
 *
 * POURQUOI PAS DANS {@code Commande} — l'interface est le CONTRAT que voient
 * l'invocateur et les controllers ; le repli sur « ? » pour un client absent
 * est un détail de mise en forme qui n'a rien à y faire. Les trois commandes
 * en portaient chacune leur copie, à l'identique.
 */
final class Libelles {

    private Libelles() {
    }

    /**
     * Nom du client, ou {@code ?} s'il n'y en a pas.
     *
     * Une commande se CONSTRUIT sans rien valider — c'est le service qui
     * refuse un client nul, à l'exécution. Son libellé doit donc rester
     * affichable avant même qu'elle ait été exécutée, y compris pour une
     * commande que le service rejettera.
     */
    static String nom(Client client) {
        return client == null ? "?" : client.getNom();
    }
}
