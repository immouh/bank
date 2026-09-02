package com.example.bank.core.service.securite;

import java.util.Optional;

/**
 * Où sont rangés les mots de passe hachés.
 *
 * POURQUOI CE N'EST PAS DANS {@code ClientRepository} — {@code Client}
 * n'expose aucun accesseur de mot de passe, et c'est délibéré : un test
 * l'interdit depuis la phase A. Un repository de clients ne peut donc ni le
 * lire ni le rendre. Les identifiants sont d'ailleurs une affaire
 * d'authentification, pas de modèle bancaire : les séparer permet, demain, de
 * les déplacer ailleurs (annuaire, fournisseur d'identité) sans toucher au
 * stockage des comptes.
 */
public interface RepertoireMotsDePasse {

    /** Haché de référence du client, vide si aucun n'est enregistré. */
    Optional<String> hachePour(int rib);

    /** Enregistre ou remplace le haché d'un client. */
    void enregistrer(int rib, String hache);
}
