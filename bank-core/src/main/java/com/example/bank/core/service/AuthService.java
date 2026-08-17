package com.example.bank.core.service;

import com.example.bank.core.exception.ClientIntrouvableException;
import com.example.bank.core.model.Client;
import com.example.bank.core.repository.ClientRepository;

/**
 * Authentification.
 *
 * RÈGLE DE PLACEMENT — la COMPARAISON du mot de passe appartient au modèle
 * ({@code Client.verifierMotDePasse}), car elle ne porte que sur l'état interne
 * du client. La RECHERCHE du client par son nom demande le repository : c'est
 * donc au service de la faire, puis de déléguer la comparaison au modèle.
 */
public class AuthService {

    private final ClientRepository repository;

    public AuthService(ClientRepository repository) {
        this.repository = repository;
    }

    /**
     * @return le client authentifié
     * @throws ClientIntrouvableException si le nom est inconnu OU le mot de
     *         passe faux — un message unique dans les deux cas, pour ne pas
     *         révéler quels noms d'utilisateur existent.
     */
    public Client authentifier(String nom, String motDePasse) {
        return repository.findByNom(nom)
                .filter(client -> client.verifierMotDePasse(motDePasse))
                .orElseThrow(() -> new ClientIntrouvableException(
                        "Nom d'utilisateur ou mot de passe incorrect."));
    }
}
