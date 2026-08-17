package com.example.bank.swing.controller;

import com.example.bank.core.exception.ClientIntrouvableException;
import com.example.bank.core.model.Client;
import com.example.bank.core.repository.ClientRepository;
import com.example.bank.core.service.BanqueService;

import java.math.BigDecimal;

/**
 * Virements pilotés depuis l'interface graphique.
 *
 * La fenêtre ne manipule que des RIB (ce que l'utilisateur saisit) : la
 * résolution RIB -> Client se fait ici, jamais dans le code Swing.
 */
public class VirementController {

    private final BanqueService banqueService;
    private final ClientRepository clientRepository;

    public VirementController(BanqueService banqueService, ClientRepository clientRepository) {
        this.banqueService = banqueService;
        this.clientRepository = clientRepository;
    }

    /** Virement externe : compte courant de l'émetteur -> compte courant du destinataire. */
    public void virer(int emetteurRib, int destinataireRib, BigDecimal montant) {
        Client emetteur = rechercher(emetteurRib);
        Client destinataire = clientRepository.findByRib(destinataireRib)
                .orElseThrow(() -> new ClientIntrouvableException("Destinataire introuvable."));
        banqueService.virer(emetteur, destinataire, montant);
    }

    /** Virement interne : compte courant -> Livret A du même client. */
    public void virerVersLivretA(int emetteurRib, BigDecimal montant) {
        banqueService.virerVersLivretA(rechercher(emetteurRib), montant);
    }

    private Client rechercher(int rib) {
        return clientRepository.findByRib(rib)
                .orElseThrow(() -> ClientIntrouvableException.parRib(rib));
    }
}
