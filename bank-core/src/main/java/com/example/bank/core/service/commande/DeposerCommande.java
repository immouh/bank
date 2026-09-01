package com.example.bank.core.service.commande;

import com.example.bank.core.model.Client;
import com.example.bank.core.service.BanqueService;

import java.math.BigDecimal;
import java.util.Objects;

/** Dépôt sur le compte courant d'un client. */
public class DeposerCommande implements Commande {

    private final BanqueService banqueService;
    private final Client client;
    private final BigDecimal montant;

    public DeposerCommande(BanqueService banqueService, Client client, BigDecimal montant) {
        this.banqueService = Objects.requireNonNull(banqueService, "Le service est obligatoire.");
        this.client = client;
        this.montant = montant;
    }

    /**
     * Le montant n'est PAS validé ici : {@code BanqueService.deposer} le fait
     * déjà, et le valider deux fois laisserait deux règles à maintenir.
     */
    @Override
    public void executer() {
        banqueService.deposer(client, montant);
    }

    @Override
    public String libelle() {
        return "Dépôt de " + montant + " € sur le compte de " + nomClient();
    }

    private String nomClient() {
        return client == null ? "?" : client.getNom();
    }
}
