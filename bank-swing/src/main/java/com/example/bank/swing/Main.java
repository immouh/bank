package com.example.bank.swing;

import com.example.bank.core.model.Client;
import com.example.bank.core.repository.ClientRepository;
import com.example.bank.core.repository.InMemoryClientRepository;
import com.example.bank.core.service.AuthService;
import com.example.bank.core.service.BanqueService;
import com.example.bank.swing.controller.CompteController;
import com.example.bank.swing.controller.LoginController;
import com.example.bank.swing.controller.VirementController;
import com.example.bank.swing.ui.GestionnaireInterfaceGraphique;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;

/**
 * Point d'assemblage de l'application : c'est le seul endroit qui sait quelle
 * implémentation de repository est utilisée et qui câble services et
 * controllers entre eux.
 */
public class Main {

    public static void main(String[] args) {
        // Câblage des couches, du plus bas au plus haut.
        ClientRepository repository = new InMemoryClientRepository();
        BanqueService banqueService = new BanqueService(repository);
        AuthService authService = new AuthService(repository);

        chargerDonneesDeDemonstration(banqueService, repository);

        LoginController loginController = new LoginController(authService);
        CompteController compteController = new CompteController(banqueService);
        VirementController virementController = new VirementController(banqueService, repository);

        // Swing doit être construit et manipulé sur l'Event Dispatch Thread.
        SwingUtilities.invokeLater(() ->
                new GestionnaireInterfaceGraphique(loginController, compteController, virementController));
    }

    /**
     * Reproduit l'état initial de démonstration d'origine, mais en passant par
     * le service : l'argent du Livret A vient désormais du compte courant au
     * lieu d'apparaître de nulle part.
     *
     * État final identique à avant : Mouh compte 0,00 € / Livret A 1000,00 €,
     * amine compte 300,00 €.
     */
    private static void chargerDonneesDeDemonstration(BanqueService banqueService,
                                                      ClientRepository repository) {
        Client mouh = banqueService.rechercherParRib(123);
        Client amine = banqueService.rechercherParRib(456);

        banqueService.creerLivretA(mouh);
        banqueService.deposer(mouh, new BigDecimal("1000"));
        banqueService.virerVersLivretA(mouh, new BigDecimal("1000"));

        banqueService.deposer(mouh, new BigDecimal("500"));
        banqueService.retirer(mouh, new BigDecimal("200"));
        banqueService.virer(mouh, amine, new BigDecimal("300"));
    }
}
