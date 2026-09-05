package com.example.bank.core.service.commande;

import com.example.bank.core.model.Client;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.audit.EvenementAudit;

import java.math.BigDecimal;

/** Retrait sur le compte courant d'un client. */
public class RetirerCommande extends OperationSimpleCommande {

    public RetirerCommande(BanqueService banqueService, Client client, BigDecimal montant) {
        super(banqueService, client, montant);
    }

    @Override
    protected void appliquer(BanqueService service, Client client, BigDecimal montant) {
        service.retirer(client, montant);
    }

    @Override
    protected String verbe() {
        return "Retrait";
    }

    @Override
    public EvenementAudit evenementAudit() {
        return EvenementAudit.RETRAIT;
    }
}
