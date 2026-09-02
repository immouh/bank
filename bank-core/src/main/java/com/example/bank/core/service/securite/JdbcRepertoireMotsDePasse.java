package com.example.bank.core.service.securite;

import com.example.bank.core.exception.technique.PersistanceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * Mots de passe hachés rangés dans la colonne {@code clients.mot_de_passe}.
 *
 * La colonne est la même qu'en phase C, son contenu a changé : un haché
 * BCrypt au lieu du mot de passe en clair. Ce répertoire est le SEUL endroit
 * du projet qui la lit ou l'écrit.
 */
public class JdbcRepertoireMotsDePasse implements RepertoireMotsDePasse {

    private final Connection connexion;

    public JdbcRepertoireMotsDePasse(Connection connexion) {
        this.connexion = Objects.requireNonNull(connexion, "La connexion est obligatoire.");
    }

    @Override
    public Optional<String> hachePour(int rib) {
        String sql = "SELECT mot_de_passe FROM clients WHERE rib = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            try (ResultSet lignes = requete.executeQuery()) {
                return lignes.next()
                        ? Optional.ofNullable(lignes.getString("mot_de_passe"))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistanceException("Lecture des identifiants impossible.", e);
        }
    }

    @Override
    public void enregistrer(int rib, String hache) {
        String sql = "UPDATE clients SET mot_de_passe = ? WHERE rib = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, hache);
            requete.setInt(2, rib);
            if (requete.executeUpdate() == 0) {
                throw new PersistanceException("Client " + rib + " inconnu en base.");
            }
        } catch (SQLException e) {
            throw new PersistanceException("Écriture des identifiants impossible.", e);
        }
    }
}
