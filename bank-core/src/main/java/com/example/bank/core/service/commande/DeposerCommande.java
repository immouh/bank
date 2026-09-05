package com.example.bank.core.service.commande;

import com.example.bank.core.model.Client;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.audit.EvenementAudit;

import java.math.BigDecimal;

/** Dépôt sur le compte courant d'un client. */
public class DeposerCommande extends OperationSimpleCommande {

    public DeposerCommande(BanqueService banqueService, Client client, BigDecimal montant) {
        super(banqueService, client, montant);
    }

    @Override
    protected void appliquer(BanqueService service, Client client, BigDecimal montant) {
        service.deposer(client, montant);
    }

    @Override
    protected String verbe() {
        return "Dépôt";
    }

    @Override
    public EvenementAudit evenementAudit() {
        return EvenementAudit.DEPOT;
    }
}
