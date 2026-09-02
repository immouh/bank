package com.example.bank.core.model;

import com.example.bank.core.exception.existence.LivretAAbsentException;
import com.example.bank.core.exception.existence.LivretADejaExistantException;
import com.example.bank.core.exception.technique.InvariantRompuException;
import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.concret.LivretA;
import com.example.bank.core.model.offre.concret.OffreStandardFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Un client de la banque, avec les comptes de son offre.
 *
 * RÈGLE DE PLACEMENT — ce modèle ne porte QUE la logique qui s'applique à son
 * propre état interne : ouvrir un compte, empiler une ligne d'historique,
 * vérifier son mot de passe. Il n'accède jamais à un autre Client, à un
 * repository, ni à la console : coordonner deux clients (un virement) ou
 * relire des données est le travail de {@code BanqueService}.
 *
 * <h2>Le client ne tient plus ses soldes lui-même</h2>
 *
 * Les trois champs {@code soldeCompte}, {@code livretAExiste} et
 * {@code soldeLivretA} ont disparu au profit d'une {@code List<Compte>}. Le
 * solde et les règles qui le gouvernent (découvert autorisé, état actif /
 * bloqué / fermé) appartiennent désormais au {@link Compte}, qui les portait
 * déjà pour le package {@code offre} sans que personne ne s'en serve. Cette
 * classe ne fait plus que déléguer.
 *
 * Conséquence directe et voulue : le DÉCOUVERT du tier s'applique enfin. Un
 * client de l'offre Standard peut descendre à -300 €, un Premium à -2 000 €,
 * un Étudiant à 0 € — sans une ligne de code de découvert ici.
 *
 * <h2>Le client est attaché à son offre</h2>
 *
 * La {@link OffreFactory} reçue à la construction est ce qui garantit la
 * cohérence de la famille de produits : le compte courant vient d'elle, et le
 * Livret A ouvert plus tard reçoit la stratégie de taux du MÊME tier. Un
 * client Étudiant ne peut donc pas se retrouver avec un livret rémunéré au
 * taux Premium.
 *
 * Les montants sont en {@link BigDecimal} (échelle 2, arrondi HALF_EVEN) :
 * un type flottant ne représente pas exactement les centimes.
 */
public class Client {

    private final String nom;                   // NOM DU CLIENT
    private final String mdp;                   // MOT DE PASSE DU CLIENT
    private final int rib;                      // RIB DU CLIENT
    private final OffreFactory offre;           // OFFRE COMMERCIALE SOUSCRITE
    private final List<Compte> comptes;         // COMPTE COURANT + EPARGNE EVENTUELLE
    private final List<Transaction> historique; // HISTORIQUE DES OPERATIONS

    /**
     * Client de l'offre STANDARD, l'offre par défaut de la banque.
     *
     * Ce raccourci existe pour les appelants qui n'ont pas d'offre à choisir
     * (données de démonstration, tests). Le tier reste explicite dès qu'il
     * compte : voir {@link #Client(String, String, int, OffreFactory)}.
     */
    public Client(String nom, String mdp, int rib) {
        this(nom, mdp, rib, new OffreStandardFactory());
    }

    public Client(String nom, String mdp, int rib, OffreFactory offre) {
        this.nom = nom;
        this.mdp = mdp;
        this.rib = rib;
        this.offre = offre;
        this.comptes = new ArrayList<>();
        // Tout client a un compte courant dès l'ouverture, celui de son offre.
        this.comptes.add(offre.creerCompte());
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

    /** Vue non modifiable : on n'ouvre un compte que par les méthodes dédiées. */
    public List<Compte> getComptes() {
        return Collections.unmodifiableList(comptes);
    }

    /**
     * Le compte courant, celui qui reçoit dépôts, retraits et virements.
     *
     * IDENTIFICATION DES COMPTES — le tri se fait sur
     * {@link Compte#estEpargne()}, jamais sur un {@code instanceof} : ajouter
     * un LDD ou un PEL demain ne demandera aucune retouche ici. Le compte
     * courant est créé par le constructeur et n'est jamais retiré, la liste en
     * contient donc toujours exactement un.
     */
    public Compte getCompteCourant() {
        return comptes.stream()
                .filter(compte -> !compte.estEpargne())
                .findFirst()
                .orElseThrow(() -> new InvariantRompuException(
                        "Client sans compte courant : invariant rompu."));
    }

    /** Le Livret A du client, vide tant qu'il n'en a pas ouvert. */
    public Optional<Compte> getLivretA() {
        return comptes.stream().filter(Compte::estEpargne).findFirst();
    }

    /**
     * Somme des soldes de TOUS les comptes du client — le patrimoine qu'il a
     * chez nous, épargne comprise.
     */
    public BigDecimal soldeTotal() {
        return comptes.stream()
                .map(Compte::getSolde)
                .reduce(Montants.ZERO, BigDecimal::add);
    }

    /** Solde du compte courant. */
    public BigDecimal getSoldeCompte() {
        return getCompteCourant().getSolde();
    }

    public boolean isLivretAExiste() {
        return getLivretA().isPresent();
    }

    /** Solde du Livret A, zéro tant qu'aucun livret n'est ouvert. */
    public BigDecimal getSoldeLivretA() {
        return getLivretA().map(Compte::getSolde).orElse(Montants.ZERO);
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
        getCompteCourant().crediter(montant);
    }

    /**
     * Débite le compte courant, dans la limite du solde AUGMENTÉ du découvert
     * autorisé par l'offre du client. C'est le {@link Compte} qui arbitre.
     */
    public void debiter(BigDecimal montant) {
        getCompteCourant().debiter(montant);
    }

    /**
     * Ouvre le Livret A, à zéro : il s'alimente ensuite depuis le compte
     * courant. Le livret est rémunéré au taux de l'offre du client.
     */
    public void ouvrirLivretA() {
        if (isLivretAExiste()) {
            throw new LivretADejaExistantException();
        }
        comptes.add(new LivretA(offre.creerStrategieTaux()));
    }

    /** Crédite le Livret A. L'épargne ne se débite jamais vers l'extérieur. */
    public void crediterLivretA(BigDecimal montant) {
        BigDecimal m = Montants.exigerPositif(montant);
        getLivretA().orElseThrow(LivretAAbsentException::new).crediter(m);
    }
}
