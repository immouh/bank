package com.example.bank.swing.controller;

import com.example.bank.core.model.Client;
import com.example.bank.core.service.BanqueService;

import java.math.BigDecimal;

/**
 * Opérations de compte pilotées depuis l'interface graphique.
 *
 * {@link #rafraichir(Client)} est la clé de la correction du bug des soldes
 * figés : la fenêtre ne conserve plus de copie des montants, elle redemande
 * l'état du client après chaque opération réussie.
 */
public class CompteController {

    private final BanqueService banqueService;

    public CompteController(BanqueService banqueService) {
        this.banqueService = banqueService;
    }

    public void deposer(Client client, BigDecimal montant) {
        banqueService.deposer(client, montant);
    }

    public void retirer(Client client, BigDecimal montant) {
        banqueService.retirer(client, montant);
    }

    /** Relit l'état à jour du client en repassant par le stockage. */
    public Client rafraichir(Client client) {
        return banqueService.rechercherParRib(client.getRib());
    }
}
