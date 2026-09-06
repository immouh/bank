package com.example.bank.web.securite;

import com.example.bank.core.exception.existence.ClientIntrouvableException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * D'où les contrôleurs tiennent le RIB de l'appelant.
 *
 * LE POINT DE SÉCURITÉ LE PLUS IMPORTANT DE CE MODULE. Le RIB vient du JETON,
 * jamais d'un paramètre de requête, d'un corps JSON ou d'un segment d'URL.
 * Un endpoint qui accepterait {@code GET /api/comptes/{rib}} laisserait
 * n'importe quel client authentifié lire les comptes d'un autre en changeant
 * un chiffre — la faille classique, et celle qu'aucune vérification de mot de
 * passe n'attrape, puisque l'appelant EST authentifié.
 *
 * Passer par une classe dédiée plutôt que de lire le contexte dans chaque
 * contrôleur laisse un seul endroit à relire pour s'en assurer.
 */
public final class ClientAuthentifie {

    private ClientAuthentifie() {
    }

    /**
     * RIB de l'appelant, tel que le filtre JWT l'a posé.
     *
     * @throws ClientIntrouvableException si aucune authentification n'est
     *         présente — ne devrait pas arriver sur une route protégée, la
     *         configuration de sécurité l'ayant déjà refusée ; c'est une
     *         ceinture en plus du harnais.
     */
    public static int rib() {
        Authentication authentification =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentification == null
                || !(authentification.getPrincipal() instanceof Integer rib)) {
            throw new ClientIntrouvableException("Requête non authentifiée.");
        }
        return rib;
    }
}
