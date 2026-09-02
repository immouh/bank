package com.example.bank.core.service.securite;

import com.example.bank.core.exception.technique.PersistanceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Compteur d'échecs persistant : le verrouillage survit au redémarrage de
 * l'application.
 *
 * Un registre volatil se contournerait en relançant le programme tous les
 * trois essais — ce qui reviendrait à n'avoir aucun verrouillage.
 */
public class JdbcRegistreTentatives implements RegistreTentatives {

    private final Connection connexion;
    private final Clock horloge;

    public JdbcRegistreTentatives(Connection connexion) {
        this(connexion, Clock.systemDefaultZone());
    }

    public JdbcRegistreTentatives(Connection connexion, Clock horloge) {
        this.connexion = Objects.requireNonNull(connexion, "La connexion est obligatoire.");
        this.horloge = horloge;
    }

    @Override
    public void enregistrerEchec(int rib) {
        int echecs = lire(rib).map(Compteur::echecs).orElse(0) + 1;
        String sql = "MERGE INTO tentatives_connexion (client_rib, echecs, derniere_tentative) "
                + "KEY (client_rib) VALUES (?, ?, ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            requete.setInt(2, echecs);
            requete.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now(horloge)));
            requete.executeUpdate();
        } catch (SQLException e) {
            throw new PersistanceException("Enregistrement de la tentative impossible.", e);
        }
    }

    @Override
    public void reinitialiser(int rib) {
        try (PreparedStatement requete = connexion.prepareStatement(
                "DELETE FROM tentatives_connexion WHERE client_rib = ?")) {
            requete.setInt(1, rib);
            requete.executeUpdate();
        } catch (SQLException e) {
            throw new PersistanceException("Remise à zéro des tentatives impossible.", e);
        }
    }

    @Override
    public boolean estVerrouille(int rib) {
        Optional<Compteur> compteur = lire(rib);
        if (compteur.isEmpty() || compteur.get().echecs() < ECHECS_AVANT_VERROUILLAGE) {
            return false;
        }
        if (Verrouillage.expire(compteur.get().derniereTentative(), horloge)) {
            reinitialiser(rib);
            return false;
        }
        return true;
    }

    @Override
    public int echecs(int rib) {
        return lire(rib).map(Compteur::echecs).orElse(0);
    }

    @Override
    public int minutesRestantes(int rib) {
        if (!estVerrouille(rib)) {
            return 0;
        }
        return Verrouillage.minutesRestantes(lire(rib).orElseThrow().derniereTentative(), horloge);
    }

    private Optional<Compteur> lire(int rib) {
        String sql = "SELECT echecs, derniere_tentative FROM tentatives_connexion WHERE client_rib = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            try (ResultSet lignes = requete.executeQuery()) {
                if (!lignes.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Compteur(lignes.getInt("echecs"),
                        lignes.getTimestamp("derniere_tentative").toLocalDateTime()));
            }
        } catch (SQLException e) {
            throw new PersistanceException("Lecture des tentatives impossible.", e);
        }
    }

    private record Compteur(int echecs, LocalDateTime derniereTentative) {
    }
}
