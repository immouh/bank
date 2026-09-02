package com.example.bank.core.service.securite;

import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.core.model.Client;

/**
 * Première étape : le nom saisi correspond-il à quelqu'un ?
 *
 * L'ÉTAPE NE SE CONTENTE PAS DE REFUSER, ELLE PREND LE MÊME TEMPS QU'UN
 * REFUS DE MOT DE PASSE. Sinon, un nom inconnu répondrait en une
 * microseconde là où un mot de passe faux coûte les ~100 ms de BCrypt : le
 * simple chronomètre suffirait alors à énumérer les comptes existants, et le
 * message unique ne protégerait plus rien. On vérifie donc la saisie contre
 * un haché factice, dont on sait qu'elle ne correspondra pas, avant de
 * lever l'exception.
 */
public class VerificationClientExiste extends EtapeAuthentification {

    private final HachageStrategy hachage;

    public VerificationClientExiste(HachageStrategy hachage) {
        this.hachage = hachage;
    }

    @Override
    protected void executer(Client client, String motDePasseSaisi) {
        if (client != null) {
            return;
        }
        // Le haché factice vient de l'algorithme, déjà calculé : on paie le
        // temps d'UNE vérification, exactement comme un mot de passe faux.
        hachage.verifier(motDePasseSaisi == null ? "" : motDePasseSaisi, hachage.hacheFactice());
        throw identifiantsInvalides();
    }

    /**
     * Message STRICTEMENT identique à celui de {@link VerificationMotDePasse} :
     * nom inconnu et mot de passe faux doivent être indiscernables.
     */
    static ClientIntrouvableException identifiantsInvalides() {
        return new ClientIntrouvableException("Nom d'utilisateur ou mot de passe incorrect.");
    }
}
