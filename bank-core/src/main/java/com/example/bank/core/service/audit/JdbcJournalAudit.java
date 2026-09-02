package com.example.bank.core.service.audit;

import com.example.bank.core.exception.technique.PersistanceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Journal d'audit persistant.
 *
 * EN AJOUT SEUL — aucune méthode ne modifie ni ne supprime une ligne. Un
 * journal qu'on peut réécrire ne prouve rien.
 */
public class JdbcJournalAudit implements JournalAudit {

    private final Connection connexion;
    private final Clock horloge;

    public JdbcJournalAudit(Connection connexion) {
        this(connexion, Clock.systemDefaultZone());
    }

    public JdbcJournalAudit(Connection connexion, Clock horloge) {
        this.connexion = Objects.requireNonNull(connexion, "La connexion est obligatoire.");
        this.horloge = horloge;
    }

    @Override
    public void enregistrer(int rib, EvenementAudit evenement, String detail) {
        String sql = "INSERT INTO journal_audit (client_rib, evenement, horodatage, detail) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            requete.setString(2, evenement.name());
            requete.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now(horloge)));
            requete.setString(4, detail);
            requete.executeUpdate();
        } catch (SQLException e) {
            throw new PersistanceException("Écriture au journal d'audit impossible.", e);
        }
    }

    @Override
    public List<LigneAudit> pour(int rib) {
        String sql = "SELECT client_rib, evenement, horodatage, detail "
                + "FROM journal_audit WHERE client_rib = ? ORDER BY id";
        List<LigneAudit> lignes = new ArrayList<>();
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            try (ResultSet resultat = requete.executeQuery()) {
                while (resultat.next()) {
                    lignes.add(new LigneAudit(
                            resultat.getInt("client_rib"),
                            EvenementAudit.valueOf(resultat.getString("evenement")),
                            resultat.getTimestamp("horodatage").toLocalDateTime(),
                            resultat.getString("detail")));
                }
            }
            return lignes;
        } catch (SQLException e) {
            throw new PersistanceException("Lecture du journal d'audit impossible.", e);
        }
    }
}
