package com.example.bank.core.service.commande;

import com.example.bank.core.model.Client;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.audit.EvenementAudit;

import java.math.BigDecimal;
import java.util.Objects;

/** Retrait sur le compte courant d'un client. */
public class RetirerCommande implements Commande {

    private final BanqueService banqueService;
    private final Client client;
    private final BigDecimal montant;

    public RetirerCommande(BanqueService banqueService, Client client, BigDecimal montant) {
        this.banqueService = Objects.requireNonNull(banqueService, "Le service est obligatoire.");
        this.client = client;
        this.montant = montant;
    }

    @Override
    public void executer() {
        banqueService.retirer(client, montant);
    }

    @Override
    public int ribConcerne() {
        return client == null ? 0 : client.getRib();
    }

    @Override
    public EvenementAudit evenementAudit() {
        return EvenementAudit.RETRAIT;
    }

    @Override
    public String libelle() {
        return "Retrait de " + montant + " € sur le compte de " + nomClient();
    }

    private String nomClient() {
        return client == null ? "?" : client.getNom();
    }
}
