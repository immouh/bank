package com.example.bank.core.service.commande;

import com.example.bank.core.model.Client;
import com.example.bank.core.service.BanqueService;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Socle des opérations à UN SEUL client : dépôt et retrait.
 *
 * PATRON PATRON DE MÉTHODE — les deux commandes ne différaient que par trois
 * choses : la méthode du service à appeler, l'événement d'audit à consigner et
 * le mot qui ouvre le libellé. Tout le reste — les trois champs, le contrôle
 * du service, {@code ribConcerne()}, la forme du libellé — était écrit deux
 * fois mot pour mot. Il l'est désormais ici, une fois.
 *
 * LE VIREMENT RESTE DEHORS, volontairement : il porte DEUX clients, son RIB
 * concerné est celui de l'émetteur et son libellé nomme les deux parties. Le
 * faire descendre de cette classe demanderait d'y remonter des notions qui
 * n'ont pas de sens pour un dépôt.
 */
abstract class OperationSimpleCommande implements Commande {

    private final BanqueService banqueService;
    private final Client client;
    private final BigDecimal montant;

    protected OperationSimpleCommande(BanqueService banqueService, Client client,
                                      BigDecimal montant) {
        this.banqueService = Objects.requireNonNull(banqueService, "Le service est obligatoire.");
        this.client = client;
        this.montant = montant;
    }

    /**
     * L'opération elle-même, sur le service.
     *
     * Le montant n'est PAS validé avant l'appel : {@code BanqueService} le
     * fait déjà, et le valider deux fois laisserait deux règles à maintenir.
     */
    protected abstract void appliquer(BanqueService service, Client client, BigDecimal montant);

    /** Mot qui ouvre le libellé : « Dépôt », « Retrait ». */
    protected abstract String verbe();

    @Override
    public final void executer() {
        appliquer(banqueService, client, montant);
    }

    @Override
    public final int ribConcerne() {
        return client == null ? 0 : client.getRib();
    }

    @Override
    public final String libelle() {
        return verbe() + " de " + montant + " € sur le compte de " + Libelles.nom(client);
    }
}
