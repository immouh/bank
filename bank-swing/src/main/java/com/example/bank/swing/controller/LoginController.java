package com.example.bank.swing.controller;

import com.example.bank.core.exception.ClientIntrouvableException;
import com.example.bank.core.model.Client;
import com.example.bank.core.service.AuthService;

/**
 * Point d'entrée unique de la fenêtre de connexion vers le coeur métier.
 * La fenêtre ne connaît ni AuthService, ni le repository, ni le modèle Client
 * autrement que comme résultat.
 */
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * @return le client authentifié
     * @throws ClientIntrouvableException si les identifiants sont invalides
     */
    public Client authentifier(String nom, String motDePasse) {
        return authService.authentifier(nom, motDePasse);
    }
}
