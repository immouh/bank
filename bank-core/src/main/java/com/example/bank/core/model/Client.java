package com.example.bank.core.model;

import com.example.bank.core.exception.LivretAAbsentException;
import com.example.bank.core.exception.SoldeInsuffisantException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un client de la banque, avec son compte courant et son éventuel Livret A.
 *
 * RÈGLE DE PLACEMENT — ce modèle ne porte QUE la logique qui s'applique à son
 * propre état interne : créditer/débiter ses soldes, vérifier son mot de passe,
 * empiler une ligne d'historique. Il n'accède jamais à un autre Client, à un
 * repository, ni à la console : coordonner deux clients (un virement) ou
 * relire des données est le travail de {@code BanqueService}.
 *
 * Les montants sont en {@link BigDecimal} (échelle 2, arrondi HALF_EVEN) :
 * un type flottant ne représente pas exactement les centimes.
 */
public class Client {

    private final String nom;                   // NOM DU CLIENT
    private final String mdp;                   // MOT DE PASSE DU CLIENT
    private final int rib;                      // RIB DU CLIENT
    private BigDecimal soldeCompte;             // SOLDE DU COMPTE COURANT
    private boolean livretAExiste;              // BOOL QUI DIT SI UN LIVRET A EXISTE
    private BigDecimal soldeLivretA;            // SOLDE DU LIVRET A
    private final List<Transaction> historique; // HISTORIQUE DES OPERATIONS

    public Client(String nom, String mdp, int rib) {
        this.nom = nom;
        this.mdp = mdp;
        this.rib = rib;
        this.soldeCompte = Montants.ZERO;       // INITIALISATION DU SOLDE DU COMPTE A 0
        this.livretAExiste = false;             // LIVRET A INEXISTANT
        this.soldeLivretA = Montants.ZERO;
        this.historique = new ArrayList<>();
    }

    // ------------------------------------------------------------------
    // LES GETTERS
    // ------------------------------------------------------------------

    public String getNom() {
        return nom;
    }

    public int getRib() {
        return rib;
    }

    public BigDecimal getSoldeCompte() {
        return soldeCompte;
    }

    public boolean isLivretAExiste() {
        return livretAExiste;
    }

    public BigDecimal getSoldeLivretA() {
        return soldeLivretA;
    }

    /** Vue non modifiable : l'historique ne s'altère que par les opérations. */
    public List<Transaction> getHistorique() {
        return Collections.unmodifiableList(historique);
    }

    /**
     * Vérifie le mot de passe sans jamais l'exposer à l'appelant.
     * Reste dans le modèle : le mot de passe est un état interne du client,
     * et le comparer ne demande aucune donnée extérieure.
     * (Le hachage sera ajouté dans une phase ultérieure.)
     */
    public boolean verifierMotDePasse(String saisie) {
        return mdp != null && mdp.equals(saisie);
    }

    // ------------------------------------------------------------------
    // OPERATIONS SUR SON PROPRE ETAT
    // ------------------------------------------------------------------

    /** Enfile une transaction dans l'historique. */
    public void ajouterTransaction(Transaction t) {
        historique.add(t);
    }

    /** Crédite le compte courant. */
    public void crediter(BigDecimal montant) {
        BigDecimal m = Montants.exigerPositif(montant);
        soldeCompte = soldeCompte.add(m);
    }

    /** Débite le compte courant, dans la limite du solde disponible. */
    public void debiter(BigDecimal montant) {
        BigDecimal m = Montants.exigerPositif(montant);
        if (soldeCompte.compareTo(m) < 0) {
            throw new SoldeInsuffisantException(soldeCompte, m);
        }
        soldeCompte = soldeCompte.subtract(m);
    }

    /** Ouvre le Livret A, à zéro : il s'alimente ensuite depuis le compte courant. */
    public void ouvrirLivretA() {
        if (livretAExiste) {
            throw new IllegalStateException("Livret A déjà existant.");
        }
        livretAExiste = true;
        soldeLivretA = Montants.ZERO;
    }

    /** Crédite le Livret A. L'épargne ne se débite jamais vers l'extérieur. */
    public void crediterLivretA(BigDecimal montant) {
        BigDecimal m = Montants.exigerPositif(montant);
        if (!livretAExiste) {
            throw new LivretAAbsentException();
        }
        soldeLivretA = soldeLivretA.add(m);
    }
}
