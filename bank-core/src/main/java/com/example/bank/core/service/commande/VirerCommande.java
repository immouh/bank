package com.example.bank.core.service.commande;

import com.example.bank.core.model.Client;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.audit.EvenementAudit;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Virement du compte courant de l'émetteur vers celui du destinataire.
 *
 * La commande porte les deux clients, mais ne coordonne rien elle-même : le
 * double mouvement et les deux lignes d'historique restent l'affaire de
 * {@code BanqueService.virer}.
 */
public class VirerCommande implements Commande {

    private final BanqueService banqueService;
    private final Client emetteur;
    private final Client destinataire;
    private final BigDecimal montant;

    public VirerCommande(BanqueService banqueService, Client emetteur,
                         Client destinataire, BigDecimal montant) {
        this.banqueService = Objects.requireNonNull(banqueService, "Le service est obligatoire.");
        this.emetteur = emetteur;
        this.destinataire = destinataire;
        this.montant = montant;
    }

    @Override
    public void executer() {
        banqueService.virer(emetteur, destinataire, montant);
    }

    @Override
    public int ribConcerne() {
        return emetteur == null ? 0 : emetteur.getRib();
    }

    @Override
    public EvenementAudit evenementAudit() {
        return EvenementAudit.VIREMENT;
    }

    @Override
    public String libelle() {
        return "Virement de " + montant + " € de " + Libelles.nom(emetteur)
                + " vers " + Libelles.nom(destinataire);
    }
}
