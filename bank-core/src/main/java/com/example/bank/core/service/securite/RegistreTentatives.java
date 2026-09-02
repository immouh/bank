package com.example.bank.core.service.securite;

/**
 * Compteur de tentatives de connexion échouées, et verrouillage qui en découle.
 *
 * POURQUOI PAS DES CHAMPS SUR {@code Client} — le nombre d'échecs n'est pas
 * un attribut bancaire du client, c'est un état de la mécanique
 * d'authentification. Le poser sur {@code Client} obligerait à lui ajouter des
 * mutateurs pour le recharger depuis la base, ce qui affaiblirait
 * l'encapsulation que le modèle défend depuis la phase B — et mélangerait
 * dans un même objet « ce que le client possède » et « comment on le laisse
 * entrer ».
 */
public interface RegistreTentatives {

    /** Nombre d'échecs consécutifs au-delà duquel l'accès est suspendu. */
    int ECHECS_AVANT_VERROUILLAGE = 3;

    /**
     * Durée du verrouillage.
     *
     * DÉVERROUILLAGE AUTOMATIQUE, pas manuel : l'application n'a ni écran
     * d'administration ni support à qui s'adresser, un verrouillage définitif
     * rendrait donc le compte inutilisable pour de bon. Quinze minutes
     * suffisent à ruiner une attaque par force brute (trois essais par
     * quart d'heure) sans punir l'utilisateur qui s'est trompé de clavier.
     */
    int MINUTES_DE_VERROUILLAGE = 15;

    /** Compte un échec de plus pour ce client. */
    void enregistrerEchec(int rib);

    /** Remet le compteur à zéro — une connexion réussie efface l'ardoise. */
    void reinitialiser(int rib);

    /** Vrai tant que le verrouillage court. Un verrouillage expiré ne compte plus. */
    boolean estVerrouille(int rib);

    /** Échecs consécutifs actuellement retenus contre ce client. */
    int echecs(int rib);

    /** Minutes restantes avant la fin du verrouillage, 0 si le compte est libre. */
    int minutesRestantes(int rib);
}
