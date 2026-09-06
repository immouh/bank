package com.example.bank.web.persistance;

import com.example.bank.core.exception.technique.PersistanceException;
import com.example.bank.core.model.Client;
import com.example.bank.core.model.Transaction;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * AJOUTE les lignes d'historique produites par une requête, sans toucher aux
 * précédentes.
 *
 * <h2>Pourquoi cette classe existe</h2>
 *
 * {@code JdbcClientRepository.enregistrerHistorique(client)} EFFACE l'historique
 * stocké puis le réécrit depuis la mémoire du {@code Client}. C'est le bon
 * comportement pour Swing, où le même objet {@code Client} vit toute la
 * session et porte donc l'historique complet. Ce n'est PAS utilisable depuis
 * une API stateless : chaque requête relit un client neuf, dont l'historique
 * en mémoire est vide (le repository ne le recharge pas, et il ne peut pas —
 * {@code Transaction} refixe sa date à {@code now()} à la construction, si
 * bien qu'un rechargement inventerait des horodatages). Appeler
 * {@code enregistrerHistorique} après un dépôt EFFACERAIT donc tout le passé
 * du client pour n'y laisser que ce dépôt.
 *
 * Cette classe fait ce que le coeur ne sait pas encore faire : un AJOUT. Elle
 * ne supprime jamais rien et continue la numérotation là où la table s'est
 * arrêtée.
 *
 * <h2>Pourquoi ici et pas dans le coeur</h2>
 *
 * Parce que cette phase ne modifie pas {@code bank-core}. La vraie correction
 * est côté coeur — une méthode {@code ajouterAuHistorique(Client, int)} sur
 * {@code JdbcClientRepository}, ou mieux, un {@code Transaction} qui accepte
 * son horodatage, ce que {@code LigneHistorique} annonce déjà comme « un
 * ajout de trois lignes au modèle ». Le jour où ce sera fait, cette classe
 * disparaît et les contrôleurs appellent le repository.
 */
@Component
public class HistoriquePersistant {

    private final Connection connexion;

    public HistoriquePersistant(Connection connexion) {
        this.connexion = connexion;
    }

    /**
     * Écrit les transactions du client à partir de l'indice donné.
     *
     * @param aPartirDe indice de la première transaction à écrire dans
     *                  {@code client.getHistorique()} — les précédentes sont
     *                  déjà en base et ne doivent pas être réécrites
     */
    public void ajouter(Client client, int aPartirDe) {
        List<Transaction> historique = client.getHistorique();
        if (aPartirDe >= historique.size()) {
            return;
        }
        int ordre = prochainOrdre(client.getRib());

        String sql = "INSERT INTO transactions "
                + "(client_rib, numero_ordre, type, montant, description, horodatage) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            for (int i = aPartirDe; i < historique.size(); i++) {
                Transaction transaction = historique.get(i);
                requete.setInt(1, client.getRib());
                requete.setInt(2, ordre++);
                requete.setString(3, transaction.getType().name());
                requete.setBigDecimal(4, transaction.getMontant());
                requete.setString(5, transaction.getDescription());
                requete.setTimestamp(6, Timestamp.valueOf(transaction.getDate()));
                requete.addBatch();
            }
            requete.executeBatch();
        } catch (SQLException e) {
            throw new PersistanceException("Écriture de l'historique impossible.", e);
        }
    }

    /**
     * Numéro d'ordre libre suivant. La colonne porte une contrainte d'unicité
     * {@code (client_rib, numero_ordre)} : repartir de zéro ferait échouer
     * l'insertion au lieu d'écraser silencieusement.
     */
    private int prochainOrdre(int rib) {
        String sql = "SELECT COALESCE(MAX(numero_ordre) + 1, 0) FROM transactions "
                + "WHERE client_rib = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            try (ResultSet lignes = requete.executeQuery()) {
                lignes.next();
                return lignes.getInt(1);
            }
        } catch (SQLException e) {
            throw new PersistanceException("Lecture de l'historique impossible.", e);
        }
    }
}
