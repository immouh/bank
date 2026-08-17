package com.example.bank.core.service;

import com.example.bank.core.exception.ClientIntrouvableException;
import com.example.bank.core.exception.LivretAAbsentException;
import com.example.bank.core.exception.VirementVersSoiMemeException;
import com.example.bank.core.model.Client;
import com.example.bank.core.model.Montants;
import com.example.bank.core.model.Transaction;
import com.example.bank.core.repository.ClientRepository;

import java.math.BigDecimal;

/**
 * Orchestration des opérations bancaires.
 *
 * RÈGLE DE PLACEMENT — tout ce qui coordonne PLUSIEURS objets métier (un
 * virement touche l'émetteur ET le destinataire) ou qui doit parler au
 * repository vit ici. Le service ne recalcule jamais un solde lui-même : il
 * appelle les méthodes du modèle, qui restent seules propriétaires de leur état.
 */
public class BanqueService {

    private final ClientRepository repository;

    public BanqueService(ClientRepository repository) {
        this.repository = repository;
    }

    /** Dépôt sur le compte courant. */
    public void deposer(Client client, BigDecimal montant) {
        exigerClient(client);
        BigDecimal m = Montants.exigerPositif(montant);

        client.crediter(m);
        client.ajouterTransaction(new Transaction(m, Transaction.TypeTransaction.DEPOT,
                "Dépôt effectué avec succès"));
        repository.save(client);
    }

    /** Retrait sur le compte courant, dans la limite du solde disponible. */
    public void retirer(Client client, BigDecimal montant) {
        exigerClient(client);
        BigDecimal m = Montants.exigerPositif(montant);

        client.debiter(m);
        client.ajouterTransaction(new Transaction(m, Transaction.TypeTransaction.RETRAIT,
                "Retrait effectué avec succès"));
        repository.save(client);
    }

    /** Ouvre le Livret A du client (solde initial à zéro). */
    public void creerLivretA(Client client) {
        exigerClient(client);

        client.ouvrirLivretA();
        repository.save(client);
    }

    /**
     * VIREMENT INTERNE : compte courant -> Livret A du même client.
     * L'épargne est alimentée par le compte courant, jamais l'inverse.
     */
    public void virerVersLivretA(Client client, BigDecimal montant) {
        exigerClient(client);
        BigDecimal m = Montants.exigerPositif(montant);
        // Vérifié AVANT le débit : sinon un client sans Livret A verrait son
        // compte débité par une opération qui échoue juste après.
        if (!client.isLivretAExiste()) {
            throw new LivretAAbsentException();
        }

        client.debiter(m);          // lève SoldeInsuffisantException
        client.crediterLivretA(m);
        client.ajouterTransaction(new Transaction(m, Transaction.TypeTransaction.VIREMENT,
                "Virement du compte courant vers le Livret A"));
        repository.save(client);
    }

    /**
     * VIREMENT EXTERNE : compte courant de l'émetteur -> compte courant du
     * destinataire. Les deux parties reçoivent une ligne d'historique.
     *
     * Cette méthode est l'exemple type de ce qui ne peut PAS vivre dans le
     * modèle : elle touche deux Client à la fois.
     */
    public void virer(Client emetteur, Client destinataire, BigDecimal montant) {
        exigerClient(emetteur);
        if (destinataire == null) {
            throw new ClientIntrouvableException("Destinataire introuvable.");
        }
        if (emetteur.getRib() == destinataire.getRib()) {
            throw new VirementVersSoiMemeException();
        }
        BigDecimal m = Montants.exigerPositif(montant);

        emetteur.debiter(m);        // lève SoldeInsuffisantException avant toute écriture
        destinataire.crediter(m);

        emetteur.ajouterTransaction(new Transaction(m, Transaction.TypeTransaction.VIREMENT,
                "Virement émis vers " + destinataire.getNom()
                        + " (RIB " + destinataire.getRib() + ")"));
        destinataire.ajouterTransaction(new Transaction(m, Transaction.TypeTransaction.VIREMENT,
                "Virement reçu de " + emetteur.getNom()
                        + " (RIB " + emetteur.getRib() + ")"));

        repository.save(emetteur);
        repository.save(destinataire);
    }

    /** Relit l'état à jour d'un client depuis le stockage. */
    public Client rechercherParRib(int rib) {
        return repository.findByRib(rib)
                .orElseThrow(() -> ClientIntrouvableException.parRib(rib));
    }

    private void exigerClient(Client client) {
        if (client == null) {
            throw new ClientIntrouvableException("Client introuvable.");
        }
    }
}
