package com.example.bank.core.repository;

import com.example.bank.core.exception.technique.PersistanceException;
import com.example.bank.core.model.Client;
import com.example.bank.core.model.Transaction;
import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.concret.OffreEtudianteFactory;
import com.example.bank.core.model.offre.concret.OffrePremiumFactory;
import com.example.bank.core.model.offre.concret.OffreStandardFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stockage des clients dans une base relationnelle, via JDBC.
 *
 * <h2>Ce que cette classe garantit</h2>
 *
 * <ul>
 *   <li><b>Aucune concaténation SQL.</b> Toute valeur passe par un
 *       {@link PreparedStatement}, y compris celles qui « viennent du
 *       code » : le jour où un RIB arrivera d'un formulaire, il n'y aura
 *       rien à revoir.</li>
 *   <li><b>Relecture fidèle.</b> Un client relu depuis la base retrouve ses
 *       comptes, leurs soldes ET leur état (actif, en découvert, bloqué,
 *       fermé), pas seulement un solde brut.</li>
 *   <li><b>Atomicité.</b> {@link #sauvegarderEnsemble(Client...)} écrit
 *       plusieurs clients dans une seule transaction SQL, avec rollback
 *       complet au moindre échec.</li>
 * </ul>
 *
 * <h2>Deux limites assumées, dues au modèle</h2>
 *
 * <p><b>1. Créer un client demande son mot de passe en paramètre.</b>
 * {@code Client} n'expose aucun accesseur de mot de passe — c'est
 * volontaire, et un test le verrouille. Ce repository ne peut donc pas le
 * lire dans l'objet pour l'insérer : {@link #save(Client)} met à jour un
 * client existant, la création passe par
 * {@link #creer(Client, String)}. Insérer avec un mot de passe vide « en
 * attendant » créerait des comptes sans mot de passe : hors de question.</p>
 *
 * <p><b>2. L'historique est écrit mais pas rechargé dans le
 * {@code Client}.</b> {@code Transaction} fixe sa date à la construction et
 * n'accepte pas d'horodatage ; recharger l'historique daterait toutes les
 * opérations passées du redémarrage. Voir {@link #enregistrerHistorique} et
 * {@link #historique(int)}, qui rendent les vraies dates.</p>
 */
public class JdbcClientRepository implements ClientRepository {

    // ------------------------------------------------------------------
    // CODES STOCKÉS — le mapping vit ici, en un seul endroit.
    // ------------------------------------------------------------------
    private static final String TYPE_ETUDIANT = "CompteEtudiant";
    private static final String TYPE_STANDARD = "CompteStandard";
    private static final String TYPE_PREMIUM = "ComptePremium";
    private static final String TYPE_LIVRET_A = "LivretA";

    private static final String ETAT_ACTIF = "ACTIF";
    private static final String ETAT_EN_DECOUVERT = "EN_DECOUVERT";
    private static final String ETAT_BLOQUE = "BLOQUE";
    private static final String ETAT_FERME = "FERME";

    private final Connection connexion;

    public JdbcClientRepository(Connection connexion) {
        this.connexion = Objects.requireNonNull(connexion, "La connexion est obligatoire.");
    }

    // ==================================================================
    // LECTURE
    // ==================================================================

    @Override
    public Optional<Client> findByRib(int rib) {
        String sql = "SELECT rib, nom, mot_de_passe FROM clients WHERE rib = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            return premierClient(requete);
        } catch (SQLException e) {
            throw new PersistanceException("Lecture du client impossible.", e);
        }
    }

    @Override
    public Optional<Client> findByNom(String nom) {
        String sql = "SELECT rib, nom, mot_de_passe FROM clients WHERE nom = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, nom);
            return premierClient(requete);
        } catch (SQLException e) {
            throw new PersistanceException("Lecture du client impossible.", e);
        }
    }

    @Override
    public List<Client> findAll() {
        String sql = "SELECT rib, nom, mot_de_passe FROM clients ORDER BY rib";
        List<Client> clients = new ArrayList<>();
        try (PreparedStatement requete = connexion.prepareStatement(sql);
             ResultSet lignes = requete.executeQuery()) {
            while (lignes.next()) {
                clients.add(reconstruire(lignes.getInt("rib"),
                        lignes.getString("nom"),
                        lignes.getString("mot_de_passe")));
            }
            return clients;
        } catch (SQLException e) {
            throw new PersistanceException("Lecture des clients impossible.", e);
        }
    }

    @Override
    public boolean existsByRib(int rib) {
        String sql = "SELECT 1 FROM clients WHERE rib = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            try (ResultSet lignes = requete.executeQuery()) {
                return lignes.next();
            }
        } catch (SQLException e) {
            throw new PersistanceException("Lecture du client impossible.", e);
        }
    }

    /** Historique stocké d'un client, dans son ordre d'exécution. */
    public List<LigneHistorique> historique(int rib) {
        String sql = "SELECT client_rib, numero_ordre, type, montant, description, horodatage "
                + "FROM transactions WHERE client_rib = ? ORDER BY numero_ordre";
        List<LigneHistorique> lignes = new ArrayList<>();
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            try (ResultSet resultat = requete.executeQuery()) {
                while (resultat.next()) {
                    lignes.add(new LigneHistorique(
                            resultat.getInt("client_rib"),
                            resultat.getInt("numero_ordre"),
                            Transaction.TypeTransaction.valueOf(resultat.getString("type")),
                            resultat.getBigDecimal("montant"),
                            resultat.getString("description"),
                            resultat.getTimestamp("horodatage").toLocalDateTime()));
                }
            }
            return lignes;
        } catch (SQLException e) {
            throw new PersistanceException("Lecture de l'historique impossible.", e);
        }
    }

    // ==================================================================
    // ÉCRITURE
    // ==================================================================

    /**
     * Insère un client qui n'existe pas encore, avec son mot de passe.
     *
     * Seule porte d'entrée pour créer un client : {@code Client} ne rend
     * jamais son mot de passe, il faut donc le fournir à côté.
     */
    public void creer(Client client, String motDePasse) {
        String sql = "INSERT INTO clients (rib, nom, mot_de_passe) VALUES (?, ?, ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, client.getRib());
            requete.setString(2, client.getNom());
            requete.setString(3, motDePasse);
            requete.executeUpdate();
        } catch (SQLException e) {
            throw new PersistanceException(
                    "Création du client " + client.getRib() + " impossible.", e);
        }
        ecrireComptes(client);
    }

    /**
     * Met à jour un client existant : son nom et l'intégralité de ses comptes.
     *
     * Le mot de passe n'est pas touché — il n'est pas lisible depuis
     * {@code Client}, et une mise à jour de solde n'a aucune raison d'y
     * toucher.
     */
    @Override
    public void save(Client client) {
        String sql = "UPDATE clients SET nom = ? WHERE rib = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, client.getNom());
            requete.setInt(2, client.getRib());
            if (requete.executeUpdate() == 0) {
                throw new PersistanceException("Client " + client.getRib()
                        + " inconnu en base : utiliser creer(client, motDePasse) "
                        + "pour l'insérer, le mot de passe n'étant pas lisible depuis Client.");
            }
        } catch (SQLException e) {
            throw new PersistanceException(
                    "Sauvegarde du client " + client.getRib() + " impossible.", e);
        }
        ecrireComptes(client);
    }

    /**
     * ATOMICITÉ — écrit tous les clients passés dans UNE transaction SQL.
     *
     * Le virement en est le cas d'usage : débit de l'un et crédit de l'autre
     * valident ensemble ou échouent ensemble. Au moindre problème, rollback
     * complet : la base reste exactement dans l'état d'avant.
     */
    @Override
    public void sauvegarderEnsemble(Client... clients) {
        boolean autoCommitInitial;
        try {
            autoCommitInitial = connexion.getAutoCommit();
            connexion.setAutoCommit(false);
        } catch (SQLException e) {
            throw new PersistanceException("Ouverture de transaction impossible.", e);
        }

        try {
            for (Client client : clients) {
                save(client);
            }
            connexion.commit();
        } catch (SQLException | RuntimeException echec) {
            annuler(echec);
            throw echec instanceof RuntimeException erreur
                    ? erreur
                    : new PersistanceException("Sauvegarde groupée impossible.", echec);
        } finally {
            retablirAutoCommit(autoCommitInitial);
        }
    }

    /**
     * Écrit l'historique du client tel qu'il est en mémoire.
     *
     * Volontairement séparé de {@link #save(Client)} : tant que
     * {@code Transaction} n'accepte pas son horodatage, un client relu revient
     * avec un historique vide, et un {@code save} automatique effacerait alors
     * les lignes déjà stockées. Cette méthode s'appelle donc explicitement,
     * sur un client dont l'historique en mémoire est complet.
     */
    public void enregistrerHistorique(Client client) {
        supprimerHistorique(client.getRib());

        String sql = "INSERT INTO transactions "
                + "(client_rib, numero_ordre, type, montant, description, horodatage) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            List<Transaction> historique = client.getHistorique();
            for (int ordre = 0; ordre < historique.size(); ordre++) {
                Transaction transaction = historique.get(ordre);
                requete.setInt(1, client.getRib());
                requete.setInt(2, ordre);
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

    // ==================================================================
    // DÉTAIL D'IMPLÉMENTATION
    // ==================================================================

    private Optional<Client> premierClient(PreparedStatement requete) throws SQLException {
        try (ResultSet lignes = requete.executeQuery()) {
            if (!lignes.next()) {
                return Optional.empty();
            }
            return Optional.of(reconstruire(lignes.getInt("rib"),
                    lignes.getString("nom"),
                    lignes.getString("mot_de_passe")));
        }
    }

    /**
     * Reconstruit un client complet à partir de ses lignes.
     *
     * Le client est rebâti par son API PUBLIQUE, jamais par réflexion :
     * l'offre se déduit du type de son compte courant, le solde est rejoué
     * par un crédit ou un débit — ce qui fait basculer tout seul l'état
     * Actif / EnDécouvert — puis le blocage ou la clôture sont appliqués
     * par-dessus. Un compte relu est donc un compte que le modèle aurait pu
     * produire lui-même, pas un objet bricolé de l'extérieur.
     */
    private Client reconstruire(int rib, String nom, String motDePasse) throws SQLException {
        List<LigneCompte> comptes = lireComptes(rib);

        LigneCompte courant = comptes.stream()
                .filter(ligne -> !TYPE_LIVRET_A.equals(ligne.type()))
                .findFirst()
                .orElseThrow(() -> new PersistanceException(
                        "Client " + rib + " stocké sans compte courant."));

        Client client = new Client(nom, motDePasse, rib, offrePour(courant.type()));
        appliquer(client.getCompteCourant(), courant);

        comptes.stream()
                .filter(ligne -> TYPE_LIVRET_A.equals(ligne.type()))
                .findFirst()
                .ifPresent(livret -> {
                    client.ouvrirLivretA();
                    appliquer(client.getLivretA().orElseThrow(), livret);
                });

        return client;
    }

    /** Rejoue solde puis état sur un compte neuf. L'ordre compte : un compte bloqué refuse les mouvements. */
    private void appliquer(Compte compte, LigneCompte ligne) {
        try {
            int signe = ligne.solde().signum();
            if (signe > 0) {
                compte.crediter(ligne.solde());
            } else if (signe < 0) {
                compte.debiter(ligne.solde().negate());
            }
        } catch (RuntimeException e) {
            throw new PersistanceException("Solde stocké incompatible avec l'offre du compte : "
                    + ligne.type() + " à " + ligne.solde() + " €.", e);
        }

        switch (ligne.etat()) {
            case ETAT_BLOQUE -> compte.bloquer();
            case ETAT_FERME -> compte.fermer();
            // ACTIF et EN_DECOUVERT découlent du signe du solde, que la
            // machine à états vient d'appliquer toute seule.
            case ETAT_ACTIF, ETAT_EN_DECOUVERT -> { }
            default -> throw new PersistanceException("État de compte inconnu : " + ligne.etat());
        }
    }

    private List<LigneCompte> lireComptes(int rib) throws SQLException {
        String sql = "SELECT type, solde, etat FROM comptes WHERE client_rib = ? ORDER BY id";
        List<LigneCompte> comptes = new ArrayList<>();
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, rib);
            try (ResultSet lignes = requete.executeQuery()) {
                while (lignes.next()) {
                    comptes.add(new LigneCompte(lignes.getString("type"),
                            lignes.getBigDecimal("solde"),
                            lignes.getString("etat")));
                }
            }
        }
        return comptes;
    }

    /**
     * Réécrit les comptes du client : suppression puis insertion.
     *
     * Un client a un compte courant et au plus un livret ; le coût est donc
     * négligeable, et c'est la seule façon simple de rester juste sans
     * identifiant technique sur {@code Compte} — un UPDATE ligne à ligne
     * demanderait de savoir quelle ligne correspond à quel objet.
     */
    private void ecrireComptes(Client client) {
        try (PreparedStatement suppression =
                     connexion.prepareStatement("DELETE FROM comptes WHERE client_rib = ?")) {
            suppression.setInt(1, client.getRib());
            suppression.executeUpdate();
        } catch (SQLException e) {
            throw new PersistanceException("Mise à jour des comptes impossible.", e);
        }

        String sql = "INSERT INTO comptes (client_rib, type, solde, etat) VALUES (?, ?, ?, ?)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            for (Compte compte : client.getComptes()) {
                requete.setInt(1, client.getRib());
                requete.setString(2, typeDe(compte));
                requete.setBigDecimal(3, compte.getSolde());
                requete.setString(4, etatDe(compte));
                requete.addBatch();
            }
            requete.executeBatch();
        } catch (SQLException e) {
            throw new PersistanceException("Écriture des comptes impossible.", e);
        }
    }

    private void supprimerHistorique(int rib) {
        try (PreparedStatement requete =
                     connexion.prepareStatement("DELETE FROM transactions WHERE client_rib = ?")) {
            requete.setInt(1, rib);
            requete.executeUpdate();
        } catch (SQLException e) {
            throw new PersistanceException("Mise à jour de l'historique impossible.", e);
        }
    }

    private void annuler(Exception cause) {
        try {
            connexion.rollback();
        } catch (SQLException echecDuRollback) {
            PersistanceException erreur = new PersistanceException(
                    "Échec de l'annulation : la base peut être incohérente.", echecDuRollback);
            erreur.addSuppressed(cause);
            throw erreur;
        }
    }

    private void retablirAutoCommit(boolean valeurInitiale) {
        try {
            connexion.setAutoCommit(valeurInitiale);
        } catch (SQLException e) {
            throw new PersistanceException("Rétablissement du mode de validation impossible.", e);
        }
    }

    private static String typeDe(Compte compte) {
        return compte.getClass().getSimpleName();
    }

    private static String etatDe(Compte compte) {
        return switch (compte.getEtat().getClass().getSimpleName()) {
            case "CompteActif" -> ETAT_ACTIF;
            case "CompteEnDecouvert" -> ETAT_EN_DECOUVERT;
            case "CompteBloque" -> ETAT_BLOQUE;
            case "CompteFerme" -> ETAT_FERME;
            default -> throw new PersistanceException(
                    "État de compte non stockable : " + compte.getEtat().libelle());
        };
    }

    private static OffreFactory offrePour(String typeDeCompteCourant) {
        return switch (typeDeCompteCourant) {
            case TYPE_ETUDIANT -> new OffreEtudianteFactory();
            case TYPE_STANDARD -> new OffreStandardFactory();
            case TYPE_PREMIUM -> new OffrePremiumFactory();
            default -> throw new PersistanceException(
                    "Type de compte courant inconnu : " + typeDeCompteCourant);
        };
    }

    /** Une ligne de la table comptes, avant reconstruction de l'objet métier. */
    private record LigneCompte(String type, BigDecimal solde, String etat) {
    }
}
