package com.example.bank.swing.controller;

import com.example.bank.core.model.Client;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.commande.DeposerCommande;
import com.example.bank.core.service.commande.InvocateurCommande;
import com.example.bank.core.service.commande.RetirerCommande;

import java.math.BigDecimal;

/**
 * Opérations de compte pilotées depuis l'interface graphique.
 *
 * {@link #rafraichir(Client)} est la clé de la correction du bug des soldes
 * figés : la fenêtre ne conserve plus de copie des montants, elle redemande
 * l'état du client après chaque opération réussie.
 *
 * PATRON COMMAND — le controller n'appelle plus {@code BanqueService}
 * directement : il CONSTRUIT une commande et la confie à l'invocateur, qui
 * l'exécute et la journalise. Les exceptions métier remontent inchangées
 * jusqu'à la fenêtre, qui les affiche : le câblage des boutons n'a rien à
 * changer.
 */
public class CompteController {

    private final BanqueService banqueService;
    private final InvocateurCommande invocateur;

    public CompteController(BanqueService banqueService, InvocateurCommande invocateur) {
        this.banqueService = banqueService;
        this.invocateur = invocateur;
    }

    public void deposer(Client client, BigDecimal montant) {
        invocateur.executer(new DeposerCommande(banqueService, client, montant));
    }

    public void retirer(Client client, BigDecimal montant) {
        invocateur.executer(new RetirerCommande(banqueService, client, montant));
    }

    /** Relit l'état à jour du client en repassant par le stockage. */
    public Client rafraichir(Client client) {
        return banqueService.rechercherParRib(client.getRib());
    }
}
